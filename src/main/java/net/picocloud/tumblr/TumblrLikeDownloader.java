package net.picocloud.tumblr;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TumblrLikeDownloader {

    private static final Logger logger = Logger.getLogger(TumblrLikeDownloader.class.getName());

    private static final String PAGES = "pages";
    private static final String POSTS_TXT = "posts.txt";
    private static final String PICS_TXT = "pics.txt";
    private static final String VIDS_TXT = "videos.txt";
    public static final String VIDS = "videos";
    public static final String PICS = "pics";

    private final Config config;
    private final WebDriver driver;

    public TumblrLikeDownloader(Config config) {
        this.driver = config.driver;
        this.config = config;
    }

    public static void main(String[] args) throws IOException {
        Config config = new Config(args);
        if (!config.isValid()) {
            Config.printUsage();
            return;
        }

        TumblrLikeDownloader llt = new TumblrLikeDownloader(config);
        llt.setupAndLogin();
        llt.loadLikes();
        llt.tearDown();
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
        MediaLoader mediaLoader = null;
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
            mediaLoader = new MediaLoader();
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
                if (config.downloadMedia) {
                    mediaLoader.getMedia(config.destPath + PICS, urls);
                }
            }
            // Get Videos
            if (config.vids) {
                var vids = driver.findElements(By.xpath("//video/source"));
                var urls = writeAttributes(vidFile, vids, "src");
                if (config.downloadMedia) {
                    mediaLoader.getMedia(config.destPath + VIDS, urls);
                }
            }
            if (config.pages) {
                var pathElements = driver.getCurrentUrl().split("/");
                var name = pathElements[pathElements.length - 3] + pathElements[pathElements.length - 2] + ".html";
                if ("likedby.html".equals(name))
                    name = "page1.html";
                try (var fw = new FileWriter(config.destPath  + PAGES + File.separator + name)) {
                    fw.write(driver.getPageSource());
                }
            }
            if (!loadNextPage(time) || hasElement(driver, By.className("no_posts_found"))) { // retry
                for (int i = 0; i < 3 && hasElement(driver, By.className("no_posts_found")); i++) {
                    logger.log(Level.INFO,"Waiting for 7 seconds ({0}/3)",i);
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
            logger.info(()->"Analyzing " + getDurationString(time));
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
                Thread.currentThread().interrupt();
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

    private String[] writeAttributes(FileWriter file, List<WebElement> elements, String attr) throws IOException {
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
