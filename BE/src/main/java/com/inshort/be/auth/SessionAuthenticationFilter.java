package com.inshort.be.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
  private final boolean secureCookie;
  private final Duration sessionTimeout;

  public SessionAuthenticationFilter(
      AuthSessionService authSessionService,
      @Value("${app.auth.cookie-secure:true}") boolean secureCookie,
      @Value("${app.auth.session-timeout:10m}") Duration sessionTimeout) {
    this.authSessionService = authSessionService;
    this.secureCookie = secureCookie;
    this.sessionTimeout = sessionTimeout;
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
                      .ifPresent(
                          userId -> authenticateAndRefresh(request, response, sessionId, userId)));
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

  private void authenticateAndRefresh(
      HttpServletRequest request, HttpServletResponse response, String sessionId, Long userId) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, AUTHORITIES));

    if (shouldRefreshSession(request)) {
      if (authSessionService.refresh(sessionId)) {
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(sessionId).toString());
      }
    }
  }

  private ResponseCookie sessionCookie(String value) {
    return ResponseCookie.from(AuthSessionService.SESSION_COOKIE_NAME, value)
        .httpOnly(true)
        .secure(secureCookie)
        .sameSite("Strict")
        .path("/")
        .maxAge(sessionTimeout)
        .build();
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
