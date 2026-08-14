package com.inshort.be.auth;

import com.inshort.be.user.entity.User;
import com.inshort.be.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private static final String DUMMY_PIN_HASH =
      "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthSessionService authSessionService;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthSessionService authSessionService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authSessionService = authSessionService;
  }

  public LoginResult login(String phone, String pin) {
    User user = userRepository.findByPhone(phone).orElse(null);
    String pinHash = user == null ? DUMMY_PIN_HASH : user.getPinHash();
    boolean pinMatches = passwordEncoder.matches(pin, pinHash);

    if (user == null || !pinMatches) {
      throw invalidCredentials();
    }

    AuthSessionService.Session session = authSessionService.create(user.getId());
    return new LoginResult(user, session);
  }

  public User findUser(Long userId) {
    return userRepository.findById(userId).orElseThrow(this::invalidCredentials);
  }

  private ResponseStatusException invalidCredentials() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid phone or PIN");
  }

  public record LoginResult(User user, AuthSessionService.Session session) {}
}
