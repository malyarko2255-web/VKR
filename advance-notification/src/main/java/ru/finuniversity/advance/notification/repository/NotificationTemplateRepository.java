package ru.finuniversity.advance.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finuniversity.advance.notification.entity.NotificationTemplate;

import java.util.List;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    List<NotificationTemplate> findByCodeAndActiveTrue(String code);
}
