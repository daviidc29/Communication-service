package co.edu.escuelaing.uplearn.chat.dto;

import lombok.Data;
import java.util.List;

/**
 * Representa la respuesta con los roles de un usuario.
 */
@Data
public class RolesResponse {
    private String id;
    private String email;
    private String name;
    private List<String> roles;
    private boolean hasRoles;
    private String lastUpdated;
}
