package bhoon.sugang_helper.crawling.application;

import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.crawling.infra.JbnuCourseApiClient;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

public class JbnuCourseStaxItemReader implements ItemStreamReader<ParsedCourseDto> {

    private final JbnuCourseApiClient apiClient;
    private final JbnuCourseParser courseParser;
    private final String year;
    private final String semester;
    private InputStream responseStream;
    private Iterator<ParsedCourseDto> iterator;

    public JbnuCourseStaxItemReader(JbnuCourseApiClient apiClient, JbnuCourseParser courseParser,
                                    String year, String semester) {
        this.apiClient = apiClient;
        this.courseParser = courseParser;
        this.year = year;
        this.semester = semester;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        responseStream = apiClient.fetchCourseDataStream(year, semester);
        iterator = courseParser.streamCourses(responseStream, year, semester);
    }

    @Override
    public ParsedCourseDto read() {
        return iterator != null && iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // The source API does not provide a stable row cursor for restart offsets.
    }

    @Override
    public void close() {
        if (responseStream == null) {
            return;
        }
        try {
            responseStream.close();
        } catch (IOException e) {
            throw new ItemStreamException("Failed to close crawler response stream", e);
        }
    }
}
