package com.vnpay.sit.auth;

import com.vnpay.sit.model.UserRole;
import com.vnpay.sit.user.entity.SitUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

  private static final String SECRET = "test-secret-key-with-32-chars-minimum";

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(SECRET, 3_600_000L);
  }

  @Test
  void constructor_shouldRejectMissingSecret() {
    assertThatThrownBy(() -> new JwtService("", 3_600_000L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SIT_JWT_SECRET");
  }

  @Test
  void constructor_shouldRejectShortSecret() {
    assertThatThrownBy(() -> new JwtService("too-short", 3_600_000L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("32 ký tự");
  }

  @Test
  void generateToken_shouldEmbedClaims() {
    SitUser user = user("admin@vnpay.vn", UserRole.ADMIN, 42L);

    String token = jwtService.generateToken(user);

    assertThat(jwtService.extractEmail(token)).isEqualTo("admin@vnpay.vn");
    assertThat(jwtService.extractRole(token)).isEqualTo(UserRole.ADMIN);
    assertThat(jwtService.isTokenValid(token)).isTrue();
  }

  @Test
  void isTokenValid_shouldRejectTamperedToken() {
    SitUser user = user("qc@merchant.com", UserRole.MERCHANT_QC, 7L);
    String token = jwtService.generateToken(user);

    assertThat(jwtService.isTokenValid(token + "x")).isFalse();
  }

  private static SitUser user(String email, UserRole role, Long id) {
    SitUser user = new SitUser();
    user.setId(id);
    user.setEmail(email);
    user.setRole(role);
    user.setActive(true);
    return user;
  }
}
