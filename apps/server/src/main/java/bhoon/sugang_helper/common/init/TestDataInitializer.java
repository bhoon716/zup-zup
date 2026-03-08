package bhoon.sugang_helper.common.init;

import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 성능 테스트를 위한 대규모 더미 유저 데이터 초기화 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 100) {
            log.info("[Initialization] 이미 충분한 유저 데이터가 존재합니다. 건너뜁니다.");
            return;
        }

        log.info("[Initialization] 성능 테스트용 더미 유저 25,000명 생성을 시작합니다...");

        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 25000; i++) {
            Role role = (i <= 100) ? Role.ADMIN : Role.USER;
            User user = User.builder()
                    .name("Test User " + i)
                    .email("testuser_" + i + "@jbnu.ac.kr")
                    .role(role)
                    .onboardingCompleted(true)
                    .emailEnabled(true)
                    .fcmEnabled(true)
                    .webPushEnabled(true)
                    .build();
            users.add(user);

            // 1000명 단위로 배치 저장 (Out of Memory 방지)
            if (i % 1000 == 0) {
                userRepository.saveAll(users);
                users.clear();
                log.info("... {}명 저장 완료", i);
            }
        }

        log.info("[Initialization] 유저 데이터 생성 완료!");
    }
}
