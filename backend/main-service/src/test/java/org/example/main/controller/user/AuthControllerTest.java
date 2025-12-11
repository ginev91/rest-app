package org.example.main.controller.user;

import jakarta.servlet.http.HttpSession;
import org.example.main.dto.request.user.LoginRequestDto;
import org.example.main.dto.request.user.RegisterRequestDto;
import org.example.main.dto.response.user.AuthResponseDto;
import org.example.main.security.JwtUtils;
import org.example.main.service.user.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    @Mock
    private IUserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        
        lenient().when(jwtUtils.getJwtExpirationMs()).thenReturn(3600000L);
    }

    @Test
    void login_setsCookie_and_returnsBody_withDiagnostic() {
        LoginRequestDto dto = LoginRequestDto.builder()
                .username("alice@test.com")
                .password("pass")
                .tableNumber(1)
                .tablePin("1111")
                .build();

        AuthResponseDto respDto = AuthResponseDto.builder()
                .token("tok")
                .username("alice@test.com")
                .userId(UUID.randomUUID())
                .role("ROLE_USER")
                .build();

        
        doAnswer(invocation -> respDto)
                .when(userService).login(any(LoginRequestDto.class), any());

        HttpSession session = mock(HttpSession.class);

        ResponseEntity<AuthResponseDto> resp = authController.login(dto, session);

        verify(userService, times(1)).login(any(LoginRequestDto.class), any());

        assertThat(resp).isNotNull();
        String setCookie = resp.getHeaders().getFirst("Set-Cookie");
        assertThat(setCookie).isNotNull();
        
        assertThat(setCookie).contains("access_token=");
        
        assertThat(setCookie).contains("Max-Age=3600").contains("HttpOnly").contains("SameSite=Lax");
        assertThat(resp.getBody()).isEqualTo(respDto);
    }

    @Test
    void logout_clearsCookie() {
        ResponseEntity<Void> resp = authController.logout();

        assertThat(resp).isNotNull();
        String setCookie = resp.getHeaders().getFirst("Set-Cookie");
        assertThat(setCookie).isNotNull();
        
        assertThat(setCookie).contains("access_token=").contains("Max-Age=0").contains("HttpOnly");
    }

    @Test
    void register_returnsAuthResponseBody() {
        var reg = new RegisterRequestDto();
        reg.setUsername("newuser@example.com");
        reg.setPassword("pw");
        reg.setFullName("New User");

        AuthResponseDto respDto = AuthResponseDto.builder()
                .token("tok2")
                .username("newuser@example.com")
                .userId(UUID.randomUUID())
                .role("ROLE_USER")
                .build();

        when(userService.register(reg)).thenReturn(respDto);

        ResponseEntity<?> r = authController.register(reg);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(r.getBody()).isEqualTo(respDto);
    }

    @Test
    void me_returnsOk_whenAuthenticated() {
        Authentication auth = mock(Authentication.class);
        HttpSession session = mock(HttpSession.class);

        Map<String, Object> dto = Map.of(
                "username", "alice@test.com",
                "userId", UUID.randomUUID()
        );

        when(userService.me(auth, session)).thenReturn(dto);

        ResponseEntity<?> resp = authController.me(auth, session);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("username", "alice@test.com");
        assertThat(body).containsKey("userId");
    }

    @Test
    void me_returns401_whenUnauthenticated() {
        Authentication auth = mock(Authentication.class);
        HttpSession session = mock(HttpSession.class);

        when(userService.me(auth, session)).thenThrow(new IllegalArgumentException("Unauthenticated"));

        ResponseEntity<?> resp = authController.me(auth, session);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCodeValue()).isEqualTo(401);
        assertThat(resp.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "Unauthenticated");
    }

    @Test
    void me_returns500_whenServiceThrowsUnexpected() {
        Authentication auth = mock(Authentication.class);
        HttpSession session = mock(HttpSession.class);

        when(userService.me(auth, session)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> resp = authController.me(auth, session);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCodeValue()).isEqualTo(500);
        assertThat(resp.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body).containsEntry("message", "Internal server error");
    }
}