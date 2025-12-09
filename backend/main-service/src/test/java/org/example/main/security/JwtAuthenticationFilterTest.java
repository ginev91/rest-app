package org.example.main.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @AfterEach
    void cleanup() {
        
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void skipsInternalPath_and_delegatesToChain() throws Exception {
        JwtUtils ju = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);
        JwtAuthenticationFilter f = new JwtAuthenticationFilter(ju, uds);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/api/internal/health");

        f.doFilterInternal(req, resp, chain);

        verify(chain).doFilter(req, resp);
        verifyNoInteractions(ju);
    }

    @Test
    void noToken_proceedsChain_and_noAuthSet() throws Exception {
        JwtUtils ju = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);
        JwtAuthenticationFilter f = new JwtAuthenticationFilter(ju, uds);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/api/public/hello");
        when(ju.getTokenFromRequest(req)).thenReturn(null);

        f.doFilterInternal(req, resp, chain);

        verify(chain).doFilter(req, resp);
        Authentication a = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        assertThat(a).isNull();
    }

    @Test
    void invalidToken_resultsInUnauthorizedResponse_and_noChain() throws Exception {
        JwtUtils ju = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);
        JwtAuthenticationFilter f = new JwtAuthenticationFilter(ju, uds);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/api/protected/resource");
        when(ju.getTokenFromRequest(req)).thenReturn("bad-token");
        when(ju.validateToken("bad-token")).thenReturn(false);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(resp.getWriter()).thenReturn(pw);

        f.doFilterInternal(req, resp, chain);

        verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(resp).setHeader("X-Auth-Error", "invalid_or_expired_token");
        verify(chain, never()).doFilter(req, resp);
        String body = sw.toString();
        assertThat(body).contains("\"error\":\"unauthenticated\"");
    }

    @Test
    void exceptionDuringProcessing_triggersUnauthorized_and_invalidatesSession() throws Exception {
        JwtUtils ju = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);
        JwtAuthenticationFilter f = new JwtAuthenticationFilter(ju, uds);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/api/protected/resource");
        when(ju.getTokenFromRequest(req)).thenThrow(new RuntimeException("boom"));

        when(req.getSession(false)).thenReturn(session);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(resp.getWriter()).thenReturn(pw);

        f.doFilterInternal(req, resp, chain);

        verify(session).invalidate();
        verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(resp).setHeader("X-Auth-Error", "invalid_or_expired_token");
        verify(chain, never()).doFilter(req, resp);
        assertThat(sw.toString()).contains("unauthenticated");
    }

    @Test
    void successfulAuthentication_setsSecurityContext_and_callsChain() throws Exception {
        JwtUtils ju = mock(JwtUtils.class);
        UserDetailsService uds = mock(UserDetailsService.class);
        JwtAuthenticationFilter f = new JwtAuthenticationFilter(ju, uds);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/api/protected/resource");
        when(ju.getTokenFromRequest(req)).thenReturn("good-token");
        when(ju.validateToken("good-token")).thenReturn(true);
        when(ju.getUsernameFromToken("good-token")).thenReturn("user1");

        UserDetails ud = mock(UserDetails.class);
        when(uds.loadUserByUsername("user1")).thenReturn(ud);

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user1", null);
        when(ju.buildAuthentication(ud, req)).thenReturn(auth);

        f.doFilterInternal(req, resp, chain);

        verify(chain).doFilter(req, resp);
        assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(auth);
    }
}
