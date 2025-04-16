package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {


}
