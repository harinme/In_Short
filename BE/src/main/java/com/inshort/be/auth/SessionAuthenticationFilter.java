package com.inshort.be.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

  private static final List<SimpleGrantedAuthority> AUTHORITIES =
      List.of(new SimpleGrantedAuthority("ROLE_USER"));

  private final AuthSessionService authSessionService;

  public SessionAuthenticationFilter(AuthSessionService authSessionService) {
    this.authSessionService = authSessionService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      findSessionId(request)
          .ifPresent(
              sessionId ->
                  authSessionService
                      .findUserId(sessionId)
                      .ifPresent(userId -> authenticateAndRefresh(request, sessionId, userId)));
    }
    filterChain.doFilter(request, response);
  }

  private java.util.Optional<String> findSessionId(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return java.util.Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(cookie -> AuthSessionService.SESSION_COOKIE_NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst();
  }

  private void authenticateAndRefresh(HttpServletRequest request, String sessionId, Long userId) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, AUTHORITIES));

    if (shouldRefreshSession(request)) {
      authSessionService.refresh(sessionId);
    }
  }

  private boolean shouldRefreshSession(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.equals("/api/auth/csrf")
        && !path.equals("/api/auth/login")
        && !path.equals("/api/auth/logout")
        && !path.startsWith("/api/voice-conversations")
        && !path.equals("/api/ai/test")
        && !path.startsWith("/actuator")
        && !path.startsWith("/swagger-ui")
        && !path.startsWith("/v3/api-docs");
  }
}
