package net.picocloud.tumblr;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class MediaLoaderTest {

    private static Path tmp;

    @BeforeClass
    public static void setup() throws IOException {
        tmp = Files.createTempDirectory("MediaLoaderTest");
    }

    @Test
    public void getMedium()  {
        MediaLoader ml = new MediaLoader();
        ml.getMedia(tmp.toString(),"https://64.media.tumblr.com/58589dd9647ae0ad64726ef0badd2ddb/2cd780c18824a716-5f/s540x810/c57abfc65e972de8e0fca1e42b732335150ecaa9.jpg");
        Path file = Path.of(tmp.toString() + File.separator + "c57abfc65e972de8e0fca1e42b732335150ecaa9.jpg");
        assertTrue(Files.exists(file));

    }

    @AfterClass
    public static void tearDown()  {
        delete(tmp.toFile());
    }

    private static void delete(File file) {
        if (file == null)
            return;
        if (file.isDirectory()) {
            for (var f : file.listFiles()) {
                delete(f);
            }
        }
        file.delete();
    }
}