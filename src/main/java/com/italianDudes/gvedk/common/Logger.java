package com.italianDudes.gvedk.common;

import com.italianDudes.gvedk.GVEDK;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"UnusedReturnValue","unused"})
public final class Logger {

    //Attributes
    private static final int MAX_QUEUE_SIZE = 5;
    private static final File latestLogFilePointer = new File(GVEDK.Defs.LOG_LATEST_FILE);
    private static BufferedWriter logger = null;
    private static ExecutorService queue = null;
    private final static LocalDateTime startTime = LocalDateTime.now();

    //Classes
    private static class LogWriter implements Runnable {

        //Attributes
        private final String message;
        private final boolean isError;

        //Constructors
        public LogWriter(String message, boolean isError){
            this.message = message;
            this.isError = isError;
        }

        //Methods
        @Override
        public void run() {
            if(isError){
                System.err.println("[ERROR]["+LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"] "+message);
                System.err.flush();
            }else {
                System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] " + message);
                System.out.flush();
            }
            try {
                writeMessageIntoLogFile(message,isError);
            }catch (IOException e){
                System.err.println("[ERROR]["+LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"] Can't write log message into log file!");
            }
        }
    }

    //Constructors
    private Logger(){
        throw new UnsupportedOperationException("Can't instantiate this class!");
    }

    //Methods
    public static boolean initLogger() throws IOException {
        return initLogger(MAX_QUEUE_SIZE);
    }
    public static boolean initLogger(int size) throws IOException {
        File logDirectory = new File(GVEDK.Defs.LOG_DIR);
        if(!logDirectory.exists() || !logDirectory.isDirectory()) {
            if (!logDirectory.mkdir()) {
                System.err.println("[ERROR]["+LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"] Can't create log directory!");
                return false;
            }
        }
        queue = Executors.newFixedThreadPool(size);
        return createLogFile();
    }
    public static void log(String message, boolean isError){
        queue.submit(new LogWriter(message, isError));
    }
    private static boolean saveLog() throws IOException{
        String date = startTime.format(DateTimeFormatter.BASIC_ISO_DATE)+"_"+startTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
        date = date.replaceAll(":","-");
        date = date.split("\\.")[0];
        File newLogDestination = new File(GVEDK.Defs.LOG_DIR+date+".log");
        FileUtils.copyFile(latestLogFilePointer,newLogDestination);
        return true;
    }
    public static boolean closeLogger() throws IOException {

        boolean result;
        queue.shutdown();
        try {
            result = queue.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }catch (InterruptedException e){
            System.err.println("[ERROR]["+LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"] An error has occurred during queue termination");
            result = false;
        }
        if(!result) {
            System.err.println("[ERROR]["+LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"] An error has occurred during queue termination");
        }
        try{
            logger.close();
        }catch (IOException e){
            System.err.println("[ERROR]["+LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"] Can't close log file!");
            throw e;
        }
        return saveLog();
    }
    private static boolean createLogFile() throws IOException {
        try {
            logger = new BufferedWriter(new FileWriter(latestLogFilePointer));
        }catch (IOException e){
            System.err.println("[ERROR]["+LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"] Can't initialize logger!");
            throw e;
        }
        return true;
    }
    private synchronized static void writeMessageIntoLogFile(String message, boolean isError) throws IOException {
        try {
            if(isError)
                logger.append("[ERROR]");
            logger.append("[").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("] ").append(message).append("\n");
        }catch (IOException e){
            System.err.println("[ERROR]["+LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"] Can't write message into log file!");
            throw e;
        }
        try {
            logger.flush();
        }catch (IOException e){
            System.err.println("[ERROR]["+LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)+"] Can't flush log file!");
            throw e;
        }
    }
}
