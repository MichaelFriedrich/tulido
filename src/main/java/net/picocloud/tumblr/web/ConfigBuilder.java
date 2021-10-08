package net.picocloud.tumblr.web;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.File;
import java.util.logging.Logger;

public class ConfigBuilder {
    private Config config = null;
    private static final Logger logger = Logger.getLogger(ConfigBuilder.class.getName());

    public static ConfigBuilder getInstance(String username, String password, String blogname) {
        Config config = new Config(username, password, blogname);
        return new ConfigBuilder(config);
    }

    private ConfigBuilder(Config config) {
        this.config = config;
    }

    public Config build() {
        if (!config.pages && !config.vids && !config.pics && !config.posts) { // save nothing
            config.valid = false;
            logger.warning("nothing to extract");
        }
        if (config.driver == null) {
            config.valid = false;
            logger.warning("no web driver for selenium chosen");
        }
        if (config.username == null || config.password == null || config.blogname == null ||
                config.username.isBlank() || config.password.isBlank() || config.blogname.isBlank()) {
            config.valid = false;
            logger.warning("Some of the fields username, password or blogname are empty");
        }
        return config;
    }

    public ConfigBuilder noDownload() {
        config.downloadMedia = false;
        return this;
    }

    public ConfigBuilder noPics() {
        config.pics = false;
        return this;
    }

    public ConfigBuilder noVids() {
        config.vids = false;
        return this;
    }

    public ConfigBuilder noPages() {
        config.pages = false;
        return this;
    }

    public ConfigBuilder noPosts() {
        config.posts = false;
        return this;
    }

    public ConfigBuilder firefox() {
        return firefox(false);
    }

    public ConfigBuilder firefox(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();

        if (headless)
            options.addArguments("headless");

        config.driver = new FirefoxDriver();
        return this;
    }


    public ConfigBuilder chrome() {
        return chrome(false);
    }

    public ConfigBuilder chrome(boolean headless) {
        ChromeOptions options = new ChromeOptions();

        if (headless)
            options.addArguments("headless");

        config.driver = new ChromeDriver(options);
        return this;
    }


    public ConfigBuilder targetDir(String dir) {
        config.destPath = Config.trimPath(dir) + File.separator;
        return this;
    }
}
