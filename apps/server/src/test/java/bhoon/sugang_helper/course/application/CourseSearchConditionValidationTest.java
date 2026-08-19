package bhoon.sugang_helper.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class CourseSearchConditionValidationTest {

    @Test
    void beanValidationRejectsOversizedListsAndInvalidSort() {
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .classifications(List.of("x".repeat(51)))
                .sortBy("unsupported")
                .build();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        assertThat(validator.validate(condition)).isNotEmpty();
    }

    @Test
    void enumAndRangeValuesAreRejectedBeforeQueryConstruction() {
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .classifications(List.of("없는 분류"))
                .credits(List.of("not-a-credit"))
                .build();

        assertThatThrownBy(condition::validateSearchValues)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void integratedKeywordCannotBeCombinedWithNameFilter() {
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .keyword("GECO178")
                .name("자료구조")
                .build();

        assertThatThrownBy(condition::validateSearchValues)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void courseDirectionTermsAreNormalizedAndDeduplicatedBeforeCriteriaCreation() {
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .courseDirection("  대면수업 ; 실시간\n대면수업,  혼합수업  ")
                .build();

        condition.validateSearchValues();

        assertThat(condition.getCourseDirection()).isEqualTo("대면수업,실시간,혼합수업");
    }

    @Test
    void courseDirectionRejectsTooManyUniqueTerms() {
        CourseSearchCondition condition = CourseSearchCondition.builder()
                .courseDirection("a,b,c,d,e,f,g,h,i,j,k")
                .build();

        assertThatThrownBy(condition::validateSearchValues)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
