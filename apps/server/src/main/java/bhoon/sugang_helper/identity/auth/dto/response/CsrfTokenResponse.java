package bhoon.sugang_helper.identity.auth.dto.response;

public record CsrfTokenResponse(String token) {

    public static CsrfTokenResponse of(String token) {
        return new CsrfTokenResponse(token);
    }
}
