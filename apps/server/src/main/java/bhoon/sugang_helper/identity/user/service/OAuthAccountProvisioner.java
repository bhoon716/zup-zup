package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.user.dto.AuthenticatedUser;

public interface OAuthAccountProvisioner {

    AuthenticatedUser findOrCreate(String issuer, String subject, String email, String name);
}
