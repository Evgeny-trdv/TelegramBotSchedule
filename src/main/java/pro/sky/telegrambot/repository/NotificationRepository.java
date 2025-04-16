package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pro.sky.telegrambot.model.Notification;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    //List<Notification> findBySendTimeBeforeAndSentFalse(LocalDateTime now);

    List<Notification> findByDateBeforeAndSentFalse(LocalDateTime now);
}
