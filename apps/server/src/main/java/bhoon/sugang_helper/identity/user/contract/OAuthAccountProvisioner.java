package bhoon.sugang_helper.identity.user.contract;

public interface OAuthAccountProvisioner {

    AuthenticatedUser findOrCreate(String issuer, String subject, String email, String name);
}
