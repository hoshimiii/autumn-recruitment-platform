package dev.recruitops.web;

import dev.recruitops.security.OperationAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
  private final OperationAuthorizer authorizer;
  public AuthController(OperationAuthorizer authorizer) { this.authorizer = authorizer; }

  @GetMapping("/api/v1/auth/session")
  Map<String, Object> session(HttpServletRequest request) {
    CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return Map.of("mode", authorizer.mode(), "authenticated", authorizer.authenticated(),
        "roles", authorizer.roles(), "capabilities", authorizer.capabilities(),
        "csrfToken", csrf == null ? "" : csrf.getToken(), "csrfHeader", csrf == null ? "" : csrf.getHeaderName(),
        "loginUrl", "/oauth2/authorization/recruitops", "logoutUrl", "/api/v1/auth/logout");
  }
}
