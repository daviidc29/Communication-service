package co.edu.escuelaing.uplearn.chat.config;

import co.edu.escuelaing.uplearn.chat.dto.RolesResponse;
import co.edu.escuelaing.uplearn.chat.service.AuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthFilterTest {

    @Mock
    private AuthorizationService authz;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthFilter authFilter;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testShouldNotFilter_PublicPath() {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        boolean result = authFilter.shouldNotFilter(request);
        assertTrue(result);
    }

    @Test
    void testShouldNotFilter_IgnoredPath() {
        when(request.getRequestURI()).thenReturn("/assets/logo.png");
        boolean result = authFilter.shouldNotFilter(request);
        assertTrue(result);
    }

    @Test
    void testShouldNotFilter_ProtectedPath() {
        when(request.getRequestURI()).thenReturn("/api/chat/messages");
        boolean result = authFilter.shouldNotFilter(request);
        assertFalse(result);
    }

    @Test
    void testDoFilterInternal_OptionsMethod() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("OPTIONS");

        authFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authz); 
    }

    @Test
    void testDoFilterInternal_Success() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        String token = "Bearer valid_token";
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(token);

        RolesResponse mockUser = new RolesResponse();
        mockUser.setId("123");
        when(authz.me(token)).thenReturn(mockUser);

        authFilter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute("meId", "123");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_Unauthorized_WithCors() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalid");
        when(authz.me(anyString())).thenReturn(null);
        
        when(request.getHeader("Origin")).thenReturn("http://localhost:3000");
        when(request.getHeader("Access-Control-Request-Headers")).thenReturn("Content-Type, Authorization");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        authFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        verify(response).setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        verify(response).setHeader("Access-Control-Allow-Credentials", "true");
        
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_Unauthorized_NullId() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token_no_id");
        
        RolesResponse userNoId = new RolesResponse();
        when(authz.me(anyString())).thenReturn(userNoId);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        authFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_Exception() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer error_token");
        when(authz.me(anyString())).thenThrow(new RuntimeException("Error interno auth"));

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        authFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }
}