package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.Notification;
import pro.sky.telegrambot.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {

    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TelegramBot telegramBot;


    @Scheduled(cron = "0 0/1 * * * *")
    public void sendUsersNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        /**
         * создаётся список notifications с теми данными из БД, которые ещё актуальны
         */
        List<Notification> notifications = notificationRepository.findByDateBeforeAndSentFalse(now);

        /**
         * C помощью цикла проходим по всем notifications
         * вызываем метод для отправки сообщения
         * если сообщение отправлено column sent становится true
         * сохраняем изменения
         */
        for (Notification notification : notifications) {
            sendMessageNotifications(notification);
            notification.setSent(true);
            notificationRepository.save(notification);
        }
    }

    private void sendMessageNotifications(Notification notification) {
        /**
         * Отправление уведомления из БД
         */
        SendMessage sendMessage = new SendMessage(
                notification.getChatId().toString(),
                notification.getMessage());

        try {
            telegramBot.execute(sendMessage);
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения в чат{}", notification.getChatId(), e);
        }
    }
}
