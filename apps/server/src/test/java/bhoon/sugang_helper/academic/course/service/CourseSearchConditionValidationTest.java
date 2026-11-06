package bhoon.sugang_helper.academic.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.academic.course.dto.request.CourseSearchCondition;
import bhoon.sugang_helper.academic.course.dto.request.ScheduleCondition;
import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class CourseSearchConditionValidationTest {

    private static final String ERROR_CODE_FIELD = "errorCode";
    private static final String NINE_AM = "09:00";
    private static final String TEN_AM = "10:00";

    @Test
    @DisplayName("Bean Validation은 과도한 목록과 잘못된 정렬 조건을 거부한다")
    void beanValidationRejectsOversizedListsAndInvalidSort() {
        // given
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .classifications(List.of("x".repeat(51)))
                .sortBy("unsupported")
                .build();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // when & then
        assertThat(validator.validate(condition)).isNotEmpty();
    }

    @Test
    @DisplayName("잘못된 검색 분류는 조회 조건을 만들기 전에 거부한다")
    void enumAndRangeValuesAreRejectedBeforeQueryConstruction() {
        // given
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .classifications(List.of("없는 분류"))
                .credits(List.of("not-a-credit"))
                .build();

        // when & then
        assertThatThrownBy(condition::validateSearchValues)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("통합 검색어와 강의명 필터를 동시에 사용하지 못한다")
    void integratedKeywordCannotBeCombinedWithNameFilter() {
        // given
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .keyword("GECO178")
                .name("자료구조")
                .build();

        // when & then
        assertThatThrownBy(condition::validateSearchValues)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("수업 운영 방식은 조회 조건 생성 전에 정규화하고 중복을 제거한다")
    void courseDirectionTermsAreNormalizedAndDeduplicatedBeforeCriteriaCreation() {
        // given
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .courseDirection("  대면수업 ; 실시간\n대면수업,  혼합수업  ")
                .build();

        // when
        condition.validateSearchValues();

        // then
        assertThat(condition.getCourseDirection()).isEqualTo("대면수업,실시간,혼합수업");
    }

    @Test
    @DisplayName("고유한 수업 운영 방식이 제한 개수를 넘으면 거부한다")
    void courseDirectionRejectsTooManyUniqueTerms() {
        // given
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .courseDirection("a,b,c,d,e,f,g,h,i,j,k")
                .build();

        // when & then
        assertThatThrownBy(condition::validateSearchValues)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("학점은 숫자가 아니거나 허용 범위를 벗어나면 거부한다")
    void creditsRejectInvalidNumbersAndRanges() {
        // given
        List<String> invalidCredits = List.of("not-a-credit", "-1", "31");

        // when & then
        for (String credit : invalidCredits) {
            CourseSearchCondition condition = CourseSearchCondition.builder().credits(List.of(credit)).build();
            assertThatThrownBy(condition::validateSearchValues)
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.INVALID_INPUT)
                    .hasFieldOrPropertyWithValue("detail", "검색 조건이 올바르지 않습니다: credits");
        }
    }

    @Test
    @DisplayName("학점의 최솟값과 최댓값 및 소수는 그대로 허용한다")
    void creditsKeepBoundaryAndFractionalValues() {
        // given
        List<String> credits = List.of("0", "3.5", "30");
        CourseSearchCondition condition = CourseSearchCondition.builder().credits(credits).build();

        // when
        condition.validateSearchValues();

        // then
        assertThat(condition.getCredits()).isEqualTo(credits);
    }

    @Test
    @DisplayName("선택 일정은 누락된 값과 잘못되거나 역전된 시간 범위를 거부한다")
    void selectedSchedulesRejectMissingAndInvalidTimeRanges() {
        // given
        List<ScheduleCondition> invalidSchedules = Arrays.asList(null,
                new ScheduleCondition(null, NINE_AM, TEN_AM),
                new ScheduleCondition(CourseDayOfWeek.MONDAY, null, TEN_AM),
                new ScheduleCondition(CourseDayOfWeek.MONDAY, "invalid", TEN_AM),
                new ScheduleCondition(CourseDayOfWeek.MONDAY, NINE_AM, NINE_AM),
                new ScheduleCondition(CourseDayOfWeek.MONDAY, TEN_AM, NINE_AM));

        // when & then
        for (ScheduleCondition schedule : invalidSchedules) {
            CourseSearchCondition condition = CourseSearchCondition.builder()
                    .selectedSchedules(Collections.singletonList(schedule)).build();
            assertThatThrownBy(condition::validateSearchValues)
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.INVALID_INPUT)
                    .hasFieldOrPropertyWithValue("detail", "검색 조건이 올바르지 않습니다: selectedSchedules");
        }
    }

}
