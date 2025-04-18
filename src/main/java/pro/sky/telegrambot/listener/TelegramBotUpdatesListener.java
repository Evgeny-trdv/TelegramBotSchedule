package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.Notification;
import pro.sky.telegrambot.repository.NotificationRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationRepository notificationRepository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
        try {
            setCommands();
        } catch (TelegramException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public int process(List<Update> updates) {

        updates.forEach(update -> {

            if (update.message() == null || update.message().text() == null) {
                return; // игнорируем NPE
            }

            logger.info("Processing update: {}", update);

            String messageText = update.message().text();
            Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)");
            Matcher matcher = pattern.matcher(messageText);
            long chatId = update.message().chat().id();
            String name = update.message().chat().firstName();

            // Process your updates here

            if (messageText.equals("/start")) {
                sendStartMenu(chatId, name);
            }

            if (messageText.equals("Начать работу")) {
                sendStartNotification(chatId);
            }

            if (matcher.find()) {
                String dateTime = matcher.group(1);
                String text = matcher.group(3);
                Notification notification = new Notification(
                        chatId,
                        text,
                        LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
                notificationRepository.save(notification);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

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

    private void setCommands() throws TelegramException {
        BotCommand command = new BotCommand("/start", "Запуск бота");

        telegramBot.execute(new SetMyCommands(command));
    }

    private void sendStartMenu(long chatId, String name) {
        /**
         * Приветствие + создание кнопки для дальнейшей работы
         */
        String text = "Добро пожаловать, " + name + " Нажмите кнопку, чтобы продолжить.";
        SendMessage sendMessage = new SendMessage(chatId, text);

        KeyboardButton startButton = new KeyboardButton("Начать работу");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(startButton);
        keyboardMarkup.resizeKeyboard(true);
        keyboardMarkup.oneTimeKeyboard(true);

        sendMessage.replyMarkup(keyboardMarkup);

        try {
            telegramBot.execute(sendMessage);
        } catch (Exception e) {
            throw new RuntimeException("mistake");
        }
    }

    private void sendStartNotification(long chatId) {
        /**
         * стартовый текст для описания функции бота
         */
        String not = "<b><i>01.01.2022 20:00 Сделать домашнюю работу</i></b>";
        String text = "Давайте попробуем создать напоминание, чтобы мы могли Вас о нём оповестить в нужное время."
                + " Введите пожалуйста напоминание в формате: \n"
                + not;
        SendMessage sendMessage = new SendMessage(chatId, text);
        sendMessage.parseMode(ParseMode.HTML);
        telegramBot.execute(sendMessage);
    }


}
