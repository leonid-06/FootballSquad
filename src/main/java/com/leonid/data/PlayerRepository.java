package com.leonid.data;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.leonid.models.Player;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlayerRepository {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(PlayerRepository.class);

    public List<Player> getAll() {
        try {
            Path path = Paths.get(LocalBase.pathToData);
            String fileContent = Files.readString(path);
            if (Files.size(path) < 1) {
                logger.error("File must be have []");
                return null;
            }
            return LocalBase.mapper.readValue(fileContent, new TypeReference<>() {
            });
        } catch (IOException e) {
            return null;
        }
    }

    public List<Player> getActive() {
        List<Player> players = getAll();
        if (players == null) return List.of();
        return players.stream().filter(Player::isActiveOnWeek)
                .collect(Collectors.toList());
    }

    public Player identifyUser(PollAnswer pollAnswer) {
        String username = pollAnswer.getUser().getUserName();
        Integer option = pollAnswer.getOptionIds().getFirst();
        // if option = 0, then it is YES. if option = 1, then it is NO.

        Optional<Player> optionalPlayer = getByUserName(username);
        if (optionalPlayer.isEmpty()) {

            String firstname = pollAnswer.getUser().getFirstName();
            String lastname = pollAnswer.getUser().getLastName();
            Player player = new Player();
            player.setUserName(username);
            player.setName(firstname, lastname);
            player.setActiveOnWeek(option == 0);

            add(player);
            return player;
        }
        Player player = optionalPlayer.get();
        player.setActiveOnWeek(option == 0);
        update(player);
        return player;
    }

    public void update(Player player) {
        if (player == null) {
            logger.error("Attempt to update a NULL-player");
            return;
        }
        try {
            Path path = Paths.get(LocalBase.pathToData);
            String fileContent = Files.readString(path);

            // read file
            if (Files.size(path) < 1) {
                logger.error("File must be have []");
                return;
            }
            List<Player> playerList = LocalBase.mapper.readValue(fileContent, new TypeReference<>() {
            });

            //change playerList
            Optional<Player> optionalPlayer = playerList.stream()
                    .filter(p -> p.getUserName().equals(player.getUserName()))
                    .findFirst();
            if (optionalPlayer.isEmpty()) {
                logger.error("Such player to update not exists in DB");
                return;
            }
            Player p = optionalPlayer.get();
            p.copyFields(player);
            playerList.removeIf(item -> item.getUserName().equals(p.getUserName()));
            playerList.add(p);

            // write changed playerList to DB
            LocalBase.mapper.writeValue(path.toFile(), playerList);

        } catch (IOException e) {
            logger.error("Error while file reading");
        }

    }

    /**
     * Add player into a dummy database (json file)
     * @param player - object to added
     */
    public void add(Player player) {
        if (player == null) {
            logger.error("Attempt to add a NULL-player to BD");
            return;
        }
        try {
            Path path = Paths.get(LocalBase.pathToData);
            String fileContent = Files.readString(path);

            // read file
            if (Files.size(path) < 1) {
                logger.error("File must be have []");
                return;
            }
            List<Player> playerList = LocalBase.mapper.readValue(fileContent, new TypeReference<>() {
            });

            //change playerList
            Optional<Player> optionalPlayer = playerList.stream()
                    .filter(p -> p.getUserName().equals(player.getUserName()))
                    .findFirst();
            if (optionalPlayer.isPresent()) {
                logger.error("Such player to add already exists in DB");
                return;
            }
            playerList.add(player);

            // write changed playerList to DB
            LocalBase.mapper.writeValue(path.toFile(), playerList);

        } catch (IOException e) {
            logger.error("Error while file reading");
        }
    }

    public Optional<Player> getByUserName(String username) {
        List<Player> players = getAll();
        if (players == null) return Optional.empty();
        return players.stream().filter(player -> player.getUserName().equals(username)).findFirst();
    }

    public void resetWeek() {
        List<Player> players = getAll();
        if (players==null || players.isEmpty()) return;
        players.forEach(player -> player.setActiveOnWeek(false));
        Path path = Paths.get(LocalBase.pathToData);
        try {
            LocalBase.mapper.writeValue(path.toFile(), players);
        } catch (IOException e) {
            logger.error("error with reset");
        }
    }
}
