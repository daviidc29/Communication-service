package co.edu.escuelaing.uplearn.chat.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Representa un mensaje en un chat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("messages")
public class Message {
    @Id
    private String id;
    @Indexed
    private String chatId;
    @Indexed
    private String fromUserId;
    @Indexed
    private String toUserId;
    private String content;
    private Instant createdAt;
    @Indexed
    private boolean delivered;
    @Indexed
    private boolean read;
}
