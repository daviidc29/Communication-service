package co.edu.escuelaing.uplearn.chat.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
@CompoundIndex(name = "chat_created_idx", def = "{'chatId': 1, 'createdAt': 1}", background = true)
@CompoundIndex(name = "to_delivered_created_idx", def = "{'toUserId': 1, 'delivered': 1, 'createdAt': 1}", background = true)
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
