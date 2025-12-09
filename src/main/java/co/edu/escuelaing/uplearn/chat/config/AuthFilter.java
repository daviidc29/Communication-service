package co.edu.escuelaing.uplearn.chat.config;

import co.edu.escuelaing.uplearn.chat.service.AuthorizationService;
import co.edu.escuelaing.uplearn.chat.dto.RolesResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

  private final AuthorizationService authz;
  private final AntPathMatcher matcher = new AntPathMatcher();

  private static final String[] PUBLIC = {"/", "/error", "/favicon.ico", "/actuator/**"};
  private static final String[] PROTECTED = {"/api/**"};

  @Override
  protected boolean shouldNotFilter(HttpServletRequest req) {
    String path = req.getRequestURI();
    // No filtrar si es público
    for (String p : PUBLIC) if (matcher.match(p, path)) return true;
    // Filtrar solo /api/**
    boolean isProtected = Arrays.stream(PROTECTED).anyMatch(p -> matcher.match(p, path));
    return !isProtected;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    // Preflight CORS
    if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
      res.setStatus(HttpServletResponse.SC_NO_CONTENT);
      return;
    }

    String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
    try {
      RolesResponse me = authz.me(auth); // valida token contra Users
      if (me == null || me.getId() == null) {
        send(res, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        return;
      }
      // puedes guardar el id en el request si luego lo quieres leer
      req.setAttribute("meId", me.getId());
      chain.doFilter(req, res);
    } catch (Exception e) {
      send(res, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
    }
  }

  private static void send(HttpServletResponse res, int code, String msg) throws IOException {
    res.setStatus(code);
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
    res.getWriter().write("{\"error\":\"" + msg + "\"}");
  }
}
