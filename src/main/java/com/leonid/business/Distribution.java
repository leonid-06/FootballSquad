package com.leonid.business;

import com.leonid.models.Player;
import com.leonid.models.Team;

import java.util.*;

public class Distribution {

    private final ArrayList<Player> activePlayersList;

    public Distribution(ArrayList<Player> players) {
        activePlayersList = players;
    }

    public ArrayList<Team> getTeams() {

        int activePlayer = activePlayersList.size();
        int countOfTeams = getCountTeams(activePlayer);

        ArrayList<Player> captains = getCaptains(countOfTeams);
        ArrayList<Team> teams = new ArrayList<>();
        for (int i = 0; i < countOfTeams; i++) {
            Player captain = captains.get(i);
            Team team = new Team(captain);
            team.addToMain(captain);
            teams.add(team);
        }

        while (isContainFreePlayers()) {
            // Circle of choice

            int[] generalRatings = getGeneralRatings(teams);
            int[] priorities = getPrioritiesForTeam(generalRatings);

            for (int priority : priorities) {
                Optional<Player> optionalPlayer = getMaxFreeRatingPlayer();
                if (optionalPlayer.isPresent()) {
                    Team team = teams.get(priority);
                    optionalPlayer.get().setBelongTeam(true);
                    team.addToMain(optionalPlayer.get());
                }
            }
        }

        // change teams
        formReserveBench(teams);

        return teams;

    }

    private void formReserveBench(ArrayList<Team> teams) {
        for (Team team : teams) {
            team.setCountOfPlayers(team.getMainPlayers().size());
            if (team.getMainPlayers().size() == 5) continue;
            if (team.getMainPlayers().size() > 5) {
                team.getSubstitutePlayers().addAll(team.getMainPlayers().subList(5, team.getMainPlayers().size()));
                while (team.getMainPlayers().size() > 5) {
                    team.getMainPlayers().remove(team.getMainPlayers().size() - 1);
                }
            }
        }
    }

    private int[] getGeneralRatings(ArrayList<Team> teams) {
        int[] retVal = new int[teams.size()];
        for (int i = 0; i < retVal.length; i++) {
            retVal[i] = teams.get(i).getGeneralRating();
        }
        return retVal;
    }

    // Teams
    public int[] getPrioritiesForTeam(int[] generalRatings) {
        int[] retVal = new int[generalRatings.length];
//        Arrays.fill(retVal, -1);
//        int randomValue;
//        for (int i = 0; i < retVal.length; i++) {
//            randomValue = (int) Math.floor(Math.random() * countOfTeams);
//            if (!containSuchNumber(retVal, randomValue)) {
//                retVal[i] = randomValue;
//            } else i--;
//        }

        LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();

        for (int i = 0; i < generalRatings.length; i++) {
            map.put(i, generalRatings[i]);
        }

        List<Map.Entry<Integer, Integer>> list = new LinkedList<>(map.entrySet());

        list.sort(Map.Entry.comparingByValue());

        for (int i = 0; i < generalRatings.length; i++) {
            retVal[i] = list.get(i).getKey();
        }

        return retVal;
    }

    private boolean containSuchNumber(int[] array, int number) {
        for (int j : array) if (j == number) return true;
        return false;
    }

    private Optional<Player> getMaxFreeRatingPlayer() {
        return activePlayersList.stream()
                .filter(player -> !player.isBelongTeam())
                .max(Comparator.comparingInt(Player::getRating));
    }


    public int getCountTeams(int activePlayer) {
        return (activePlayer % 5 < 4) ? activePlayer / 5 : activePlayer / 5 + 1;
    }

    public ArrayList<Player> getCaptains(int countCaptains) {
        // around which rating will the captain’s rating be chosen
        int randomRating = (int) Math.round(Math.random() * 35) + 60;
        // how accurate the player selection will be
        int delta = 2;

        ArrayList<Player> captains = new ArrayList<>();

        int attemptToSelect = 0;

        for (int i = 0; i < countCaptains; i++) {
            int index = (int) Math.floor(Math.random() * activePlayersList.size());
            Player potentialCap = activePlayersList.get(index);


            if (isSimilarRating(potentialCap, randomRating, delta) && !potentialCap.isMatchCapitan()) {
                potentialCap.setBelongTeam(true);
                potentialCap.setMatchCapitan(true);
                captains.add(potentialCap);
            } else {
                attemptToSelect++;
                i--;
                int shift = attemptToSelect / activePlayersList.size();
                delta += shift;
            }
        }

        return captains;

    }

    private boolean isSimilarRating(Player player, int neededRat, int delta) {
        int playerRat = player.getRating();

        // player rat must lie between (neededRat-delta) and (neededRat+delta)
        return playerRat > (neededRat - delta) && playerRat < (neededRat + delta);
    }

    private boolean isContainFreePlayers() {
        return activePlayersList.stream().anyMatch(player -> !player.isBelongTeam());
    }
}
