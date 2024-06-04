package com.leonid.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leonid.models.TypeOfUpdate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalBase {

//    public final static String adminUserName = "johnny_stl";
    public static String adminUserName;
    public static Long adminChatId = 0L;
    public static Long groupChatId = 0L;
    public static int currentRating = 75;
    public final static ObjectMapper mapper = new ObjectMapper();
    public static String currentUsername;
    public static String pathToLogs;
    public static String pathToData;

    public static void hardReset() {
        Path path = Paths.get(pathToLogs);
        try(BufferedWriter writer = Files.newBufferedWriter(path)){
            writer.write("");
        } catch (IOException ignored) {}

        groupChatId = 0L;
        currentRating = 75;
    }


    public static String getInfo(TypeOfUpdate type) {
        return "\n1) AdminChatId " + adminChatId +
                "\n2) GroupChatId " + groupChatId +
                "\n3) Type of update " + type +
                "\n<br/><br/>";
    }
}
