package bhoon.sugang_helper.identity.user.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserSessionLogout {

    void logout(HttpServletRequest request, HttpServletResponse response);
}
