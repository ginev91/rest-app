package org.example.main.config;

import org.example.main.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.Filter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    PasswordEncoder passwordEncoder;

    @Test
    void corsConfigurationSource_returnsExpectedCorsConfiguration() {
        SecurityConfig cfg = new SecurityConfig();

        var src = cfg.corsConfigurationSource();
        assertThat(src).isInstanceOf(UrlBasedCorsConfigurationSource.class);

        UrlBasedCorsConfigurationSource uSrc = (UrlBasedCorsConfigurationSource) src;
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/any/path");
        CorsConfiguration conf = uSrc.getCorsConfiguration(req);
        assertThat(conf).isNotNull();

        assertThat(conf.getAllowedOrigins()).containsExactly("http://localhost:8082");
        assertThat(conf.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(conf.getAllowedHeaders()).containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept");
        assertThat(conf.getAllowCredentials()).isTrue();
        assertThat(conf.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void securityFilterChain_buildsUsingProvidedHttp_and_addsJwtFilter() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);

        when(http.cors(ArgumentMatchers.any())).thenReturn(http);
        when(http.csrf(ArgumentMatchers.any())).thenReturn(http);
        when(http.sessionManagement(ArgumentMatchers.any())).thenReturn(http);
        when(http.authorizeHttpRequests(ArgumentMatchers.any())).thenReturn(http);
        when(http.addFilterBefore(ArgumentMatchers.any(Filter.class), ArgumentMatchers.eq(UsernamePasswordAuthenticationFilter.class))).thenReturn(http);

        SecurityFilterChain chain = mock(SecurityFilterChain.class);
        doReturn(chain).when(http).build();


        SecurityConfig cfg = new SecurityConfig();

        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);

        SecurityFilterChain result = cfg.securityFilterChain(http, jwtUtils, uds, passwordEncoder);

        assertThat(result).isSameAs(chain);

        verify(http).addFilterBefore(ArgumentMatchers.any(Filter.class), ArgumentMatchers.eq(UsernamePasswordAuthenticationFilter.class));
        verify(http).cors(ArgumentMatchers.any());
        verify(http).csrf(ArgumentMatchers.any());
        verify(http).sessionManagement(ArgumentMatchers.any());
        verify(http).authorizeHttpRequests(ArgumentMatchers.any());
        verify(http).build();
    }

    @Test
    void authenticationManager_delegatesToAuthenticationManagerBuilder() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        AuthenticationManagerBuilder authBuilder = mock(AuthenticationManagerBuilder.class);

        when(http.getSharedObject(AuthenticationManagerBuilder.class)).thenReturn(authBuilder);
        when(authBuilder.authenticationProvider(ArgumentMatchers.any())).thenReturn(authBuilder);

        AuthenticationManager mgr = mock(AuthenticationManager.class);
        when(authBuilder.build()).thenReturn(mgr);

        SecurityConfig cfg = new SecurityConfig();

        UserDetailsService uds = mock(UserDetailsService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        AuthenticationManager result = cfg.authenticationManager(http, uds, encoder);

        assertThat(result).isSameAs(mgr);

        verify(http).getSharedObject(AuthenticationManagerBuilder.class);
        verify(authBuilder).authenticationProvider(ArgumentMatchers.any());
        verify(authBuilder).build();
    }
}