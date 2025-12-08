package co.edu.escuelaing.uplearn.chat.repository;

import co.edu.escuelaing.uplearn.chat.domain.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repositorio para gestionar entidades Message en MongoDB.
 */
public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByChatIdOrderByCreatedAtAsc(String chatId);

    List<Message> findByToUserIdAndDeliveredIsFalseOrderByCreatedAtAsc(String toUserId);
}
