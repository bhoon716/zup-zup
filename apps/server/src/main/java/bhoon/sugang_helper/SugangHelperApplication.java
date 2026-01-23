package bhoon.sugang_helper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.data.jpa.repository.config.EnableJpaAuditing
public class SugangHelperApplication {

	public static void main(String[] args) {
		SpringApplication.run(SugangHelperApplication.class, args);
	}

}
