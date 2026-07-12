package bhoon.sugang_helper.common.util;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Decodes one allowed still image under bounded resources and re-encodes only pixel data.
 */
@Component
@Slf4j
public class ImageUploadSanitizer {

    private static final byte[] PNG_SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };
    private static final byte[] PNG_ANIMATION_CONTROL = new byte[] {'a', 'c', 'T', 'L'};
    private static final byte[] PNG_END = new byte[] {'I', 'E', 'N', 'D'};
    private static final AtomicBoolean IMAGE_IO_PLUGINS_SCANNED = new AtomicBoolean();

    private final ExecutorService imageDecodeExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), runnable -> {
                Thread thread = new Thread(runnable, "feedback-image-decode");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    @Value("${file.upload.image.max-source-bytes:10485760}")
    private long maximumImageSourceBytes = 10_485_760L;
    @Value("${file.upload.image.max-width:4096}")
    private int maximumImageWidth = 4096;
    @Value("${file.upload.image.max-height:4096}")
    private int maximumImageHeight = 4096;
    @Value("${file.upload.image.max-pixels:8388608}")
    private long maximumImagePixels = 8_388_608L;
    @Value("${file.upload.image.max-output-bytes:5242880}")
    private int maximumSanitizedImageBytes = 5_242_880;
    @Value("${file.upload.image.decode-timeout:PT2S}")
    private Duration imageDecodeTimeout = Duration.ofSeconds(2);

    @PostConstruct
    void verifyWebpDecoder() {
        scanImageIoPlugins();
        if (!ImageIO.getImageReadersByMIMEType("image/webp").hasNext()) {
            throw new IllegalStateException("WebP ImageIO reader is unavailable");
        }
    }

    void validateSourceSize(MultipartFile file) {
        if (maximumImageSourceBytes > 0 && file.getSize() > maximumImageSourceBytes) {
            throw imageProcessingLimitExceeded();
        }
    }

    SanitizedImage sanitize(MultipartFile file, String mimeType) {
        Future<SanitizedImage> task;
        try {
            task = imageDecodeExecutor.submit(() -> decodeAndEncodeImage(file, mimeType));
        } catch (RejectedExecutionException exception) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }

        try {
            return task.get(Math.max(1L, imageDecodeTimeout.toMillis()), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            task.cancel(true);
            throw imageProcessingLimitExceeded();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof CustomException customException) {
                throw customException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            log.warn("Image decode failed: {}", cause.getClass().getSimpleName());
            throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
        }
    }

    private SanitizedImage decodeAndEncodeImage(MultipartFile file, String mimeType) {
        scanImageIoPlugins();
        if ("image/png".equals(mimeType) && containsPngAnimation(file)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
        }

        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, false, true);
                if ("image/webp".equals(mimeType) && reader.getNumImages(false) != 1) {
                    throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
                }
                return encodeSanitizedImage(image);
            } finally {
                reader.dispose();
            }
        } catch (CustomException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
        }
    }

    private boolean containsPngAnimation(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            if (!Arrays.equals(input.readNBytes(PNG_SIGNATURE.length), PNG_SIGNATURE)) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
            }
            while (true) {
                byte[] lengthBytes = input.readNBytes(Integer.BYTES);
                byte[] type = input.readNBytes(Integer.BYTES);
                if (lengthBytes.length != Integer.BYTES || type.length != Integer.BYTES) {
                    throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
                }
                long chunkLength = Integer.toUnsignedLong(ByteBuffer.wrap(lengthBytes).getInt());
                if (maximumImageSourceBytes > 0 && chunkLength > maximumImageSourceBytes) {
                    throw imageProcessingLimitExceeded();
                }
                if (Arrays.equals(type, PNG_ANIMATION_CONTROL)) {
                    return true;
                }
                skipFully(input, chunkLength + Integer.BYTES);
                if (Arrays.equals(type, PNG_END)) {
                    return false;
                }
            }
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
        }
    }

    private void skipFully(InputStream input, long length) throws IOException {
        long remaining = length;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
            } else if (input.read() == -1) {
                throw new IOException("Unexpected end of PNG chunk");
            } else {
                remaining--;
            }
        }
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || width > maximumImageWidth || height > maximumImageHeight
                || (long) width * height > maximumImagePixels) {
            throw imageProcessingLimitExceeded();
        }
    }

    private SanitizedImage encodeSanitizedImage(BufferedImage image) {
        String format = image.getColorModel().hasAlpha() ? "png" : "jpeg";
        try (BoundedOutputStream output = new BoundedOutputStream(maximumSanitizedImageBytes)) {
            if (!ImageIO.write(image, format, output)) {
                throw new IOException("No image writer available");
            }
            return new SanitizedImage(output.toByteArray(), format.equals("jpeg") ? "jpg" : "png");
        } catch (ImageOutputLimitExceededException exception) {
            throw imageProcessingLimitExceeded();
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT);
        }
    }

    private CustomException imageProcessingLimitExceeded() {
        return new CustomException(ErrorCode.IMAGE_PROCESSING_LIMIT_EXCEEDED);
    }

    @PreDestroy
    void stopImageDecoder() {
        imageDecodeExecutor.shutdownNow();
    }

    private static void scanImageIoPlugins() {
        if (IMAGE_IO_PLUGINS_SCANNED.compareAndSet(false, true)) {
            ImageIO.setUseCache(false);
            ImageIO.scanForPlugins();
        }
    }

    record SanitizedImage(byte[] content, String extension) {
    }

    private static final class BoundedOutputStream extends java.io.OutputStream {

        private final int maximumSize;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        private BoundedOutputStream(int maximumSize) {
            this.maximumSize = maximumSize;
        }

        @Override
        public void write(int value) throws IOException {
            ensureCapacity(1);
            output.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            if (offset < 0 || length < 0 || length > bytes.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            ensureCapacity(length);
            output.write(bytes, offset, length);
        }

        private void ensureCapacity(int additionalBytes) throws ImageOutputLimitExceededException {
            if (additionalBytes > maximumSize - output.size()) {
                throw new ImageOutputLimitExceededException();
            }
        }

        private byte[] toByteArray() {
            return output.toByteArray();
        }
    }

    private static final class ImageOutputLimitExceededException extends IOException {
    }
}
