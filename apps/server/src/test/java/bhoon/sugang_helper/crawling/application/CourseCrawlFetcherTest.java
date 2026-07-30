package bhoon.sugang_helper.crawling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.crawling.infra.JbnuCourseApiClient;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class CourseCrawlFetcherTest {

    private static final String YEAR = "2026";
    private static final String SEMESTER = "U211600010";

    @Mock
    private JbnuCourseApiClient apiClient;
    @Mock
    private JbnuCourseParser courseParser;

    @InjectMocks
    private CourseCrawlFetcher fetcher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fetcher, "certDivisions", List.of("1", "2", "5", "7", "3", "4"));
        ReflectionTestUtils.setField(fetcher, "interRequestDelayMs", 0L);
    }

    @Test
    @DisplayName("외부 응답 전체를 파싱한 뒤 불변 목록으로 반환한다")
    void fetch_ReturnsCompleteParsedList() {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);
        ParsedCourseDto course = course("course-1");
        given(apiClient.fetchCourseDataStream(eq(YEAR), eq(SEMESTER), anyString())).willReturn(stream);
        given(courseParser.streamCourses(any(), eq(YEAR), eq(SEMESTER)))
                .willReturn(List.of(course).iterator())
                .willReturn(Collections.emptyIterator());

        assertThat(fetcher.fetch(YEAR, SEMESTER)).containsExactly(course);
    }

    @Test
    @DisplayName("전체 응답에 중복 courseKey가 있으면 DB 트랜잭션 시작 전에 거부한다")
    void fetch_RejectsDuplicateCourseKeys() {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);
        ParsedCourseDto first = course("duplicate");
        ParsedCourseDto second = course("duplicate");
        given(apiClient.fetchCourseDataStream(eq(YEAR), eq(SEMESTER), anyString())).willReturn(stream);
        given(courseParser.streamCourses(any(), eq(YEAR), eq(SEMESTER)))
                .willReturn(List.of(first, second).iterator())
                .willReturn(Collections.emptyIterator());

        assertThatThrownBy(() -> fetcher.fetch(YEAR, SEMESTER))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.CRAWLER_PARSING_ERROR));
    }

    @Test
    @DisplayName("지정된 모든 certDivision 파라미터로 순차 호출된다")
    void fetch_CallsExactCertDivisionsInOrder() {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);
        ParsedCourseDto course = course("course-1");
        given(apiClient.fetchCourseDataStream(eq(YEAR), eq(SEMESTER), anyString())).willReturn(stream);
        given(courseParser.streamCourses(any(), eq(YEAR), eq(SEMESTER)))
                .willReturn(List.of(course).iterator())
                .willReturn(Collections.emptyIterator());

        fetcher.fetch(YEAR, SEMESTER);

        org.mockito.Mockito.verify(apiClient).fetchCourseDataStream(YEAR, SEMESTER, "1");
        org.mockito.Mockito.verify(apiClient).fetchCourseDataStream(YEAR, SEMESTER, "2");
        org.mockito.Mockito.verify(apiClient).fetchCourseDataStream(YEAR, SEMESTER, "5");
        org.mockito.Mockito.verify(apiClient).fetchCourseDataStream(YEAR, SEMESTER, "7");
        org.mockito.Mockito.verify(apiClient).fetchCourseDataStream(YEAR, SEMESTER, "3");
        org.mockito.Mockito.verify(apiClient).fetchCourseDataStream(YEAR, SEMESTER, "4");
        org.mockito.Mockito.verify(apiClient, org.mockito.Mockito.times(6)).fetchCourseDataStream(eq(YEAR), eq(SEMESTER), anyString());
    }

    private ParsedCourseDto course(String courseKey) {
        return new ParsedCourseDto(
                courseKey, "COMP101", null, "컴퓨터프로그래밍", "01", null, 50, 20,
                null, YEAR, SEMESTER, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, List.of());
    }
}
