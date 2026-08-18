package com.inshort.be.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final AuthSessionService authSessionService;
  private final CsrfTokenRepository csrfTokenRepository;
  private final boolean secureCookie;
  private final Duration sessionTimeout;

  public AuthController(
      AuthService authService,
      AuthSessionService authSessionService,
      CsrfTokenRepository csrfTokenRepository,
      @Value("${app.auth.cookie-secure:true}") boolean secureCookie,
      @Value("${app.auth.session-timeout:10m}") Duration sessionTimeout) {
    this.authService = authService;
    this.authSessionService = authSessionService;
    this.csrfTokenRepository = csrfTokenRepository;
    this.secureCookie = secureCookie;
    this.sessionTimeout = sessionTimeout;
  }

  @GetMapping("/csrf")
  public CsrfResponse csrf(HttpServletRequest request, HttpServletResponse response) {
    CsrfToken csrfToken = csrfTokenRepository.loadToken(request);
    if (csrfToken == null) {
      csrfToken = csrfTokenRepository.generateToken(request);
      csrfTokenRepository.saveToken(csrfToken, request, response);
    }
    return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthService.LoginResult result = authService.login(request.phone(), request.pin());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE, sessionCookie(result.session().id(), sessionTimeout).toString())
        .body(
            LoginResponse.from(
                result.user().getId(), result.user().getName(), result.session().expiresAt()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    findSessionId(request).ifPresent(authSessionService::delete);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO).toString())
        .build();
  }

  @GetMapping("/me")
  public LoginResponse me(Authentication authentication, HttpServletRequest request) {
    if (!(authentication.getPrincipal() instanceof Long userId)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    var user = authService.findUser(userId);
    Instant expiresAt =
        findSessionId(request)
            .flatMap(authSessionService::expiresAt)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated"));
    return LoginResponse.from(user.getId(), user.getName(), expiresAt);
  }

  private ResponseCookie sessionCookie(String value, Duration maxAge) {
    return ResponseCookie.from(AuthSessionService.SESSION_COOKIE_NAME, value)
        .httpOnly(true)
        .secure(secureCookie)
        .sameSite("Strict")
        .path("/")
        .maxAge(maxAge)
        .build();
  }

  private java.util.Optional<String> findSessionId(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return java.util.Optional.empty();
    }
    return java.util.Arrays.stream(cookies)
        .filter(cookie -> AuthSessionService.SESSION_COOKIE_NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst();
  }

  public record LoginRequest(
      @NotBlank
          @Pattern(
              regexp = "010\\d{8}",
              message = "Phone number must start with 010 and contain 11 digits")
          String phone,
      @NotBlank @Pattern(regexp = "\\d{6}", message = "PIN must be six digits") String pin) {}

  public record LoginResponse(Long userId, String name, Instant expiresAt) {
    static LoginResponse from(Long userId, String name, Instant expiresAt) {
      return new LoginResponse(userId, name, expiresAt);
    }
  }

  public record CsrfResponse(String headerName, String token) {}
}
