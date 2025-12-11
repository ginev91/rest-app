package org.example.main.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtUtilsTest {

    private static final String SECRET = "0123456789abcdef0123456789ABCDEF"; // 32 chars

    @AfterEach
    void cleanup() {
    }

    @Test
    void constructor_rejectsBlankOrShortSecret() {
        assertThatThrownBy(() -> new JwtUtils("", 1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtUtils("short-secret", 1000)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generate_validate_and_getUsername_work() {
        JwtUtils ju = new JwtUtils(SECRET, 3600000);
        String token = ju.generateToken("alice", List.of("ADMIN", "USER"));

        assertThat(token).isNotBlank();
        assertThat(ju.validateToken(token)).isTrue();
        assertThat(ju.getUsernameFromToken(token)).isEqualTo("alice");
        assertThat(ju.getJwtExpirationMs()).isGreaterThan(0);
    }

    @Test
    void getTokenFromRequest_prefersCookie_thenParameter() {
        JwtUtils ju = new JwtUtils(SECRET, 3600000);

        HttpServletRequest req = mock(HttpServletRequest.class);
        Cookie c = new Cookie("access_token", "cookie-token");
        when(req.getCookies()).thenReturn(new Cookie[]{c});
        assertThat(ju.getTokenFromRequest(req)).isEqualTo("cookie-token");

        // cookie absent -> reads parameter
        when(req.getCookies()).thenReturn(null);
        when(req.getParameter("access_token")).thenReturn("param-token");
        assertThat(ju.getTokenFromRequest(req)).isEqualTo("param-token");

        // both absent -> null
        when(req.getParameter("access_token")).thenReturn(null);
        assertThat(ju.getTokenFromRequest(req)).isNull();
    }

    @Test
    void buildAuthentication_usesRolesFromToken_and_fallsBackToUserAuthorities() {
        JwtUtils ju = new JwtUtils(SECRET, 3600000);

        String tokenWithRoles = ju.generateToken("bob", List.of("ROLE_A", "ROLE_B"));

        HttpServletRequest reqWithCookie = mock(HttpServletRequest.class);
        when(reqWithCookie.getCookies()).thenReturn(new Cookie[]{ new Cookie("access_token", tokenWithRoles) });

        UserDetails ud = User.withUsername("bob").password("x").authorities(new SimpleGrantedAuthority("ROLE_FALLBACK")).build();

        Authentication auth = ju.buildAuthentication(ud, reqWithCookie);
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("bob");
        assertThat(auth.getAuthorities()).extracting(a -> a.getAuthority()).contains("ROLE_A", "ROLE_B");

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String tokenWithRolesString = Jwts.builder()
                .setSubject("carol")
                .claim("roles", "ROLE_X,ROLE_Y")
                .signWith(key)
                .compact();

        HttpServletRequest req2 = mock(HttpServletRequest.class);
        when(req2.getCookies()).thenReturn(new Cookie[]{ new Cookie("access_token", tokenWithRolesString) });

        UserDetails ud2 = User.withUsername("carol").password("x").authorities(new SimpleGrantedAuthority("ROLE_FALLBACK2")).build();
        Authentication auth2 = ju.buildAuthentication(ud2, req2);
        assertThat(auth2).isNotNull();
        assertThat(auth2.getName()).isEqualTo("carol");
        assertThat(auth2.getAuthorities()).extracting(a -> a.getAuthority()).contains("ROLE_X", "ROLE_Y");

        HttpServletRequest reqNull = mock(HttpServletRequest.class);
        when(reqNull.getCookies()).thenReturn(null);
        UserDetails ud3 = User.withUsername("dan").password("x").authorities(new SimpleGrantedAuthority("ROLE_ONLY")).build();
        Authentication auth3 = ju.buildAuthentication(ud3, reqNull);
        assertThat(auth3).isNotNull();
        assertThat(auth3.getAuthorities()).extracting(a -> a.getAuthority()).contains("ROLE_ONLY");
    }
}


