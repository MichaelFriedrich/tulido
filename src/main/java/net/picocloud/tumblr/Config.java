package net.picocloud.tumblr;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;


/**
 * Command line configuration parsing and checking
 */
public class Config {


    private static final Logger logger = Logger.getLogger(Config.class.getName());
    /**
     * destination path for downloaded files
     */
    String destPath = null;
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

    boolean downloadMedia = true;


    public static void printUsage() {
        System.out.println("Usage:\n" +
                "java -jar tulido.jar <username> <password> <blogname> [<options> ...]\n" +
                "Options:\n" +
                "-pages     : do not create all like pages\n" +
                "-vids      : do not create vids.txt\n" +
                "-posts     : do not create posts.txt\n" +
                "-pics      : do not create pics.txt\n" +
                "-firefox   : use Firefox (excludes: -chrome)\n" +
                "-chrome    : use Chrome (default, excludes: -firefox)" +
                "-d | -dest : target directory to store files (default: current directory)" +
                "Downloads all likes from the tumblr blog <blogname> with the given <username> and <password>.\n" +
                "If no options are given, the following files are created:\n" +
                "- directory pages containing all pages with the likes as html files,\n" +
                "- pics.txt containing all the urls of the pictures liked,\n" +
                "- posts.txt containing all the urls of the posts liked,\n" +
                "- vids.txt containing all the urls of the videos liked.\n" +
                "\nYou can download the pictures or other files with \"cat xxx.txt | xargs wget\"");
    }

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
                    case "-dest":
                    case "-d":
                        if (i+1 >= args.length)
                            valid = false;
                        else
                           destPath = args[i+1];
                        i++;
                        break;
                    case "-nd":
                    case "-no-download":
                        downloadMedia = false;
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

            if (destPath == null)
                destPath = Path.of(".").toFile().getAbsolutePath();

            destPath = trimPath(destPath) + File.separator;

        } catch (IllegalStateException ise) {
            logger.severe("Please install the correct webdriver, either geckodriver for Firefox or chromedriver for chrome. See the README file.");
            valid = false;
        }
    }


    private static String trimPath(String path) {
        while (path.endsWith("."))
            path = path.substring(0,path.length()-1);
        if (path.endsWith(File.separator))
            path = path.substring(0,path.length()-1);
        return path;
    }

    public static void createTargetDir(String targetDir) throws IOException {
        File target = new File(targetDir);
        if (!target.exists() && !target.mkdir())
            throw new IOException("Could not create directory: " + targetDir);

        if (target.isHidden() || !Files.isWritable(Path.of(targetDir)))
            throw new IOException("Directory is not writable: " + targetDir);
    }

    public boolean isValid() {
        return valid;
    }
}

