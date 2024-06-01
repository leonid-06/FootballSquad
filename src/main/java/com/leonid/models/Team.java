package com.leonid.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class Team {

    public Team(Player capitan) {
        this.capitan = capitan;
        this.mainPlayers = new ArrayList<>();
        this.substitutePlayers = new ArrayList<>();
    }

    @Getter
    private ArrayList<Player> mainPlayers;
    @Getter
    private ArrayList<Player> substitutePlayers;
    @Getter
    private int generalRating = 0;
    @Setter
    private int countOfPlayers = 1;
    @Getter
    private Player capitan;

    public void addToMain(Player player) {
        generalRating += player.getRating();
        mainPlayers.add(player);
    }

    @Override
    public String toString() {
        return "Team: " +
                "\nCount of players: " + countOfPlayers +
                "\nCapitan: " + capitan +
                "\nMain players: " + mainPlayers +
                "\nSubstitute players: " + substitutePlayers +
                "\nGENERAL RATING: " + generalRating;
    }
}
