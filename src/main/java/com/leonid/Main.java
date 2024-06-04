package com.leonid;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {

    // FIRST OF ALL YOU NEED change -src/main/resources/logback.xml- file
    // to assign to it path to log file
    public static void main(String[] args) {

        String botToken = System.getenv("botToken");
        String adminUser = System.getenv("adminUser");
        String pathToData = System.getenv("pathToData");
        String pathToLogs = System.getenv("pathToLogs");

        System.out.println("FROM ENV " + botToken);
        System.out.println("FROM ENV " + adminUser);
        System.out.println("FROM ENV " + pathToData);
        System.out.println("FROM ENV " + pathToLogs);

        try {
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new PlanningBot(botToken, adminUser, pathToData, pathToLogs));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}