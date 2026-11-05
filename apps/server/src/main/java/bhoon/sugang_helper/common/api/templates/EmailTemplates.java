package bhoon.sugang_helper.common.api.templates;

import java.util.Map;

/** 다른 모듈이 사용하는 EmailTemplates 계약. */
public interface EmailTemplates {

    String loadTemplate(String templateName, Map<String, String> values);
}
