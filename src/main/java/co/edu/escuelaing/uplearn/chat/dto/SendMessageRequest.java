package co.edu.escuelaing.uplearn.chat.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representa la solicitud para enviar un mensaje.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {
    private String toUserId;
    private String content;
}
