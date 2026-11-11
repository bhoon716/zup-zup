package bhoon.sugang_helper.planning.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import bhoon.sugang_helper.academic.api.catalog.CourseCatalog;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.planning.timetable.dto.request.TimetableRequest;
import bhoon.sugang_helper.planning.timetable.dto.response.TimetableResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("migration")
@Testcontainers
@DataJpaTest(showSql = false, properties = {
    "spring.autoconfigure.exclude=",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TimetableService.class, UserIdentityQueryService.class})
@MockitoBean(types = CourseCatalog.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TimetablePrimaryMigrationTest {

    private static final String SWITCH_USER_EMAIL = "switch-primary@example.invalid";
    private static final String CREATE_USER_EMAIL = "create-primary@example.invalid";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void dataSource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Test
    @DisplayName("기존 대표 시간표를 다른 시간표로 변경하고 저장한다")
    @WithMockUser(username = SWITCH_USER_EMAIL)
    void switchesPrimaryTimetable() {
        // given
        saveUser(SWITCH_USER_EMAIL);
        TimetableResponse next = timetableService.createTimetable(new TimetableRequest("변경할 시간표", false));
        TimetableResponse previous = timetableService.createTimetable(new TimetableRequest("기존 대표 시간표", true));

        // when
        timetableService.setPrimary(next.getId());

        // then
        assertPrimaryTimetable(next, previous);
    }

    @Test
    @DisplayName("새 시간표를 대표로 생성하면 기존 시간표의 대표 설정을 해제한다")
    @WithMockUser(username = CREATE_USER_EMAIL)
    void createsTimetableAsPrimary() {
        // given
        saveUser(CREATE_USER_EMAIL);
        TimetableResponse previous = timetableService.createTimetable(new TimetableRequest("기존 대표 시간표", true));

        // when
        TimetableResponse next = timetableService.createTimetable(new TimetableRequest("새 대표 시간표", true));

        // then
        assertPrimaryTimetable(next, previous);
    }

    private void saveUser(String email) {
        userRepository.save(User.builder().email(email).name("시간표 사용자").role(Role.USER).build());
    }

    private void assertPrimaryTimetable(TimetableResponse next, TimetableResponse previous) {
        assertThat(timetableService.getMyTimetables())
                .extracting(TimetableResponse::getId, TimetableResponse::isPrimary)
                .containsExactlyInAnyOrder(tuple(next.getId(), true), tuple(previous.getId(), false));
    }
}
