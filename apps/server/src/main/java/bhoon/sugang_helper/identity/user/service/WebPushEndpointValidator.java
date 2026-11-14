package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class WebPushEndpointValidator {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "fcm.googleapis.com",
            "updates.push.services.mozilla.com",
            "web.push.apple.com");
    private static final String CHROMIUM_STAGING_HOST = "jmt17.google.com";
    private static final Pattern CHROMIUM_STAGING_PATH = Pattern.compile("/fcm/send/[A-Za-z0-9_:-]+");

    public void validate(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getUserInfo() != null
                    || !isAllowedProvider(uri, host)) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "허용되지 않은 Web Push endpoint입니다.");
            }
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "유효하지 않은 Web Push endpoint입니다.");
        }
    }

    private boolean isAllowedProvider(URI uri, String host) {
        if (CHROMIUM_STAGING_HOST.equalsIgnoreCase(host)) {
            return uri.getRawQuery() == null && uri.getRawFragment() == null
                    && CHROMIUM_STAGING_PATH.matcher(uri.getRawPath()).matches();
        }
        return ALLOWED_HOSTS.contains(host.toLowerCase(Locale.ROOT));
    }
}
