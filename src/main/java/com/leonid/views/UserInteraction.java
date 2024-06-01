package com.leonid.views;

import com.leonid.data.LocalBase;
import com.leonid.models.Player;
import com.leonid.models.Team;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class UserInteraction {
    public static InlineKeyboardMarkup getAssignRatingMarkup(String userName, int rating) {
        var morePrev = InlineKeyboardButton.builder()
                .text("<<")
                .callbackData("morePrev")
                .build();
        var prev = InlineKeyboardButton.builder()
                .text("<")
                .callbackData("prev")
                .build();
        var number = InlineKeyboardButton.builder()
                .text(String.valueOf(rating))
                .callbackData("rating"+rating+userName)
                .build();
        var next = InlineKeyboardButton.builder()
                .text(">")
                .callbackData("next")
                .build();
        var moreNext = InlineKeyboardButton.builder()
                .text(">>")
                .callbackData("moreNext")
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(List.of(morePrev, prev, number, next, moreNext)))
                .build();
    }

    public static ReplyKeyboard getManageMenuMarkup() {
        var edit = InlineKeyboardButton.builder()
                .text("edit")
                .callbackData("edit_player")
                .build();
        var show = InlineKeyboardButton.builder()
                .text("show")
                .callbackData("show_players")
                .build();
        var sendPoll = InlineKeyboardButton.builder()
                .text("send poll")
                .callbackData("send_poll")
                .build();
        var distribution = InlineKeyboardButton.builder()
                .text("make distribution")
                .callbackData("distribution")
                .build();
        var leave = InlineKeyboardButton.builder()
                .text("leave")
                .callbackData("leave")
                .build();
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(List.of(edit, show, sendPoll, distribution, leave)))
                .build();
    }


    public static String getPrettyText(List<Player> players) {
        if (players.isEmpty()) return "";
        StringBuilder retVal = new StringBuilder();
        for (Player player: players){
            retVal.append(player);
            retVal.append("\n");
        }
        return retVal.toString();
    }

    public static ReplyKeyboard getPlayersEditMarkup() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < LocalBase.getPlayers().size(); i++) {
            Player player = LocalBase.getPlayers().get(i);
            String name = player.getName();
            String userName = player.getUserName();
            var playerButton = new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text(name)
                            .callbackData("edit_rating"+userName)
                            .build());
            rows.add(playerButton);
        }
        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public static String getPrettyTextForTeams(ArrayList<Team> teams) {
        StringBuilder retVal = new StringBuilder();
        for(Team team: teams){
            retVal.append(team);
            retVal.append("\n");
        }
        return retVal.toString();
    }
}
