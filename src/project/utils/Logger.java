package project.utils;

public class Logger {

    public static void print(Tag tag, Object message) {
        if(tag.isLogsEnabled()) {
            System.out.print("[" + tag + "] ");
            System.out.println(message);
        }
    }
}
