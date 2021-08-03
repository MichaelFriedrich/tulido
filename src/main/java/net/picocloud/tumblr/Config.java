package net.picocloud.tumblr;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;


/**
 * Command line configuration parsing and checking
 */
public class Config {
    /**
     * tumblr.com user name
     */
    String username;

    /**
     * tumblr.com password
     */
    String password;

    /**
     * tumblr.com blog to get the likes from
     */
    String blogname;

    WebDriver driver = null;

    /**
     * is the configuration valid?
     */
    boolean valid = true;

    /**
     * should the pages with the liked posts saved as html files?
     */
    boolean pages = true;

    /**
     * shall the video.txt file be generated?
     */
    boolean vids = true;

    /**
     * shall the posts.txt file be generated?
     */
    boolean posts = true;

    /**
     * shall the pics.txt file be generated?
     */
    boolean pics = true;

    /**
     * Creates the configuration
     *
     * @param args the first three arguements are: user, password, blogname in this order. The following arguments are optional and there is no specific order.
     *             Every arguemtn can only be passed once (though it is not always enforced).
     */
    public Config(String... args) {
        if (args.length < 3 || !args[0].contains("@")) {
            valid = false;
            return;
        }
        this.username = args[0];
        this.password = args[1];
        this.blogname = args[2];
        try {
            for (int i = 3; i < args.length && valid; i++) {
                switch (args[i]) {
                    case "-pages":
                        pages = false;
                        break;
                    case "-pics":
                        pics = false;
                        break;
                    case "-vids":
                        vids = false;
                        break;
                    case "-posts":
                        posts = false;
                        break;
                    case "-firefox":
                        if (driver != null) {
                            valid = false;
                            break;
                        }
                        driver = new FirefoxDriver();
                        break;
                    case "-chrome":
                        if (driver != null) {
                            valid = false;
                            break;
                        }
                        driver = new ChromeDriver();
                        break;
                    default:
                        valid = false;
                }
            }
            if (driver == null) // set default driver
                driver = new ChromeDriver();
            if (!pages && !vids && !pics && !posts) { // save nothing
                valid = false;
            }
        } catch (IllegalStateException ise) {
            System.err.println("Please install the correct webdriver, either geckodriver for Firefox or chromedriver for chrome. See the README file."
            );
            valid = false;
        }
    }

    public boolean isValid() {
        return valid;
    }
}

