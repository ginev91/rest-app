package org.example.main.config;

import org.example.main.security.JwtAuthenticationFilter;
import org.example.main.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.Filter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Test
    void authenticationManager_builds_using_shared_auth_builder() throws Exception {
        SecurityConfig cfg = new SecurityConfig();

        HttpSecurity http = mock(HttpSecurity.class);
        AuthenticationManagerBuilder amb = mock(AuthenticationManagerBuilder.class);
        AuthenticationManager am = mock(AuthenticationManager.class);

        when(http.getSharedObject(AuthenticationManagerBuilder.class)).thenReturn(amb);
        when(amb.authenticationProvider(any())).thenReturn(amb);
        when(amb.build()).thenReturn(am);

        UserDetailsService uds = mock(UserDetailsService.class);
        PasswordEncoder pe = mock(PasswordEncoder.class);

        AuthenticationManager out = cfg.authenticationManager(http, uds, pe);
        assertThat(out).isSameAs(am);

        verify(http).getSharedObject(AuthenticationManagerBuilder.class);
        verify(amb).authenticationProvider(any());
        verify(amb).build();
    }

    @Test
    void authenticationManager_throws_when_sharedObjectMissing() {
        SecurityConfig cfg = new SecurityConfig();

        HttpSecurity http = mock(HttpSecurity.class);
        when(http.getSharedObject(AuthenticationManagerBuilder.class)).thenReturn(null);

        UserDetailsService uds = mock(UserDetailsService.class);
        PasswordEncoder pe = mock(PasswordEncoder.class);

        assertThatThrownBy(() -> cfg.authenticationManager(http, uds, pe)).isInstanceOf(Exception.class);
        verify(http).getSharedObject(AuthenticationManagerBuilder.class);
    }

    @Test
    void authenticationManager_propagates_build_exception() throws Exception {
        SecurityConfig cfg = new SecurityConfig();

        HttpSecurity http = mock(HttpSecurity.class);
        AuthenticationManagerBuilder amb = mock(AuthenticationManagerBuilder.class);

        when(http.getSharedObject(AuthenticationManagerBuilder.class)).thenReturn(amb);
        when(amb.authenticationProvider(any())).thenReturn(amb);
        when(amb.build()).thenThrow(new IllegalStateException("build-error"));

        UserDetailsService uds = mock(UserDetailsService.class);
        PasswordEncoder pe = mock(PasswordEncoder.class);

        assertThatThrownBy(() -> cfg.authenticationManager(http, uds, pe))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("build-error");

        verify(http).getSharedObject(AuthenticationManagerBuilder.class);
        verify(amb).authenticationProvider(any());
        verify(amb).build();
    }

    @Test
    void securityFilterChain_configures_all_and_builds_chain() throws Exception {
        SecurityConfig cfg = new SecurityConfig();

        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);
        PasswordEncoder pe = mock(PasswordEncoder.class);

        HttpSecurity http = mock(HttpSecurity.class);

        
        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);

        
        when(http.addFilterBefore(any(Filter.class), eq(UsernamePasswordAuthenticationFilter.class))).thenReturn(http);

        
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain out = cfg.securityFilterChain(http, jwtUtils, uds, pe);
        assertThat(out).isSameAs(chain);

        verify(http).cors(any());
        verify(http).csrf(any());
        verify(http).sessionManagement(any());
        verify(http).authorizeHttpRequests(any());
        verify(http).addFilterBefore(any(Filter.class), eq(UsernamePasswordAuthenticationFilter.class));
        verify(http).build();
    }

    @Test
    void securityFilterChain_build_throws_propagates_exception() throws Exception {
        SecurityConfig cfg = new SecurityConfig();

        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);
        PasswordEncoder pe = mock(PasswordEncoder.class);

        HttpSecurity http = mock(HttpSecurity.class);
        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.addFilterBefore(any(Filter.class), eq(UsernamePasswordAuthenticationFilter.class))).thenReturn(http);

        when(http.build()).thenThrow(new IllegalStateException("build-fail"));

        assertThatThrownBy(() -> cfg.securityFilterChain(http, jwtUtils, uds, pe))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("build-fail");

        verify(http).build();
    }

    @Test
    void securityFilterChain_addsFilter_before_usernamePassword() throws Exception {
        SecurityConfig cfg = new SecurityConfig();

        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);
        PasswordEncoder pe = mock(PasswordEncoder.class);

        HttpSecurity http = mock(HttpSecurity.class);
        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);

        when(http.addFilterBefore(any(Filter.class), eq(UsernamePasswordAuthenticationFilter.class))).thenReturn(http);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain out = cfg.securityFilterChain(http, jwtUtils, uds, pe);
        assertThat(out).isSameAs(chain);

        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        verify(http).addFilterBefore(filterCaptor.capture(), eq(UsernamePasswordAuthenticationFilter.class));

        Filter passedFilter = filterCaptor.getValue();
        assertThat(passedFilter).isNotNull();
        
        assertThat(passedFilter.getClass()).isNotEqualTo(UsernamePasswordAuthenticationFilter.class);
    }

    @Test
    void securityFilterChain_registers_JwtAuthenticationFilter_instance() throws Exception {
        SecurityConfig cfg = new SecurityConfig();

        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);
        PasswordEncoder pe = mock(PasswordEncoder.class);

        HttpSecurity http = mock(HttpSecurity.class);
        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);

        
        ArgumentCaptor<Filter> captor = ArgumentCaptor.forClass(Filter.class);
        when(http.addFilterBefore(captor.capture(), eq(UsernamePasswordAuthenticationFilter.class))).thenReturn(http);

        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain out = cfg.securityFilterChain(http, jwtUtils, uds, pe);
        assertThat(out).isSameAs(chain);

        Filter registered = captor.getValue();
        assertThat(registered).isNotNull();
        assertThat(registered).isInstanceOf(JwtAuthenticationFilter.class);
    }

    @Test
    void passwordEncoder_isBCrypt_and_cors_configuration_values() {
        SecurityConfig cfg = new SecurityConfig();

        PasswordEncoder encoder = cfg.passwordEncoder();
        assertThat(encoder).isNotNull();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);

        CorsConfigurationSource source = cfg.corsConfigurationSource();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/");
        req.setContextPath("");
        req.setServletPath("/");
        req.setPathInfo(null);

        CorsConfiguration cfgObj = source.getCorsConfiguration(req);
        assertThat(cfgObj).isNotNull();
        assertThat(cfgObj.getAllowedOrigins()).containsExactly("http://localhost:3000");
        assertThat(cfgObj.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cfgObj.getAllowedHeaders()).contains("Authorization", "Content-Type", "Accept");
        assertThat(cfgObj.getAllowCredentials()).isTrue();
        assertThat(cfgObj.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsConfigurationSource_matches_registered_pattern_and_returns_config() {
        SecurityConfig cfg = new SecurityConfig();
        CorsConfigurationSource source = cfg.corsConfigurationSource();

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/anything/here");
        req.setContextPath("");
        req.setServletPath("/api/anything/here");
        req.setPathInfo(null);

        CorsConfiguration cfgObj = source.getCorsConfiguration(req);
        assertThat(cfgObj).isNotNull();
        assertThat(cfgObj.getAllowedOrigins()).contains("http://localhost:3000");
        assertThat(cfgObj.getAllowedMethods()).contains("GET", "POST");
    }
}