package bhoon.sugang_helper.crawling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.crawling.infra.JbnuCourseApiClient;
import java.io.InputStream;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

class JbnuCourseStaxItemReaderTest {

    private static final String YEAR = "2026";
    private static final String SEMESTER = "U211600010";

    @Test
    void readsOneParsedCourseAtATimeFromTheResponseStream() throws Exception {
        JbnuCourseApiClient apiClient = mock(JbnuCourseApiClient.class);
        JbnuCourseParser parser = mock(JbnuCourseParser.class);
        InputStream responseStream = mock(InputStream.class);
        @SuppressWarnings("unchecked")
        Iterator<ParsedCourseDto> iterator = mock(Iterator.class);
        ParsedCourseDto firstCourse = mock(ParsedCourseDto.class);
        ParsedCourseDto secondCourse = mock(ParsedCourseDto.class);
        JbnuCourseStaxItemReader reader = new JbnuCourseStaxItemReader(apiClient, parser, YEAR, SEMESTER);

        given(apiClient.fetchCourseDataStream(YEAR, SEMESTER)).willReturn(responseStream);
        given(parser.streamCourses(responseStream, YEAR, SEMESTER)).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, true, false);
        given(iterator.next()).willReturn(firstCourse, secondCourse);

        reader.open(new ExecutionContext());

        assertThat(reader.read()).isSameAs(firstCourse);
        assertThat(reader.read()).isSameAs(secondCourse);
        assertThat(reader.read()).isNull();

        reader.close();

        verify(apiClient).fetchCourseDataStream(YEAR, SEMESTER);
        verify(apiClient, never()).fetchCourseDataXml(YEAR, SEMESTER);
        verify(responseStream).close();
    }
}
