package project.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {
    private static BufferedWriter debugWriter = null;
    public static void FlushDebug()
    {
	    try {
            if(debugWriter != null)
            {
                System.out.println("Flushing DebugLog");
                debugWriter.flush();
            }
        }
        catch (IOException e)
        {
            System.err.println(e);
            e.printStackTrace();
        }
    }
    public static void print(Tag tag, Object message) {
        if(tag.isLogsEnabled()) {
            System.out.print("[" + tag + "] ");
            System.out.println(message);
        }
        if(debugWriter != null)
        {
            try {
                debugWriter.write("[" + tag + "]" + message + "\n");
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private BufferedWriter writer;

    public Logger(String filePath) {
	    String path = (new File(filePath)).getAbsolutePath();
        String debugPath = path.substring(0, path.lastIndexOf(File.separator)) + "/debug.log";

        try {
            File logFile = new File(path);
	        // File debugFile = new File(debugPath);
            if(!logFile.exists()) {
                boolean created = logFile.createNewFile();
            }
            // if(!debugFile.exists()){
            //     debugFile.createNewFile();
            // }
            this.writer = new BufferedWriter(new FileWriter(logFile));
	        // debugWriter = new BufferedWriter(new FileWriter(debugFile));
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to open the log file!");
	    System.err.println("Attempted path: " + path);
	    System.err.println(exception);
	    exception.printStackTrace();
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
            if (exception.getMessage().equals("Stream closed")) 
            {
                System.err.println("Tried to write to the log file after it was closed");
                System.err.println("Message that failed: " + "[" + timestamp + "] " + message + "\n");
            }
            else
            {
                System.err.println("An error occurred when trying to log a message into the log file!");
                System.err.println("Message that failed: " + "[" + timestamp + "] " + message + "\n");

            }
            exception.printStackTrace(System.err);
        }
    }

    public void close() {
        try {
            this.writer.close();
            // debugWriter.close();
            // debugWriter=null;
        } catch (IOException exception) {
            System.err.println("An error occurred when trying to close the log file!");
            exception.printStackTrace(System.out);
        }
    }
}
