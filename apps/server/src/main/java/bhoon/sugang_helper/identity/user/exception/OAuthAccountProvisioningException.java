package bhoon.sugang_helper.identity.user.exception;

public class OAuthAccountProvisioningException extends RuntimeException {

    private final String errorCode;

    public OAuthAccountProvisioningException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
