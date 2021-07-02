package net.picocloud.tumblr;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TumblrLikeDownloader {
    private final Config config;
    private final WebDriver driver ;

    public TumblrLikeDownloader(Config config) {
        this.driver = config.driver;
        this.config = config;
    }



    public static void main(String[] args) throws IOException {
        Config config = new Config(args);
        if (!config.isValid()) {
            printUsage();
            return;
        }

        TumblrLikeDownloader llt = new TumblrLikeDownloader(config);
        llt.setupAndLogin();
        llt.loadLikes();
        llt.tearDown();
    }

    static void printUsage() {
        System.out.println("Usage:\n" +
                "java -jar tulido.jar <username> <password> <blogname> [<options> ...]\n" +
                "Options:\n" +
                "-pages   : do not create all like pages\n" +
                "-vids    : do not create vids.txt\n" +
                "-posts   : do not create posts.txt\n" +
                "-pics    : do not create pics.txt\n" +
                "-firefox : use Firefox (excludes: -chrome)\n" +
                "-chrome  : use Chrome (default, excludes: -firefox)" +
                "Downloads all likes from the tumblr blog <blogname> with the given <username> and <password>.\n" +
                "If no options are given, the following files are created:\n" +
                "- directory pages containing all pages with the likes as html files,\n" +
                "- pics.txt containing all the urls of the pictures liked,\n" +
                "- posts.txt containing all the urls of the posts liked,\n" +
                "- vids.txt containing all the urls of the videos liked.\n" +
                "\nYou can download the pictures or other files with \"cat xxx.txt | xargs wget\"");
    }

    public void tearDown() {
        driver.quit();
    }

    public void setupAndLogin() {
        driver.get("https://www.tumblr.com");
        {
            WebDriverWait wait = new WebDriverWait(driver, 5);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".\\_3rL67")));
        }
        var count = driver.findElements(By.xpath("//*[@href=\"/login\"]")).size();

        if (count > 0) { // need to login first
            driver.get("https://www.tumblr.com/login");
            driver.findElement(By.name("email")).click();
            driver.findElement(By.name("email")).sendKeys(config.username);
            driver.findElement(By.cssSelector(".\\_242_x > .\\_3dTPo")).click();
            {
                WebElement element = driver.findElement(By.cssSelector(".\\_242_x > .\\_3dTPo"));
                Actions builder = new Actions(driver);
                builder.moveToElement(element).perform();
            }
            driver.findElement(By.name("password")).sendKeys(config.password);
            driver.findElement(By.cssSelector(".\\_1F1cG:nth-child(3) > .\\_3dTPo")).click();
        }

        // Click Agree to cookies
        {
            WebDriverWait wait = new WebDriverWait(driver, 5);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"qc-cmp2-ui\"]/div[2]/div/button[2]")));
        }
        driver.findElement(By.xpath("//*[@id=\"qc-cmp2-ui\"]/div[2]/div/button[2]")).click();
    }

    public void loadLikes() throws IOException {
        // open files

        FileWriter postFile = null;
        FileWriter picFile = null;
        FileWriter vidFile = null;

        if (config.posts)
            postFile = new FileWriter("posts.txt");
        if (config.pics)
            picFile = new FileWriter("pics.txt");
        if (config.vids)
            vidFile = new FileWriter("vids.txt");


        // open page likes
        driver.get("https://www.tumblr.com/liked/by/" + config.blogname);

        boolean pagesAvailable = true;
        while (pagesAvailable) {

            long time = System.currentTimeMillis();

            // Get posts
            if (config.posts) {
                var posts = driver.findElements(By.className("post_permalink"));
                writeAttributes(postFile, posts, "href");
            }
            // Get Pictures
            if (config.pics) {
                var pics = driver.findElements(By.className("post_media_photo"));
                writeAttributes(picFile, pics, "src");
            }
            // Get Videos
            if (config.vids) {
                var vids = driver.findElements(By.xpath("//video/source"));
                writeAttributes(vidFile, vids, "src");
            }
            if (config.pages) {
                var pathElements = driver.getCurrentUrl().split("/");
                var name = pathElements[pathElements.length - 3] + pathElements[pathElements.length - 2] + ".html";
                var fw = new FileWriter(name);
                fw.write(driver.getPageSource());
                fw.close();
            }
            if (!loadNextPage(time) || hasElement(driver, By.className("no_posts_found"))) { // retry
                for (int i = 0; i < 3 && hasElement(driver, By.className("no_posts_found")); i++) {
                    System.out.println("Waiting for 7 seconds (" + i + "/3)");
                    sleep(7);
                }
                if (!loadNextPage(time))
                    pagesAvailable = false;
                else
                    pagesAvailable = true;
            } else
                pagesAvailable = true;
        }
        if (postFile != null)
            postFile.close();
        if (picFile != null)
             picFile.close();
        if (vidFile != null)
            vidFile.close();

        driver.close();
    }


    private boolean loadNextPage(long time) {
        if (driver.findElements(By.id("next_page_link")).size() > 0) { // next page exists
            driver.get(driver.findElement(By.id("next_page_link")).getAttribute("href"));
            System.out.println("Analyzing " + getDurationString(time));
            return true;
        }
        return false;
    }

    private String getDurationString(long time) {
        return driver.getCurrentUrl() + " took " + Math.round((System.currentTimeMillis() - time) / 100.0) / 10.0 + " sec.";
    }

    private void sleep(int seconds) {
        var t = System.currentTimeMillis();
        while (t + seconds * 1000L > System.currentTimeMillis()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private boolean hasElement(WebDriver driver, By by) {
        try {
            driver.findElement(by);
        } catch (NoSuchElementException noSuchElementException) {
            return false;
        }
        return true;
    }

    private void writeAttributes(FileWriter file, List<WebElement> elements, String attr) throws IOException {
        for (WebElement element : elements) {
            var link = element.getAttribute(attr);
            file.write(link);
            file.write('\n');
        }
    }
}
