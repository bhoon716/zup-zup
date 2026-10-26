package bhoon.sugang_helper.academic.crawling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseDto;
import bhoon.sugang_helper.academic.crawling.client.CourseApiClient;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseCrawlFetcherTest {

    private static final String YEAR = "2026";
    private static final String SEMESTER = "U211600025";
    private static final String SUBJECT_CODE = "COMP101";
    private static final List<String> CERT_DIVISIONS = List.of("1", "2", "5", "7", "3", "4");
    private static final String EMPTY_RESPONSE = "{\"dsEstSbjList\":[]}";

    @Mock
    private CourseApiClient apiClient;

    private CourseCrawlFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new CourseCrawlFetcher(apiClient, new JbnuCourseParser());
        ReflectionTestUtils.setField(fetcher, "certDivisions", CERT_DIVISIONS);
        ReflectionTestUtils.setField(fetcher, "interRequestDelayMs", 0L);
    }

    @Test
    @DisplayName("실제 파서로 전체 응답을 파싱한 뒤 불변 목록으로 반환한다")
    void fetch_ReturnsCompleteParsedList() {

        stubResponses(Map.of("1", courseResponse(SUBJECT_CODE), "2", courseResponse("COMP102")));


        List<ParsedCourseDto> courses = fetcher.fetch(YEAR, SEMESTER);


        assertThat(courses).extracting(ParsedCourseDto::courseKey).containsExactly(
                "2026:U211600025:COMP101:01", "2026:U211600025:COMP102:01");
        assertThatThrownBy(() -> courses.add(courses.get(0))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("서로 다른 분류의 courseKey가 중복이면 빈 분류가 있어도 거부한다")
    void fetch_RejectsDuplicateCourseKeys() {

        stubResponses(Map.of("2", courseResponse(SUBJECT_CODE), "3", courseResponse(SUBJECT_CODE)));


        assertThatThrownBy(() -> fetcher.fetch(YEAR, SEMESTER))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.CRAWLER_PARSING_ERROR));
    }

    @Test
    @DisplayName("빈 분류가 있어도 모든 certDivision을 설정 순서대로 조회한다")
    void fetch_CallsExactCertDivisionsInOrder() {

        stubResponses(Map.of("1", courseResponse(SUBJECT_CODE)));


        fetcher.fetch(YEAR, SEMESTER);


        assertAllDivisionsRequestedInOrder();
    }

    @ParameterizedTest(name = "certDiv={0}이 비어 있어도 나머지 강의를 모두 반환한다")
    @ValueSource(strings = {"1", "2", "5", "7", "3", "4"})
    @DisplayName("첫 번째·중간·마지막 분류의 정상 빈 응답을 허용한다")
    void fetch_AllowsEmptyDivisionAtAnyPosition(String emptyDivision) {

        given(apiClient.fetchCourseDataStream(eq(YEAR), eq(SEMESTER), anyString()))
                .willAnswer(invocation -> {
                    String division = invocation.getArgument(2);
                    return responseStream(division.equals(emptyDivision)
                            ? EMPTY_RESPONSE : courseResponse("COMP" + division));
                });


        List<ParsedCourseDto> courses = fetcher.fetch(YEAR, SEMESTER);


        assertThat(courses).extracting(ParsedCourseDto::subjectCode)
                .containsExactlyElementsOf(CERT_DIVISIONS.stream()
                        .filter(division -> !division.equals(emptyDivision))
                        .map(division -> "COMP" + division)
                        .toList());
        assertAllDivisionsRequestedInOrder();
    }

    @Test
    @DisplayName("모든 분류가 정상 빈 배열이면 전체 조회 후 C003으로 실패한다")
    void fetch_RejectsEmptyCombinedResult() {

        stubResponses(Map.of());


        assertThatThrownBy(() -> fetcher.fetch(YEAR, SEMESTER))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.CRAWLER_NO_DATA));
        assertAllDivisionsRequestedInOrder();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDivisionResponses")
    @DisplayName("빈 분류와 정상 강의 뒤에서 실제 오류 응답이 오면 전체 조회가 실패한다")
    void fetch_RejectsInvalidResponseAfterEmptyDivision(String description, String response, ErrorCode errorCode) {

        stubResponses(Map.of("2", courseResponse(SUBJECT_CODE), "5", response));


        assertThatThrownBy(() -> fetcher.fetch(YEAR, SEMESTER))
                .as(description)
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode()).isEqualTo(errorCode));
        verify(apiClient).fetchCourseDataStream(YEAR, SEMESTER, "5");
        verify(apiClient, never()).fetchCourseDataStream(YEAR, SEMESTER, "7");
    }

    @Test
    @DisplayName("빈 분류가 있어도 필수 강의 정보가 누락되면 전체 결과를 거부한다")
    void fetch_RejectsMissingRequiredFieldAfterEmptyDivision() {

        stubResponses(Map.of("2", "{\"dsEstSbjList\":[{\"SBJCT_CD\":\"COMP101\",\"DVCLS_NO\":\"01\"}]}"));


        assertThatThrownBy(() -> fetcher.fetch(YEAR, SEMESTER))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.CRAWLER_PARSING_ERROR));
        assertAllDivisionsRequestedInOrder();
    }

    @Test
    @DisplayName("빈 분류 이후 통신에 실패하면 수집을 중단하고 원래 예외를 전달한다")
    void fetch_PropagatesUpstreamFailureAfterEmptyDivision() {

        CustomException failure = new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR);
        given(apiClient.fetchCourseDataStream(YEAR, SEMESTER, "1")).willReturn(responseStream(EMPTY_RESPONSE));
        given(apiClient.fetchCourseDataStream(YEAR, SEMESTER, "2"))
                .willReturn(responseStream(courseResponse(SUBJECT_CODE)));
        given(apiClient.fetchCourseDataStream(YEAR, SEMESTER, "5")).willThrow(failure);


        assertThatThrownBy(() -> fetcher.fetch(YEAR, SEMESTER)).isSameAs(failure);
        verify(apiClient, never()).fetchCourseDataStream(YEAR, SEMESTER, "7");
    }

    private static Stream<Arguments> invalidDivisionResponses() {
        return Stream.of(
                Arguments.of("오류 envelope와 빈 배열", "{\"ERRMSGINFO\":{},\"dsEstSbjList\":[]}",
                        ErrorCode.CRAWLER_CONNECTION_ERROR),
                Arguments.of("목록 필드 누락", "{}", ErrorCode.CRAWLER_PARSING_ERROR),
                Arguments.of("배열 대신 객체", "{\"dsEstSbjList\":{}}", ErrorCode.CRAWLER_PARSING_ERROR),
                Arguments.of("배열 대신 null", "{\"dsEstSbjList\":null}", ErrorCode.CRAWLER_PARSING_ERROR),
                Arguments.of("잘못된 JSON", "{\"dsEstSbjList\":[", ErrorCode.CRAWLER_PARSING_ERROR),
                Arguments.of("강의 식별자 누락", "{\"dsEstSbjList\":[{\"DVCLS_NO\":\"01\"}]}",
                        ErrorCode.CRAWLER_PARSING_ERROR));
    }

    private void stubResponses(Map<String, String> responses) {
        given(apiClient.fetchCourseDataStream(eq(YEAR), eq(SEMESTER), anyString()))
                .willAnswer(invocation -> responseStream(responses.getOrDefault(invocation.getArgument(2), EMPTY_RESPONSE)));
    }

    private void assertAllDivisionsRequestedInOrder() {
        var ordered = inOrder(apiClient);
        for (String division : CERT_DIVISIONS) {
            ordered.verify(apiClient).fetchCourseDataStream(YEAR, SEMESTER, division);
        }
        ordered.verifyNoMoreInteractions();
    }

    private ByteArrayInputStream responseStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    private String courseResponse(String subjectCode) {
        return """
                {"dsEstSbjList":[{
                  "SBJCT_CD":"%s",
                  "DVCLS_NO":"01",
                  "SBJCT_NM":"컴퓨터프로그래밍",
                  "PRM_NMPR_CNT":50,
                  "TLSN_RCNT":20
                }]}
                """.formatted(subjectCode);
    }
}
