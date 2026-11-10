package bhoon.sugang_helper.academic.crawling.client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class BoundedInputStream extends FilterInputStream {

    private final long maximumBytes;
    private long bytesRead;

    BoundedInputStream(InputStream input, long maximumBytes) {
        super(input);
        this.maximumBytes = maximumBytes;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value != -1) {
            count(1);
        }
        return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        int read = super.read(bytes, offset, length);
        if (read > 0) {
            count(read);
        }
        return read;
    }

    private void count(int read) throws IOException {
        bytesRead += read;
        if (bytesRead > maximumBytes) {
            throw new IOException("강의 수집 응답이 최대 크기를 초과했습니다. 제한 바이트 수: " + maximumBytes);
        }
    }
}
