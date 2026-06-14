package com.example.eventsphere.repository;
import com.example.eventsphere.entity.NotificationLog;
import com.example.eventsphere.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByDeliveredFalse();
    List<NotificationLog> findByType(NotificationType type);
    List<NotificationLog> findByUser_Id(Long userId);
    List<NotificationLog> findTop100ByOrderByCreatedAtDesc();
    List<NotificationLog> findByDelivered(Boolean delivered);
}
