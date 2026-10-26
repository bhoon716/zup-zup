package bhoon.sugang_helper.common.config;

import bhoon.sugang_helper.common.security.util.SensitiveDataRedactor;
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
                log.error("[Firebase] Configuration file path is empty.");
                return;
            }


            try (FileInputStream serviceAccount = new FileInputStream(configPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("[Firebase] File-based initialization completed.");
            }
        } catch (IOException e) {
            log.error("[Firebase] Initialization failed. exceptionType={}", SensitiveDataRedactor.exceptionType(e));
        }
    }
}
