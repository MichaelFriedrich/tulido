package net.picocloud.tumblr.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigBuilderTest {



    @ParameterizedTest
    @CsvSource({",,", "a,b,", " \n, , ", "a, ,c"})
    void test1(String username, String password, String blogname) {
        Config cfg = ConfigBuilder.getInstance(username, password, blogname).chrome().build();
        cfg.driver.quit();
        assertFalse(cfg.isValid());
    }

    @Test
    void test2() {
        Config cfg = ConfigBuilder.getInstance("a", "b", "c").build();
        assertFalse(cfg.isValid());
    }

    @Test
    void test3() {
        Config cfg = ConfigBuilder.getInstance("a", "b", "c").chrome().build();
        cfg.driver.quit();
        assertTrue(cfg.isValid());
    }
}