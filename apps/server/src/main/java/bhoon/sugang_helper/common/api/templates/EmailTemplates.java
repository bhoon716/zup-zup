package bhoon.sugang_helper.common.api.templates;

import java.util.Map;

public interface EmailTemplates {

    String loadTemplate(String templateName, Map<String, String> values);
}
