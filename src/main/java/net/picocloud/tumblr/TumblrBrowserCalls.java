package net.picocloud.tumblr;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TumblrBrowserCalls {

    private static final Logger logger = Logger.getLogger(TumblrBrowserCalls.class.getName());

    private static final String PAGES = "pages";
    private static final String POSTS_TXT = "posts.txt";
    private static final String PICS_TXT = "pics.txt";
    private static final String VIDS_TXT = "videos.txt";
    public static final String VIDS = "videos";
    public static final String PICS = "pics";

    private final Config config;
    private final WebDriver driver;

    public TumblrBrowserCalls(Config config) {
        this.driver = config.driver;
        this.config = config;
    }

    public static void main(String[] args) throws IOException {
        Config config = new Config(args);
        if (!config.isValid()) {
            Config.printUsage();
            return;
        }

        TumblrBrowserCalls llt = new TumblrBrowserCalls(config);
        llt.setupAndLogin();
        llt.loadLikes();
        llt.tearDown();
    }


    public void tearDown() {
        driver.quit();
    }

    public void setupAndLogin() {
        driver.get("https://www.tumblr.com");
        var count = driver.findElements(By.xpath("//*[@href=\"/login\"]")).size();

        if (count > 0) { // need to login first
            driver.get("https://www.tumblr.com/login");
            driver.findElement(By.name("email")).click();
            driver.findElement(By.name("email")).sendKeys(config.username);
            driver.findElement(By.name("password")).sendKeys(config.password);
            driver.findElement(By.xpath("//*[@aria-label=\"Einloggen\"]")).click();
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

        Config.createTargetDir(config.destPath);

        if (config.posts)
            postFile = new FileWriter(config.destPath + POSTS_TXT);
        if (config.pics)
            picFile = new FileWriter(config.destPath + PICS_TXT);
        if (config.vids)
            vidFile = new FileWriter(config.destPath + VIDS_TXT);
        if (config.pages)
            Config.createTargetDir(config.destPath + File.separatorChar + PAGES);
        if (config.downloadMedia) {
            if (config.pics)
                Config.createTargetDir(config.destPath + File.separatorChar + PICS);
            if (config.vids)
                Config.createTargetDir(config.destPath + File.separatorChar + VIDS);
        }

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
                var urls = writeAttributes(picFile, pics, "src");
                if (config.downloadMedia)
                    new Thread(() -> MediaLoader.getMedia(config.destPath + PICS, urls)).start(); // create downloads in own thread
            }
            // Get Videos
            if (config.vids) {
                var vids = driver.findElements(By.xpath("//video/source"));
                var urls = writeAttributes(vidFile, vids, "src");
                if (config.downloadMedia)
                    new Thread(() -> MediaLoader.getMedia(config.destPath + VIDS, urls)).start(); // create downloads in own thread
            }
            if (config.pages) {
                var pathElements = driver.getCurrentUrl().split("/");
                var name = pathElements[pathElements.length - 3] + pathElements[pathElements.length - 2] + ".html";
                if ("likedby.html".equals(name))
                    name = "page1.html";
                try (var fw = new FileWriter(config.destPath + PAGES + File.separator + name)) {
                    fw.write(driver.getPageSource());
                }
            }
            if (!loadNextPage(time) || hasElement(driver, By.className("no_posts_found"))) { // retry
                for (int i = 0; i < 3 && hasElement(driver, By.className("no_posts_found")); i++) {
                    logger.log(Level.INFO, "Waiting for 7 seconds ({0}/3)", i);
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


    protected boolean loadNextPage(long time) {
        if (driver.findElements(By.id("next_page_link")).size() > 0) { // next page exists
            driver.get(driver.findElement(By.id("next_page_link")).getAttribute("href"));
            logger.info(() -> "Analyzing " + getDurationString(time));
            return true;
        }
        return false;
    }

    protected String getDurationString(long time) {
        return driver.getCurrentUrl() + " took " + Math.round((System.currentTimeMillis() - time) / 100.0) / 10.0 + " sec.";
    }

    protected void sleep(int seconds) {
        var t = System.currentTimeMillis();
        while (t + seconds * 1000L > System.currentTimeMillis()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected boolean hasElement(WebDriver driver, By by) {
        try {
            driver.findElement(by);
        } catch (NoSuchElementException noSuchElementException) {
            return false;
        }
        return true;
    }

    protected String[] writeAttributes(FileWriter file, List<WebElement> elements, String attr) throws IOException {
        var list = new String[elements.size()];
        int i = 0;
        for (WebElement element : elements) {
            var link = element.getAttribute(attr);
            file.write(link);
            file.write('\n');
            list[i++] = link;
        }
        return list;
    }
}
