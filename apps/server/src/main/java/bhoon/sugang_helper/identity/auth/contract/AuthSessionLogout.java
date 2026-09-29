package bhoon.sugang_helper.identity.auth.contract;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthSessionLogout {

    void logout(HttpServletRequest request, HttpServletResponse response);
}
