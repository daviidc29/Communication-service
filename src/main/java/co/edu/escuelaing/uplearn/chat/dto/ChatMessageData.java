package co.edu.escuelaing.uplearn.chat.dto;

import lombok.*;

/**
 * Representa los datos de un mensaje de chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageData {
    private String id;
    private String chatId;
    private String fromUserId;
    private String toUserId;
    private String content;
    private String createdAt;
    private boolean delivered;
    private boolean read;
}
