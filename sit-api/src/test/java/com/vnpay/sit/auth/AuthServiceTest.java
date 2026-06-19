package com.vnpay.sit.auth;

import com.vnpay.sit.api.dto.AuthResponse;
import com.vnpay.sit.api.dto.UserResponse;
import com.vnpay.sit.auth.dto.LoginRequest;
import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.user.entity.SitUser;
import com.vnpay.sit.user.repository.SitUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private SitUserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userRepository, passwordEncoder, jwtService);
  }

  @Test
  void login_shouldReturnTokenForValidCredentials() {
    LoginRequest request = new LoginRequest();
    request.setEmail("  Admin@Vnpay.VN  ");
    request.setPassword("secret");

    SitUser user = activeUser("admin@vnpay.vn", UserRole.ADMIN, "$2a$hash");
    when(userRepository.findByEmailIgnoreCase("admin@vnpay.vn"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("secret", "$2a$hash")).thenReturn(true);
    when(jwtService.generateToken(user)).thenReturn("jwt-token");

    AuthResponse response = authService.login(request);

    assertThat(response.getToken()).isEqualTo("jwt-token");
    assertThat(response.getUser().getEmail()).isEqualTo("admin@vnpay.vn");
    assertThat(response.getUser().getRole()).isEqualTo(UserRole.ADMIN);
    verify(jwtService).generateToken(user);
  }

  @Test
  void login_shouldRejectUnknownEmail() {
    LoginRequest request = new LoginRequest();
    request.setEmail("missing@vnpay.vn");
    request.setPassword("secret");
    when(userRepository.findByEmailIgnoreCase("missing@vnpay.vn"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email hoặc mật khẩu không đúng");
  }

  @Test
  void login_shouldRejectInactiveUser() {
    LoginRequest request = new LoginRequest();
    request.setEmail("inactive@vnpay.vn");
    request.setPassword("secret");

    SitUser user = activeUser("inactive@vnpay.vn", UserRole.MERCHANT_QC, "$2a$hash");
    user.setActive(false);
    when(userRepository.findByEmailIgnoreCase("inactive@vnpay.vn"))
        .thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("vô hiệu hóa");
  }

  @Test
  void login_shouldRejectWrongPassword() {
    LoginRequest request = new LoginRequest();
    request.setEmail("qc@merchant.com");
    request.setPassword("wrong");

    SitUser user = activeUser("qc@merchant.com", UserRole.MERCHANT_QC, "$2a$hash");
    when(userRepository.findByEmailIgnoreCase("qc@merchant.com"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email hoặc mật khẩu không đúng");
  }

  @Test
  void currentUser_shouldMapPrincipal() {
    SitUser user = activeUser("qc@merchant.com", UserRole.MERCHANT_QC, "$2a$hash");
    user.setFullName("QC User");
    SitUserPrincipal principal = new SitUserPrincipal(user);

    UserResponse response = authService.currentUser(principal);

    assertThat(response.getEmail()).isEqualTo("qc@merchant.com");
    assertThat(response.getFullName()).isEqualTo("QC User");
    assertThat(response.getRole()).isEqualTo(UserRole.MERCHANT_QC);
  }

  private static SitUser activeUser(String email, UserRole role, String passwordHash) {
    SitUser user = new SitUser();
    user.setId(1L);
    user.setEmail(email);
    user.setRole(role);
    user.setPasswordHash(passwordHash);
    user.setActive(true);
    return user;
  }
}
