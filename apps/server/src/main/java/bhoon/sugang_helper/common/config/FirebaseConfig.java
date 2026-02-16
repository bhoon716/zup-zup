package bhoon.sugang_helper.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.config-path}")
    private String configPath;

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }
            if (configPath == null || configPath.isEmpty()) {
                log.error("[Firebase] 설정 파일 경로가 비어 있습니다.");
                return;
            }

            // 컨테이너/로컬 환경 모두 파일 시스템 경로를 기준으로 읽는다.
            try (FileInputStream serviceAccount = new FileInputStream(configPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("[Firebase] 파일 기반 초기화를 완료했습니다. 경로={}", configPath);
            }
        } catch (IOException e) {
            log.error("[Firebase] 초기화 중 오류가 발생했습니다: {}", e.getMessage(), e);
        }
    }
}
