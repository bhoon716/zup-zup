package bhoon.sugang_helper.crawling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.course.domain.ParsedCourseDto;
import bhoon.sugang_helper.crawling.infra.JbnuCourseApiClient;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    @DisplayName("외부 응답 전체를 파싱한 뒤 불변 목록으로 반환한다")
    void fetch_ReturnsCompleteParsedList() {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);
        ParsedCourseDto course = course("course-1");
        given(apiClient.fetchCourseDataStream(YEAR, SEMESTER)).willReturn(stream);
        given(courseParser.streamCourses(stream, YEAR, SEMESTER)).willReturn(List.of(course).iterator());

        assertThat(fetcher.fetch(YEAR, SEMESTER)).containsExactly(course);
    }

    @Test
    @DisplayName("전체 응답에 중복 courseKey가 있으면 DB 트랜잭션 시작 전에 거부한다")
    void fetch_RejectsDuplicateCourseKeys() {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);
        ParsedCourseDto first = course("duplicate");
        ParsedCourseDto second = course("duplicate");
        given(apiClient.fetchCourseDataStream(YEAR, SEMESTER)).willReturn(stream);
        given(courseParser.streamCourses(stream, YEAR, SEMESTER))
                .willReturn(List.of(first, second).iterator());

        assertThatThrownBy(() -> fetcher.fetch(YEAR, SEMESTER))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.CRAWLER_PARSING_ERROR));
    }

    private ParsedCourseDto course(String courseKey) {
        return new ParsedCourseDto(
                courseKey, "COMP101", null, "컴퓨터프로그래밍", "01", null, 50, 20,
                null, YEAR, SEMESTER, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, List.of());
    }
}
