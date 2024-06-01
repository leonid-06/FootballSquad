package com.leonid;

import com.leonid.business.Distribution;
import com.leonid.models.Player;
import com.leonid.models.Team;

import java.util.ArrayList;
import java.util.Random;

public class Test {
    public static void main(String[] args) {
        ArrayList<Player> players = new ArrayList<>();
        String[] names = {
                "Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Hank",
                "Ivy"
        };

        Random rand = new Random();

        for (String name : names) {
            Player player = new Player();
            player.setUserName(name);
            player.setRating(rand.nextInt(30) + 50);
            players.add(player);
        }

        Distribution distribution = new Distribution(players);

        ArrayList<Team> teams = distribution.getTeams();
        System.out.println("-------------");
        for (Team team : teams) {
            System.out.println(team);
            System.out.println("-------------");
        }

    }
}
