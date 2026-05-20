package ru.finuniversity.advance.notification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.finuniversity.advance.notification.entity.Notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdAndChannelAndIsReadFalseOrderByCreatedAtDesc(
            UUID recipientId, String channel, Pageable pageable);

    Page<Notification> findByRecipientIdAndChannelOrderByCreatedAtDesc(
            UUID recipientId, String channel, Pageable pageable);

    long countByRecipientIdAndChannelAndIsReadFalse(UUID recipientId, String channel);

    List<Notification> findByRecipientIdAndChannelAndCreatedAtAfterAndIsReadFalse(
            UUID recipientId, String channel, Instant after);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.recipientId = :recipientId AND n.channel = 'IN_APP' AND n.isRead = false")
    int markAllReadForUser(@Param("recipientId") UUID recipientId, @Param("now") Instant now);
}
