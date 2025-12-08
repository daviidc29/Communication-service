package co.edu.escuelaing.uplearn.chat.repository;

import co.edu.escuelaing.uplearn.chat.domain.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repositorio para gestionar entidades Chat en MongoDB.
 */
public interface ChatRepository extends MongoRepository<Chat, String> {
    Optional<Chat> findByUserAAndUserB(String userA, String userB);
}
