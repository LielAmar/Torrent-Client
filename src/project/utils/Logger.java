package project.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {

    public static void print(Tag tag, Object message) {
        if(tag.isLogsEnabled()) {
            System.out.print("[" + tag + "] ");
            System.out.println(message);
        }
    }

    private BufferedWriter writer;

    public Logger(String filePath) {
        try {
            File logFile = new File((new File(filePath)).getAbsolutePath());

            if(!logFile.exists()) {
                boolean created = logFile.createNewFile();
            }

            this.writer = new BufferedWriter(new FileWriter(logFile));
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to open the log file!");
        }
    }

    public void log(String message) {
        if(this.writer == null) {
            System.err.println("Tried to write a log message into the log file before the file has been set up!");
            return;
        }

        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

        try {
            this.writer.write("[" + timestamp + "] " + message + "\n");
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to log a message into the log file!");
        }
    }

    public void close() {
        try {
            this.writer.close();
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to close the log file!");
        }
    }
}
