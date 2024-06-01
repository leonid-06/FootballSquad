package com.leonid;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {

    // FIRST OF ALL YOU NEED change -src/main/resources/logback.xml- file
    // to assign to it path to log file
    public static void main(String[] args) {

        String botToken = System.getenv("BOT_TOKEN");
        String adminUser = System.getenv("ADMIN_USER");

        try {
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new PlanningBot(botToken, adminUser));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}