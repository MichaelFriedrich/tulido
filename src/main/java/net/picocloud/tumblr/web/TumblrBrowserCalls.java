package net.picocloud.tumblr.web;

import net.picocloud.tumblr.MediaLoader;
import net.picocloud.tumblr.TumblrApiCalls;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TumblrBrowserCalls {

    private static final Logger logger = Logger.getLogger(TumblrBrowserCalls.class.getName());

    private static final String PAGES = "pages";
    private static final String POSTS_TXT = "posts.txt";
    private static final String PICS_TXT = "pics.txt";
    private static final String VIDS_TXT = "videos.txt";
    public static final String VIDS = "videos";
    public static final String PICS = "pics";

    private WebDriver driver;

    public static void main(String[] args) throws IOException {

        InputStream stream = TumblrApiCalls.class.getClassLoader().
                getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Config config = ConfigBuilder.getInstance("", "", "").build();
        if (!config.isValid()) {
            Config.printUsage();
            return;
        }

        TumblrBrowserCalls tbc = new TumblrBrowserCalls();
        tbc.setupAndLogin(config);
        tbc.loadLikes(config);
        tbc.tearDown();
    }


    public void tearDown() {
        driver.close();
        driver.quit();
    }

    public void setupAndLogin(Config config) {
        this.driver = config.driver;
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
        try {
            WebDriverWait wait = new WebDriverWait(driver, 5);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"qc-cmp2-ui\"]/div[2]/div/button[2]")));
        } catch (TimeoutException toe) {
            toe.printStackTrace();
        }
        driver.findElement(By.xpath("//*[@aria-label=\"Account\"]")).click();
        driver.findElement(By.xpath("//*[@href=\"/likes\"]")).click();
    }

    public void loadLikes(Config config) throws IOException {

        List<LikePage> pageList = getLikePages(config.blogname);

        Config.createTargetDir(config.destPath);
        logger.info("writing files ...");
        // open files
        try (
                FileWriter postFile = config.posts ? new FileWriter(config.destPath + POSTS_TXT) : null;
                FileWriter picFile = config.pics ? new FileWriter(config.destPath + PICS_TXT) : null;
                FileWriter vidFile = config.vids ? new FileWriter(config.destPath + VIDS_TXT) : null;
        ) {
            if (config.pages)
                Config.createTargetDir(config.destPath + File.separatorChar + PAGES);
            if (config.downloadMedia) {
                if (config.pics)
                    Config.createTargetDir(config.destPath + File.separatorChar + PICS);
                if (config.vids)
                    Config.createTargetDir(config.destPath + File.separatorChar + VIDS);
            }

            for (var lp : pageList) {
                if (config.pages)
                    try (var fw = new FileWriter(config.destPath + PAGES + File.separator + lp.bodyFileName)) {
                        fw.write(lp.body);
                    }
                if (config.pics)
                    saveUrls(PICS_TXT, lp.pics, config.destPath);
                if (config.vids)
                    saveUrls(VIDS_TXT, lp.vids, config.destPath);
                if (config.posts)
                    saveUrls(POSTS_TXT, lp.posts, config.destPath);


                if (config.downloadMedia) {
                    new Thread(() -> MediaLoader.getMedia(config.destPath + PICS, lp.pics)).start(); // create downloads in own thread
                    new Thread(() -> MediaLoader.getMedia(config.destPath + VIDS, lp.vids)).start(); // create downloads in own thread
                }
            }
        }
        logger.info("done writing files");
    }

    public List<LikePage> getLikePages(String blogname) throws IOException {
        // open page likes
        driver.get("https://www.tumblr.com/liked/by/" + blogname);

        logger.info("analyzing pages ...");
        List<LikePage> pageList = new ArrayList<>();
        try {
            do {
                pageList.add(analysePage());
            } while (isPagesAvailable());
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        logger.info("done analyzing pages");
        return pageList;
    }

    private void saveUrls(String filename, Set<String> urls, String destPath) throws IOException {
        try (var fw = new FileWriter(destPath + filename, true)) {
            urls.stream().forEach(x -> {
                try {
                    fw.write(x + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    private LikePage analysePage()  {
        LikePage lp = new LikePage();
        // Get posts
        var posts = driver.findElements(By.className("post_permalink"));
        lp.posts = getUrls(posts, "href");

        // Get Pictures
        var pics = driver.findElements(By.className("post_media_photo"));
        lp.pics = getUrls(pics, "src");

        // Get Videos
        var vids = driver.findElements(By.xpath("//video/source"));
        lp.vids = getUrls(vids, "src");

        lp.body = driver.getPageSource();

        var pathElements = driver.getCurrentUrl().split("/");
        var name = pathElements[pathElements.length - 3] + pathElements[pathElements.length - 2] + ".html";
        if ("likedby.html".equals(name))
            name = "page1.html";
        lp.bodyFileName = name;

        logger.info(() -> "Analyzed : " + lp.bodyFileName);

        return lp;
    }

    private boolean isPagesAvailable() {
        boolean pagesAvailable;
        if (!loadNextPage() || hasElement(driver, By.className("no_posts_found"))) { // retry
            for (int i = 0; i < 3 && hasElement(driver, By.className("no_posts_found")); i++) {
                logger.log(Level.INFO, "Waiting for 7 seconds ({0}/3)", i);
                sleep(7);
            }
            if (!loadNextPage())
                pagesAvailable = false;
            else
                pagesAvailable = true;
        } else
            pagesAvailable = true;
        return pagesAvailable;
    }


    private boolean loadNextPage() {
        if (!driver.findElements(By.id("next_page_link")).isEmpty()) { // next page exists
            driver.get(driver.findElement(By.id("next_page_link")).getAttribute("href"));

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
            if (file != null) {
                file.write(link);
                file.write('\n');
            }
            list[i++] = link;
        }
        return list;
    }

    protected Set<String> getUrls(List<WebElement> elements, String attr)  {
        return elements.stream().map(we -> we.getAttribute(attr)).collect(Collectors.toSet());
    }

    class LikePage {
        Set<String> posts;
        Set<String> pics;
        Set<String> vids;
        String bodyFileName;
        String body;
    }
}
