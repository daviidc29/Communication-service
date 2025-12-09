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

/**
 * Filtro de autenticación para proteger rutas específicas.
 * Verifica tokens JWT en las solicitudes entrantes
 * y permite o deniega el acceso según la validez del token.
 */
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final AuthorizationService authz;
    private final AntPathMatcher matcher = new AntPathMatcher();

    private static final String[] PUBLIC = { "/", "/error", "/favicon.ico", "/actuator/**" };
    private static final String[] PROTECTED = { "/api/**" };

    /**
     * Determina si la solicitud actual debe ser filtrada o no.
     * No filtra rutas públicas y filtra solo las rutas protegidas.
     * 
     * @param req la solicitud HTTP entrante
     * @return true si la solicitud no debe ser filtrada, false en caso contrario
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getRequestURI();
        for (String p : PUBLIC)
            if (matcher.match(p, path))
                return true;
        boolean isProtected = Arrays.stream(PROTECTED).anyMatch(p -> matcher.match(p, path));
        return !isProtected;
    }

    /**
     * Filtra la solicitud HTTP entrante para autenticar al usuario.
     * Verifica el token JWT en el encabezado Authorization.
     * Si el token es válido, permite que la solicitud continúe;
     * de lo contrario, responde con un error 401 Unauthorized.
     * 
     * @param req   la solicitud HTTP entrante
     * @param res   la respuesta HTTP saliente
     * @param chain la cadena de filtros
     * @throws ServletException si ocurre un error de servlet
     * @throws IOException      si ocurre un error de E/S
     */
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {


        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        try {
            RolesResponse me = authz.me(auth);
            if (me == null || me.getId() == null) {
                sendCorsAware(req, res, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }
            req.setAttribute("meId", me.getId());
            chain.doFilter(req, res);
        } catch (Exception e) {
            sendCorsAware(req, res, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }

    /**
     * Envía una respuesta de error CORS-aware.
     * 
     * @param req  la solicitud HTTP entrante
     * @param res  la respuesta HTTP saliente
     * @param code el código de estado HTTP
     * @param msg  el mensaje de error
     * @throws IOException si ocurre un error de E/S
     */
    private static void sendCorsAware(HttpServletRequest req, HttpServletResponse res, int code, String msg)
            throws IOException {
        String origin = req.getHeader("Origin");
        if (origin != null) {
            res.setHeader("Access-Control-Allow-Origin", origin); 
            res.setHeader("Vary", "Origin");
            res.setHeader("Access-Control-Allow-Credentials", "true");
            String reqHeaders = req.getHeader("Access-Control-Request-Headers");
            if (reqHeaders != null)
                res.setHeader("Access-Control-Allow-Headers", reqHeaders);
            res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        }
        res.setStatus(code);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"error\":\"" + msg + "\"}");
    }
}
