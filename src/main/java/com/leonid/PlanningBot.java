package com.leonid;

import ch.qos.logback.classic.Logger;
import com.leonid.business.Distribution;
import com.leonid.data.LocalBase;
import com.leonid.data.PlayerRepository;
import com.leonid.models.Player;
import com.leonid.models.Team;
import com.leonid.models.TypeOfUpdate;
import com.leonid.views.UserInteraction;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlanningBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private static final Logger logger = (Logger) LoggerFactory.getLogger(PlanningBot.class);

    public PlanningBot(String botToken, String adminUser, String pathToData, String pathToLogs) {
        telegramClient = new OkHttpTelegramClient(botToken);
        LocalBase.adminUserName = adminUser;
        LocalBase.pathToData = pathToData;
        LocalBase.pathToLogs = pathToLogs;
    }

    @Override
    public void consume(Update update) {

        TypeOfUpdate type = getType(update);
        PlayerRepository repository = new PlayerRepository();

        switch (type) {
            case ADMIN_PLAIN_TEXT -> {
                String text = update.getMessage().getText();
                if (LocalBase.adminChatId == 0) LocalBase.adminChatId = update.getMessage().getChatId();
                if (text.equals("/start")) showManageMenu(LocalBase.adminChatId);
            }
            case POLL_ANSWER -> {
                PollAnswer pollAnswer = update.getPollAnswer();
                repository.identifyUser(pollAnswer);
            }
            case ADMIN_CALLBACK -> {
                String text = update.getCallbackQuery().getData();
                Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

                // when we type "edit" -> "and chose who" in admin panel
                // we assign current player to LocalBase
                if (text.startsWith("edit_rating")) {
                    LocalBase.currentUsername = text.substring(11);
                    deleteMessage(messageId);
                    sendAssessment(LocalBase.currentUsername);
                }

                // when we clicked on 84 or 72
                // we update current player in Local Base and update json file
                if (text.startsWith("rating")) {
                    int rating = Integer.parseInt(text.substring(6, 8));
                    String userName = text.substring(8);

                    Optional<Player> optionalPlayer = repository.getByUserName(userName);
                    if (optionalPlayer.isPresent()) {
                        Player player = optionalPlayer.get();
                        player.setRating(rating);
                        repository.update(player);
                    }

                    LocalBase.currentRating = 75;
                    deleteMessage(messageId);
                }

                switch (text) {
                    case "morePrev" -> {
                        LocalBase.currentRating -= 5;
                        editAssessment(messageId);
                    }
                    case "prev" -> {
                        LocalBase.currentRating--;
                        editAssessment(messageId);
                    }
                    case "next" -> {
                        LocalBase.currentRating++;
                        editAssessment(messageId);
                    }
                    case "moreNext" -> {
                        LocalBase.currentRating += 5;
                        editAssessment(messageId);
                    }
                    case "edit_player" -> showPlayersToEdit(repository.getAll());

                    case "show_players" -> showPlayers(repository.getAll());
                    case "leave" -> leave();
                    case "send_poll" -> sendPoll(repository);
                    case "distribution" -> {

                        List<Player> activePlayers = repository.getActive();

                        if (activePlayers.size() < Distribution.MIN_COUNT_OF_PLAYERS) {
                            logger.error(LocalBase.getInfo(type) + " in distribution");
                            return;
                        }

                        Distribution distribution =
                                new Distribution((ArrayList<Player>) activePlayers);
                        ArrayList<Team> teams = distribution.getTeams();
                        showDistributedTeams(teams);
                    }

                }
            }
            case BOT_ADDED_AS_ADMIN -> {
                if (LocalBase.groupChatId != 0) return;
                LocalBase.groupChatId = update.getMyChatMember().getChat().getId();
                initialGreeting(LocalBase.groupChatId);
            }
            case LEFT_FROM_GROUP, DEMOTED_TO_MEMBER -> LocalBase.hardReset();
            default -> {
                return;
            }
        }

        logger.info(LocalBase.getInfo(type));
        System.out.println(LocalBase.getInfo(type));
    }

    private void showDistributedTeams(ArrayList<Team> teams) {

        String text = UserInteraction.getPrettyTextForTeams(teams);
        if (text.isEmpty()) text = "few players";

        SendMessage sendMessage = SendMessage.builder()
                .text(text)
                .chatId(LocalBase.groupChatId)
                .build();

        executeSendMessage(sendMessage);
    }

    private void showPlayersToEdit(List<Player> activePlayers) {

        if (activePlayers == null || activePlayers.isEmpty()){
            String message = "There are any players ";
            SendMessage sendMessage = SendMessage.builder()
                    .text(message)
                    .chatId(LocalBase.adminChatId)
                    .build();
            executeSendMessage(sendMessage);
        } else {
            String message = "Select player for edit: ";
            SendMessage sendMessage = SendMessage.builder()
                    .text(message)
                    .chatId(LocalBase.adminChatId)
                    .replyMarkup(UserInteraction.getPlayersEditMarkup(activePlayers))
                    .build();
            executeSendMessage(sendMessage);
        }


    }

    private void leave() {
        LeaveChat leaveChat = LeaveChat.builder()
                .chatId(LocalBase.groupChatId)
                .build();
        LocalBase.hardReset();
        executeLeaveChat(leaveChat);
    }

    private void executeLeaveChat(LeaveChat leaveChat) {
        try {
            telegramClient.execute(leaveChat);
        } catch (TelegramApiException e) {
            SendMessage sendMessage = SendMessage.builder()
                    .text("Error with chat leave. (exception)")
                    .chatId(LocalBase.adminChatId)
                    .build();
            executeSendMessage(sendMessage);
        }
    }

    private void showPlayers(List<Player> active) {
        String text = UserInteraction.getPrettyText(active);
        if (text.isBlank()) text = "Nobody was added";
        SendMessage sendMessage = SendMessage.builder()
                .text(text)
                .chatId(LocalBase.adminChatId)
                .build();
        executeSendMessage(sendMessage);
    }

    private void showManageMenu(Long chatId) {
        SendMessage sendMessage = SendMessage.builder()
                .text("Manage menu")
                .chatId(chatId)
                .replyMarkup(UserInteraction.getManageMenuMarkup())
                .build();
        executeSendMessage(sendMessage);

    }

    private void initialGreeting(Long chatId) {
        String text = "Привіт, я буду створювати опитування щочетверга і генерувати " +
                "приблизно рівні склади в суботу вранці. Я поки не вмію взаємодіяти " +
                "зі звичайним користувачем, тому можете мені не писати";

        SendMessage sendMessage = SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .build();
        executeSendMessage(sendMessage);
    }

    private void editAssessment(Integer messageId) {

        EditMessageText editMessageText = EditMessageText.builder()
                .chatId(LocalBase.adminChatId)
                .messageId(messageId)
                .text("Rate @" + LocalBase.currentUsername + " player")
                .replyMarkup(UserInteraction.getAssignRatingMarkup(LocalBase.currentUsername, LocalBase.currentRating))
                .build();
        executeEditMessage(editMessageText);
    }

    private void executeEditMessage(EditMessageText editMessageText) {
        try {
            telegramClient.execute(editMessageText);
        } catch (TelegramApiException e) {
            exceptionProcess(e);
        }
    }

    private void deleteMessage(Integer messageId) {
        DeleteMessage deleteMessage = DeleteMessage.builder()
                .chatId(LocalBase.adminChatId)
                .messageId(messageId)
                .build();

        executeDeleteMessage(deleteMessage);
    }

    private void sendAssessment(String username) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(LocalBase.adminChatId)
                .text("Rate @" + username + " player")
                .replyMarkup(UserInteraction.getAssignRatingMarkup(username, LocalBase.currentRating))
                .build();
        executeSendMessage(sendMessage);
    }

    private void sendPoll(PlayerRepository repository) {
        SendPoll poll = SendPoll.builder()
                .chatId(LocalBase.groupChatId)
                .isAnonymous(false)
                .question("Football on Saturday?")
                .option("Yes")
                .option("No")
                .build();
        executeSendPoll(poll);

        repository.resetWeek();

    }

    private void executeSendPoll(SendPoll sendPoll) {
        try {
            telegramClient.execute(sendPoll);
        } catch (TelegramApiException e) {
            exceptionProcess(e);
        }
    }

    private void executeSendMessage(SendMessage sendMessage) {
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            exceptionProcess(e);
        }
    }

    private void executeDeleteMessage(DeleteMessage deleteMessage) {
        try {
            telegramClient.execute(deleteMessage);
        } catch (TelegramApiException e) {
            exceptionProcess(e);
        }
    }

    private TypeOfUpdate getType(Update update) {
//        if (update.hasMyChatMember()
//                && update.getMyChatMember().getNewChatMember().getStatus().equals("member")
//                && update.getMyChatMember().getOldChatMember().getStatus().equals("left")
//        ) return TypeOfUpdate.BOT_ADDED_AS_MEMBER;

        if (update.hasMyChatMember()
                && update.getMyChatMember().getNewChatMember().getStatus().equals("administrator")
                && (
                update.getMyChatMember().getOldChatMember().getStatus().equals("member") ||
                        update.getMyChatMember().getOldChatMember().getStatus().equals("left") ||
                        update.getMyChatMember().getOldChatMember().getStatus().equals("kicked")
        )
        ) return TypeOfUpdate.BOT_ADDED_AS_ADMIN;

        if (update.hasMyChatMember()
                && update.getMyChatMember().getNewChatMember().getStatus().equals("left")
                && (
                update.getMyChatMember().getOldChatMember().getStatus().equals("member") ||
                        update.getMyChatMember().getOldChatMember().getStatus().equals("administrator") ||
                        update.getMyChatMember().getOldChatMember().getStatus().equals("kicked"))
        ) return TypeOfUpdate.LEFT_FROM_GROUP;


        if (update.hasMyChatMember()
                && update.getMyChatMember().getNewChatMember().getStatus().equals("member")
                && (
                update.getMyChatMember().getOldChatMember().getStatus().equals("administrator") ||
                        update.getMyChatMember().getOldChatMember().getStatus().equals("kicked"))
        ) return TypeOfUpdate.DEMOTED_TO_MEMBER;

        if (update.hasMyChatMember()
                && update.getMyChatMember().getNewChatMember().getStatus().equals("kicked")
        ) return TypeOfUpdate.DEMOTED_TO_MEMBER;

        if (update.hasMessage()
                && !update.hasMyChatMember() && !update.hasCallbackQuery()
                && update.getMessage().hasText() && !update.getMessage().getText().isEmpty()
                && update.getMessage().getFrom().getUserName().equals(LocalBase.adminUserName))
            return TypeOfUpdate.ADMIN_PLAIN_TEXT;

        if (update.hasCallbackQuery()
                && !update.hasMyChatMember() && !update.hasPollAnswer() &&
                update.getCallbackQuery().getFrom().getUserName().equals(LocalBase.adminUserName))
            return TypeOfUpdate.ADMIN_CALLBACK;

        if (update.hasPollAnswer())
            return TypeOfUpdate.POLL_ANSWER;

        return TypeOfUpdate.SOMETHING_ELSE;
    }

    private void exceptionProcess(TelegramApiException e) {
        logger.error(e.getMessage());
        SendMessage sendMessage = SendMessage.builder()
                .text("O, nooo, we have exception")
                .chatId(LocalBase.adminChatId)
                .build();
        executeSendMessage(sendMessage);
        leave();
    }
}
