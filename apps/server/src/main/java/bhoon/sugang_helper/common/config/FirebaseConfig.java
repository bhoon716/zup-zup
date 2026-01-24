package bhoon.sugang_helper.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.config-path}")
    private String configPath;

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                if (configPath == null || configPath.isEmpty()) {
                    log.error("[Firebase] Config path is empty.");
                    return;
                }

                // Docker 환경이나 로컬 환경 어디서든 파일 시스템의 경로를 읽습니다.
                try (FileInputStream serviceAccount = new FileInputStream(configPath)) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp.initializeApp(options);
                    log.info("[Firebase] Successfully initialized using file: {}", configPath);
                }
            }
        } catch (IOException e) {
            log.error("[Firebase] Initialization error: {}", e.getMessage());
        }
    }
}
