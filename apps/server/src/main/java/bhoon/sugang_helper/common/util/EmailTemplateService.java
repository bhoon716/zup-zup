package bhoon.sugang_helper.common.util;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

/**
 * 이메일 HTML 템플릿을 로드하고 변수를 치환하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private static final String TEMPLATE_PATH = "templates/email/";

    /**
     * 템플릿 파일을 읽어와서 변수({{key}})를 value로 치환합니다.
     */
    public String loadTemplate(String templateName, Map<String, String> values) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH + templateName + ".html");
            InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            String content = FileCopyUtils.copyToString(reader);

            for (Map.Entry<String, String> entry : values.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            return content;
        } catch (Exception e) {
            log.error("[EmailTemplate] Failed to load template: {}", templateName, e);
            return "Content Error: " + values.toString();
        }
    }
}
