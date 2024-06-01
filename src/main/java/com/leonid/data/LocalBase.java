package com.leonid.data;


import com.leonid.models.Player;
import com.leonid.models.TypeOfUpdate;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LocalBase {

//    public final static String adminUserName = "johnny_stl";
    public static String adminUserName;
    public static Long adminChatId = 0L;
    public static Long groupChatId = 0L;
    public static int currentRating = 75;
    public static Player currentPlayer;
    public static boolean wasGreeting = false;

    @Getter
    private final static List<Player> players = new ArrayList<>();

    @Getter
    private final static List<Player> activePlayers = new ArrayList<>();

    public static void hardReset() {
        groupChatId = 0L;
        currentRating = 75;
        currentPlayer = null;
        wasGreeting = false;
    }

    public static void addActivePlayer(Player player) {

        // if re-vote YES
        Optional<Player> active = activePlayers.stream()
                .filter(pl -> pl.getUserName().equals(player.getUserName()))
                .findFirst();
        if (active.isEmpty()) activePlayers.add(player);

        addPlayer(player);
    }


    public static void addPlayer(Player player) {
        Optional<Player> optionalPlayer = getUserByUserName(player.getUserName());
        if (optionalPlayer.isEmpty()) players.add(player);
    }


    public static Optional<Player> getByUserName(String userName) {
        return players.stream()
                .filter(player -> player.getUserName().equals(userName))
                .findFirst();
    }

    public static Player identifyUser(PollAnswer pollAnswer) {
        Optional<Player> optionalPlayer = getUserByUserName(pollAnswer.getUser().getUserName());
        if (optionalPlayer.isEmpty()) {
            Player player = new Player();
            player.setUserName(pollAnswer.getUser().getUserName());
            player.setName(pollAnswer.getUser().getFirstName());
            if (pollAnswer.getUser().getLastName() != null)
                player.setName(player.getName() + " " + pollAnswer.getUser().getLastName());
            player.setRating(-1);
            addPlayer(player);
            return player;
        }
        return optionalPlayer.get();
    }

    private static Optional<Player> getUserByUserName(String userName) {
        return players.stream().filter(player -> player.getUserName().equals(userName)).findFirst();
    }

    private static Optional<Player> getActiveUserByUserName(String userName) {
        return activePlayers.stream().filter(player -> player.getUserName().equals(userName)).findFirst();
    }

    public static void signRating(String userName, int rating) {
        Optional<Player> optionalPlayer = getUserByUserName(userName);
        optionalPlayer.ifPresent(player -> player.setRating(rating));

        Optional<Player> optionalActivePlayer = getActiveUserByUserName(userName);
        optionalActivePlayer.ifPresent(player -> player.setRating(rating));
    }

    public static String getInfo(TypeOfUpdate type) {
        return "\n1) AdminChatId " + adminChatId +
                "\n2) GroupChatId " + groupChatId +
                "\n3) Players " + players +
                "\n4) ActivePlayers " + activePlayers +
                "\n5) Was greating " + wasGreeting +
                "\n6) Current Player " + LocalBase.currentPlayer +
                "\n7) Type of update " + type +
                "\n\n";
    }

}
