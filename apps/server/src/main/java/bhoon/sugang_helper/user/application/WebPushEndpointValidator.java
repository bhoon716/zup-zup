package bhoon.sugang_helper.user.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.net.URI;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WebPushEndpointValidator {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "fcm.googleapis.com",
            "updates.push.services.mozilla.com",
            "web.push.apple.com");

    public void validate(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !ALLOWED_HOSTS.contains(host.toLowerCase())
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getUserInfo() != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "허용되지 않은 Web Push endpoint입니다.");
            }
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "유효하지 않은 Web Push endpoint입니다.");
        }
    }
}
