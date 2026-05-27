package com.campuseventhub.notification.repository;

import com.campuseventhub.notification.entity.Notification;
import com.campuseventhub.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientId(String recipientId);
    List<Notification> findByType(NotificationType type);
}
