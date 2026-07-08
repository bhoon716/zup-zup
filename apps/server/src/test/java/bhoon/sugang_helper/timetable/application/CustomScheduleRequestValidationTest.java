package bhoon.sugang_helper.timetable.application;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.timetable.application.command.CustomScheduleRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomScheduleRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("커스텀 일정 시간대 목록은 비어있을 수 없다")
    void schedulesMustNotBeEmpty() {
        CustomScheduleRequest request = new CustomScheduleRequest(
                "점심",
                null,
                List.of());

        Set<ConstraintViolation<CustomScheduleRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("schedules");
                    assertThat(violation.getMessage()).isEqualTo("최소 하나 이상의 시간대를 선택해주세요.");
                });
    }

    @Test
    @DisplayName("커스텀 일정의 개별 시간대 항목도 검증한다")
    void scheduleItemsAreValidated() {
        CustomScheduleRequest request = new CustomScheduleRequest(
                "점심",
                null,
                List.of(new CustomScheduleRequest.CustomScheduleTimeRequest(
                        "",
                        null,
                        LocalTime.of(10, 0),
                        null)));

        Set<ConstraintViolation<CustomScheduleRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("schedules[0].dayOfWeek");
                    assertThat(violation.getMessage()).isEqualTo("요일을 입력해주세요.");
                });
    }
}
