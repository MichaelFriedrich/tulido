package net.picocloud.tumblr;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggerTest {


    private static final Logger logger = Logger.getLogger(MediaLoader.class.getName());

    public static void main(String[] args) {
        InputStream stream = TumblrApiCalls.class.getClassLoader().
                getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.log(Level.INFO, "Test", new RuntimeException());
    }
}
