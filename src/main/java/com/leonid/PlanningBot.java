package com.leonid;

import ch.qos.logback.classic.Logger;
import com.leonid.business.Distribution;
import com.leonid.data.LocalBase;
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
import java.util.Optional;

public class PlanningBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private static final Logger logger = (Logger) LoggerFactory.getLogger(PlanningBot.class);

    public PlanningBot(String botToken, String adminUser) {
        telegramClient = new OkHttpTelegramClient(botToken);
        LocalBase.adminUserName = adminUser;
    }

    @Override
    public void consume(Update update) {

        TypeOfUpdate type = getType(update);

        switch (type) {
            case ADMIN_PLAIN_TEXT -> {
                String text = update.getMessage().getText();
                if (LocalBase.adminChatId == 0) LocalBase.adminChatId = update.getMessage().getChatId();
                if (text.equals("/start") && LocalBase.groupChatId != 0) showManageMenu();
            }
            case POLL_ANSWER -> {
                PollAnswer pollAnswer = update.getPollAnswer();

                if (isYes(pollAnswer)) {
                    Player player = LocalBase.identifyUser(pollAnswer);
                    LocalBase.addActivePlayer(player);
//                    managePlayersWithoutRating();
                } else if (isNo(pollAnswer)) {
                    Player player = LocalBase.identifyUser(pollAnswer);
                    LocalBase.addPlayer(player);
//                    managePlayersWithoutRating();
                }
            }
            case ADMIN_CALLBACK -> {
                String text = update.getCallbackQuery().getData();
                Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
                if (text.startsWith("rating")) {
                    int rating = Integer.parseInt(text.substring(6, 8));
                    String userName = text.substring(8);
                    LocalBase.signRating(userName, rating);
                    LocalBase.currentRating = 75;
                    Optional<Player> curPl = LocalBase.getByUserName(userName);
                    curPl.ifPresent(player -> LocalBase.currentPlayer = player);

                    deleteMessage(messageId);
                }
                if (text.startsWith("edit_rating")) {
                    String userName = text.substring(11);
                    System.out.println("edit_rating " + userName);
                    Optional<Player> player = LocalBase.getByUserName(userName);
                    if (player.isPresent()) {
                        deleteMessage(messageId);
                        LocalBase.currentPlayer = player.get();
                        sendAssessment(LocalBase.currentPlayer);
                    }

                }
                switch (text) {
                    case "morePrev" -> {
                        LocalBase.currentRating -= 5;
                        editAssessment(messageId, LocalBase.currentPlayer);
                    }
                    case "prev" -> {
                        LocalBase.currentRating--;
                        editAssessment(messageId, LocalBase.currentPlayer);
                    }
                    case "next" -> {
                        LocalBase.currentRating++;
                        editAssessment(messageId, LocalBase.currentPlayer);
                    }
                    case "moreNext" -> {
                        LocalBase.currentRating += 5;
                        editAssessment(messageId, LocalBase.currentPlayer);
                    }

                    case "edit_player" -> {
                        showPlayersToEdit();
                    }

                    case "show_players" -> showPlayers();
                    case "leave" -> leave();
                    case "send_poll" -> sendPoll();
                    case "distribution" -> {

                        if (LocalBase.getActivePlayers().size()<4){
                            logger.error(LocalBase.getInfo(type) + " in distribution");
                            System.out.println(LocalBase.getInfo(type) + " in distribution");
                            return;
                        }

                        Distribution distribution =
                                new Distribution((ArrayList<Player>) LocalBase.getActivePlayers());
                        ArrayList<Team> teams = distribution.getTeams();
                        showDistributedTeams(teams);
                    }

                }
            }
            case BOT_ADDED_AS_ADMIN -> {

                if (!LocalBase.wasGreeting) {
                    LocalBase.groupChatId = update.getMyChatMember().getChat().getId();
                    initialGreeting();
                    LocalBase.wasGreeting = true;
                }
            }
            case SOMETHING_ELSE -> {
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

    private void showPlayersToEdit() {
        SendMessage sendMessage = SendMessage.builder()
                .text("Select player for edit: ")
                .chatId(LocalBase.adminChatId)
                .replyMarkup(UserInteraction.getPlayersEditMarkup())
                .build();

        executeSendMessage(sendMessage);
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
            System.err.println("Error with send Leave");
        }
    }

    private void showPlayers() {
        String text = UserInteraction.getPrettyText(LocalBase.getPlayers());
        if (text.isBlank()) text = "Nobody was added";
        SendMessage sendMessage = SendMessage.builder()
                .text(text)
                .chatId(LocalBase.adminChatId)
                .build();
        executeSendMessage(sendMessage);
    }

    private void showManageMenu() {
        SendMessage sendMessage = SendMessage.builder()
                .text("Manage menu")
                .chatId(LocalBase.adminChatId)
                .replyMarkup(UserInteraction.getManageMenuMarkup())
                .build();
        executeSendMessage(sendMessage);

    }

    private void initialGreeting() {
        String text = "Привіт, я буду створювати опитування щочетверга і генерувати " +
                "приблизно рівні склади в суботу вранці. Я поки не вмію взаємодіяти " +
                "зі звичайним користувачем, тому можете мені не писати";

        SendMessage sendMessage = SendMessage.builder()
                .text(text)
                .chatId(LocalBase.groupChatId)
                .build();
        executeSendMessage(sendMessage);
    }

    private void editAssessment(Integer messageId, Player player) {
        EditMessageText editMessageText = EditMessageText.builder()
                .chatId(LocalBase.adminChatId)
                .messageId(messageId)
                .text("Rate " + player.getName() + " (" + player.getUserName() + ") player")
                .replyMarkup(UserInteraction.getAssignRatingMarkup(player.getUserName(), LocalBase.currentRating))
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

    private void sendAssessment(Player player) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(LocalBase.adminChatId)
                .text("Rate " + player.getName() + " (" + player.getUserName() + ") player")
                .replyMarkup(UserInteraction.getAssignRatingMarkup(player.getUserName(), LocalBase.currentRating))
                .build();
        executeSendMessage(sendMessage);
    }

    private void sendPoll() {
        SendPoll poll = SendPoll.builder()
                .chatId(LocalBase.groupChatId)
                .isAnonymous(false)
                .question("Football on Saturday?")
                .option("Yes")
                .option("No")
                .build();
        executeSendPoll(poll);

        if (!LocalBase.getActivePlayers().isEmpty()) LocalBase.getActivePlayers().clear();
    }

    private boolean isYes(PollAnswer pollAnswer) {
        return pollAnswer.getOptionIds().getFirst().equals(0);
    }

    private boolean isNo(PollAnswer pollAnswer) {
        return pollAnswer.getOptionIds().getFirst().equals(1);
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
        if (update.hasMyChatMember()
                && update.getMyChatMember().getNewChatMember().getStatus().equals("member")
                && update.getMyChatMember().getOldChatMember().getStatus().equals("left")
        ) return TypeOfUpdate.BOT_ADDED_AS_MEMBER;

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

        if (update.hasMessage() && !update.hasMyChatMember() && update.getMessage().hasText() &&
                !update.getMessage().getText().isEmpty() &&
                update.getMessage().getFrom().getUserName().equals(LocalBase.adminUserName))
            return TypeOfUpdate.ADMIN_PLAIN_TEXT;

        if (update.hasCallbackQuery() &&
                update.getCallbackQuery().getFrom().getUserName().equals(LocalBase.adminUserName))
            return TypeOfUpdate.ADMIN_CALLBACK;

        if (update.hasPollAnswer())
            return TypeOfUpdate.POLL_ANSWER;

        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().equals("/start"))
            return TypeOfUpdate.START;

        return TypeOfUpdate.SOMETHING_ELSE;
    }

    private void exceptionProcess(TelegramApiException e){
        logger.error(e.getMessage());
        SendMessage sendMessage = SendMessage.builder()
                .text("O, nooo, we have exception")
                .chatId(LocalBase.adminChatId)
                .build();
        executeSendMessage(sendMessage);
        leave();
    }
}
