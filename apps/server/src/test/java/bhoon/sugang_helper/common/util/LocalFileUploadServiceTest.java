package bhoon.sugang_helper.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class LocalFileUploadServiceTest {

    @TempDir
    Path uploadDirectory;

    private final ImageUploadSanitizer imageUploadSanitizer = new ImageUploadSanitizer();
    private final LocalFileUploadService fileUploadService = new LocalFileUploadService(imageUploadSanitizer);

    @AfterEach
    void clearTransactionSynchronization() {
        imageUploadSanitizer.stopImageDecoder();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("트랜잭션 롤백은 업로드된 파일을 삭제한다")
    void rollbackDeletesUploadedFiles() throws Exception {
        // given
        configureUploadDirectory();
        Path uploadedFile = Files.writeString(uploadDirectory.resolve("rolled-back.png"), "content");
        TransactionSynchronizationManager.initSynchronization();

        // when
        fileUploadService.deleteFilesAfterTransactionRollback(List.of("/uploads/rolled-back.png"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        // then
        assertThat(uploadedFile).doesNotExist();
    }

    @Test
    @DisplayName("롤백 정리는 트랜잭션이 없거나 커밋되면 파일을 유지한다")
    void rollbackCleanupKeepsFilesWithoutAnActiveTransactionOrAfterCommit() throws Exception {
        // given
        configureUploadDirectory();
        Path uploadedFile = Files.writeString(uploadDirectory.resolve("kept.png"), "content");

        // when
        fileUploadService.deleteFilesAfterTransactionRollback(List.of("/uploads/kept.png"));

        // then
        assertThat(uploadedFile).exists();

        // given
        TransactionSynchronizationManager.initSynchronization();

        // when
        fileUploadService.deleteFilesAfterTransactionRollback(List.of());

        // then
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();

        // when
        fileUploadService.deleteFilesAfterTransactionRollback(List.of("/uploads/kept.png"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        // then
        assertThat(uploadedFile).exists();
    }

    @Test
    @DisplayName("이미지 MIME 타입을 주장하는 SVG도 업로드를 거부한다")
    void svgUploadIsRejectedEvenWhenItClaimsAnImageMimeType() {
        // given
        configureUploadDirectory();
        MockMultipartFile svg = new MockMultipartFile(
                "file", "script.svg", "image/svg+xml", "<svg><script>alert(1)</script></svg>".getBytes());

        // when & then
        assertThatThrownBy(() -> fileUploadService.uploadImages(List.of(svg)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    @DisplayName("WebP 업로드를 거부한다")
    void webpUploadIsRejected() throws Exception {
        // given
        configureUploadDirectory();
        byte[] source = staticWebpBytes();
        MockMultipartFile webp = new MockMultipartFile("file", "canvas.webp", "image/webp", source);

        // when & then
        assertThatThrownBy(() -> fileUploadService.uploadImages(List.of(webp)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FILE_TYPE);
        assertThat(uploadDirectory).isEmptyDirectory();
    }

    @Test
    @DisplayName("저장 파일의 콘텐츠 타입은 이름 대신 실제 내용으로 판별한다")
    void storedContentTypeIsSniffedRatherThanTakenFromFilename() throws Exception {
        // given
        configureUploadDirectory();
        Files.write(uploadDirectory.resolve("mismatch.jpg"), imageBytes("png"));

        // when & then
        assertThat(fileUploadService.loadFile("/uploads/mismatch.jpg").contentType())
                .isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    @DisplayName("기존 미지원 이미지는 다운로드용 콘텐츠 타입을 사용한다")
    void legacyUnsupportedImageFallsBackToDownloadContentType() throws Exception {
        // given
        configureUploadDirectory();
        Files.write(uploadDirectory.resolve("legacy.webp"), staticWebpBytes());

        // when & then
        assertThat(fileUploadService.loadFile("/uploads/legacy.webp").contentType())
                .isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    @DisplayName("저장 이미지는 주입된 PNG 메타데이터를 보존하지 않는다")
    void storedImageDoesNotRetainInjectedPngMetadata() throws Exception {
        // given
        configureUploadDirectory();
        byte[] source = pngWithTextChunk("GPS_SENTINEL=35.123,127.456");
        MockMultipartFile png = new MockMultipartFile("file", "location.png", "image/png", source);

        // when
        String fileUrl = fileUploadService.uploadImages(List.of(png)).getFirst();

        // then
        assertThat(new String(Files.readAllBytes(storedPath(fileUrl)), StandardCharsets.ISO_8859_1))
                .doesNotContain("GPS_SENTINEL");
    }

    @Test
    @DisplayName("저장 이미지는 주입된 EXIF 메타데이터를 보존하지 않는다")
    void storedImageDoesNotRetainInjectedExifMetadata() throws Exception {
        // given
        configureUploadDirectory();
        byte[] source = jpegWithExifMarker("EXIF_GPS_SENTINEL=35.123,127.456");
        MockMultipartFile jpeg = new MockMultipartFile("file", "location.jpg", "image/jpeg", source);

        // when
        String fileUrl = fileUploadService.uploadImages(List.of(jpeg)).getFirst();

        // then
        assertThat(new String(Files.readAllBytes(storedPath(fileUrl)), StandardCharsets.ISO_8859_1))
                .doesNotContain("EXIF_GPS_SENTINEL");
    }

    @Test
    @DisplayName("너비·높이 제한을 넘는 이미지는 저장 전에 거부한다")
    void oversizedDimensionsAreRejectedBeforeSaving() throws Exception {
        // given
        configureUploadDirectory();
        MockMultipartFile oversized = new MockMultipartFile(
                "file", "oversized.png", "image/png", pngWithDimensions(4097, 1));

        // when & then
        assertThatThrownBy(() -> fileUploadService.uploadImages(List.of(oversized)))
                .isInstanceOf(CustomException.class);
        assertThat(uploadDirectory).isEmptyDirectory();
    }

    @Test
    @DisplayName("과도한 픽셀 수의 이미지는 저장 전에 거부한다")
    void pixelBombIsRejectedBeforeSaving() throws Exception {
        // given
        configureUploadDirectory();
        MockMultipartFile pixelBomb = new MockMultipartFile(
                "file", "pixel-bomb.png", "image/png", pngWithDimensions(4096, 2049));

        // when & then
        assertThatThrownBy(() -> fileUploadService.uploadImages(List.of(pixelBomb)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_PROCESSING_LIMIT_EXCEEDED);
        assertThat(uploadDirectory).isEmptyDirectory();
    }

    @Test
    @DisplayName("움직이는 이미지 형식은 저장 전에 거부한다")
    void animatedImageFormatsAreRejectedBeforeSaving() throws Exception {
        // given
        configureUploadDirectory();
        MockMultipartFile gif = new MockMultipartFile("file", "animated.gif", "image/gif", imageBytes("gif"));

        // when & then
        assertThatThrownBy(() -> fileUploadService.uploadImages(List.of(gif)))
                .isInstanceOf(CustomException.class);
        assertThat(uploadDirectory).isEmptyDirectory();
    }

    @Test
    @DisplayName("움직이는 PNG 컨테이너는 디코딩 전에 거부한다")
    void animatedPngContainersAreRejectedBeforeDecoding() throws Exception {
        // given
        configureUploadDirectory();
        MockMultipartFile apng = new MockMultipartFile(
                "file", "animated.png", "image/png", pngWithChunk("acTL", new byte[8]));

        // when & then
        assertThatThrownBy(() -> fileUploadService.uploadImages(List.of(apng)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE_CONTENT);
        assertThat(uploadDirectory).isEmptyDirectory();
    }

    @Test
    @DisplayName("일괄 업로드 실패는 같은 요청에서 먼저 정제한 이미지도 삭제한다")
    void failedBatchDeletesImagesAlreadySanitizedInTheSameRequest() throws Exception {
        // given
        configureUploadDirectory();
        MockMultipartFile valid = new MockMultipartFile("file", "valid.png", "image/png", imageBytes("png"));
        MockMultipartFile oversized = new MockMultipartFile(
                "file", "oversized.png", "image/png", pngWithDimensions(4097, 1));

        // when & then
        assertThatThrownBy(() -> fileUploadService.uploadImages(List.of(valid, oversized)))
                .isInstanceOf(CustomException.class);
        assertThat(uploadDirectory).isEmptyDirectory();
    }

    @Test
    @DisplayName("디코딩 제한 시간을 넘으면 저장 없이 업로드를 거부한다")
    void decodeTimeoutRejectsTheUploadWithoutSaving() throws Exception {
        // given
        configureUploadDirectory();
        ReflectionTestUtils.setField(imageUploadSanitizer, "imageDecodeTimeout", Duration.ofMillis(25));
        byte[] source = imageBytes("png");
        AtomicInteger inputStreams = new AtomicInteger();
        MultipartFile slowPng = mock(MultipartFile.class);
        given(slowPng.isEmpty()).willReturn(false);
        given(slowPng.getSize()).willReturn((long) source.length);
        given(slowPng.getOriginalFilename()).willReturn("slow.png");
        given(slowPng.getInputStream()).willAnswer(invocation -> inputStreams.getAndIncrement() == 0
                ? new ByteArrayInputStream(source)
                : slowInput(source));

        // when & then
        assertThatThrownBy(() -> fileUploadService.uploadImages(List.of(slowPng)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_PROCESSING_LIMIT_EXCEEDED);
        assertThat(inputStreams.get()).isGreaterThanOrEqualTo(2);
        assertThat(uploadDirectory).isEmptyDirectory();
    }

    private void configureUploadDirectory() {
        ReflectionTestUtils.setField(fileUploadService, "uploadDir", uploadDirectory.toString());
        ReflectionTestUtils.setField(fileUploadService, "orphanMinimumAge", Duration.ofHours(24));
    }

    private Path storedPath(String fileUrl) {
        return uploadDirectory.resolve(fileUrl.substring("/uploads/".length()));
    }

    private byte[] staticWebpBytes() {
        return Base64.getDecoder().decode(
                "UklGRkwAAABXRUJQVlA4WAoAAAAQAAAAAAAAAAAAQUxQSAIAAAAAi1ZQOCAkAAAAUAEAnQEqAQABAAIANCUAToAxCAD+645eR2EN2HKVeryuAAAA");
    }

    private byte[] imageBytes(String format) throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x00ff00);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        return output.toByteArray();
    }

    private byte[] pngWithDimensions(int width, int height) throws Exception {
        byte[] png = imageBytes("png");
        ByteBuffer.wrap(png).order(ByteOrder.BIG_ENDIAN)
                .putInt(16, width)
                .putInt(20, height);
        CRC32 crc = new CRC32();
        crc.update(png, 12, 17);
        ByteBuffer.wrap(png).order(ByteOrder.BIG_ENDIAN).putInt(29, (int) crc.getValue());
        return png;
    }

    private byte[] pngWithTextChunk(String text) throws Exception {
        byte[] textData = ("Comment\u0000" + text).getBytes(StandardCharsets.ISO_8859_1);
        return pngWithChunk("tEXt", textData);
    }

    private byte[] pngWithChunk(String type, byte[] data) throws Exception {
        byte[] png = imageBytes("png");
        byte[] chunk = pngChunk(type, data);
        int iendChunkStart = png.length - 12;
        byte[] result = new byte[png.length + chunk.length];
        System.arraycopy(png, 0, result, 0, iendChunkStart);
        System.arraycopy(chunk, 0, result, iendChunkStart, chunk.length);
        System.arraycopy(png, iendChunkStart, result, iendChunkStart + chunk.length, png.length - iendChunkStart);
        return result;
    }

    private byte[] pngChunk(String type, byte[] data) {
        ByteBuffer chunk = ByteBuffer.allocate(12 + data.length).order(ByteOrder.BIG_ENDIAN);
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        chunk.putInt(data.length).put(typeBytes).put(data).putInt((int) crc.getValue());
        return chunk.array();
    }

    private byte[] jpegWithExifMarker(String marker) throws Exception {
        byte[] jpeg = imageBytes("jpeg");
        byte[] exifHeader = "Exif\u0000\u0000".getBytes(StandardCharsets.ISO_8859_1);
        byte[] tiffWithoutEntries = new byte[] {
                'I', 'I', 0x2a, 0x00, 0x08, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };
        byte[] markerBytes = marker.getBytes(StandardCharsets.ISO_8859_1);
        byte[] payload = new byte[exifHeader.length + tiffWithoutEntries.length + markerBytes.length];
        System.arraycopy(exifHeader, 0, payload, 0, exifHeader.length);
        System.arraycopy(tiffWithoutEntries, 0, payload, exifHeader.length, tiffWithoutEntries.length);
        System.arraycopy(markerBytes, 0, payload, exifHeader.length + tiffWithoutEntries.length, markerBytes.length);
        ByteBuffer app1 = ByteBuffer.allocate(4 + payload.length).order(ByteOrder.BIG_ENDIAN);
        app1.put((byte) 0xff).put((byte) 0xe1).putShort((short) (payload.length + 2)).put(payload);
        byte[] result = new byte[jpeg.length + app1.capacity()];
        System.arraycopy(jpeg, 0, result, 0, 2);
        System.arraycopy(app1.array(), 0, result, 2, app1.capacity());
        System.arraycopy(jpeg, 2, result, 2 + app1.capacity(), jpeg.length - 2);
        return result;
    }

    private InputStream slowInput(byte[] source) {
        return new FilterInputStream(new ByteArrayInputStream(source)) {
            @Override
            public int read() throws IOException {
                pause();
                return super.read();
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                pause();
                return super.read(bytes, offset, length);
            }

            private void pause() throws IOException {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while reading test image", exception);
                }
            }
        };
    }
}
