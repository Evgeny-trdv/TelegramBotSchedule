package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.BanChatMember;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.request.UnbanChatMember;
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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationRepository notificationRepository;

    private static final List<String> TARGET_CHANNELS = Arrays.asList(
            "-1002516647653",
            "-1002606419459"// Пример числового ID канала
    );

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

            String messageText = update.message().text();
            Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)");
            Matcher matcher = pattern.matcher(messageText);

            logger.info("Processing update: {}", update);

            long chatId = update.message().chat().id();
            String nameUser = update.message().chat().firstName();
            User user = update.message().from();
            Long userId = user.id();


            if (messageText.equals("/start")) {
                sendStartMenu(chatId, nameUser);
            }

            if (messageText.equals("Начать работу")) {
                sendStartNotification(chatId);
            }

            /*if (messageText.equals("/schedule")) {
                String text = "Сообщение получено, ждите уведомление";
                SendMessage sendMessage = new SendMessage(chatId, text);
                telegramBot.execute(sendMessage);
                sendNotificationBeforeClose();
            }*/

            if (messageText.equals("/leave")) {
                removeUserFromChannels(userId, TARGET_CHANNELS);
            }

            // Process your updates here

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

    private void setCommands() throws TelegramException {
        BotCommand command = new BotCommand("/start", "Запуск бота");
        BotCommand commandTwo = new BotCommand("/schedule", "Отчёт времени перед уведомлением");

        telegramBot.execute(new SetMyCommands(command, commandTwo));
    }

    private void sendStartMenu(long chatId, String name) {
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
        String not = "<b><i>01.01.2022 20:00 Сделать домашнюю работу</i></b>";
        String text = "Давайте попробуем создать напоминание, чтобы мы могли Вас о нём оповестить в нужное время."
                + " Введите пожалуйста напоминание в формате: \n"
                + not;
        SendMessage sendMessage = new SendMessage(chatId, text);
        sendMessage.parseMode(ParseMode.HTML);
        telegramBot.execute(sendMessage);
    }

    @Scheduled(fixedRate = 60000)
    public void sendNotificationBeforeClose() {
        //String text = "У вас кончается подписка";
        LocalDateTime now = LocalDateTime.now();
        List<Notification> notifications = notificationRepository.findByDateBeforeAndSentFalse(now);
        //SendMessage sendMessage = new SendMessage(notifications., notifications.g);

        for (Notification message : notifications) {
            sendMessage(message);
            message.setSent(true);
            notificationRepository.save(message);
        }
        //telegramBot.execute(sendMessage);
    }

    private void sendMessage(Notification message) {
        SendMessage sendMessage = new SendMessage(message.getChatId().toString(), message.getMessage());

        telegramBot.execute(sendMessage);
    }

    private void removeUserFromChannels(Long userId, List<String> channelIds) {
        boolean allSuccess = true;

        for (String chatId : channelIds) {
            try {
                // Сначала баним (это удалит из канала)
                BanChatMember ban = new BanChatMember(chatId, userId);
                telegramBot.execute(ban);

                // Затем разбаниваем (если нужно, чтобы мог присоединиться снова)
                UnbanChatMember unban = new UnbanChatMember(chatId, userId);
                telegramBot.execute(unban);

            } catch (IllegalAccessError e) {
                allSuccess = false;
                // Логируем ошибку для конкретного канала
                System.err.println("Error removing user from channel " + chatId + ": " + e.getMessage());
            }
        }
    }
}
