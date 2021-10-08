package net.picocloud.tumblr;

import net.picocloud.tumblr.web.Config;
import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 *
 */
public class MediaLoader {

    private static final Logger logger = Logger.getLogger(MediaLoader.class.getName());

    private static final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
            .setSoTimeout(Timeout.ofSeconds(30))
            .build();


    /**
     * downloads all urls in file and stores these files in targetDir
     * <p>
     * WARNING: all files in the target directory can get overwritten
     *
     * @param file      the path to the file with all the urls (one url per line)
     * @param targetDir the target directory where the downloads are stored. if the directory does not exist it gets created
     * @throws IOException if directory is not writable or cannot be created
     */
    public static void loadMediaFromFile(String file, String targetDir) throws IOException {
        Config.createTargetDir(targetDir);
        logger.info("Reading input file...");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Set<String> set = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                set.add(line);
            }
            logger.info("Starting download ...");
            getMedia(targetDir, set);
        }
        logger.info("Ready");
    }


    /**
     * max number of parallel http requests
     */
    private static final Semaphore semaphore = new Semaphore(20);

    /**
     * downloads all urls and stores the result in targetDir.
     * The download uses an async http client and can use a lot of bandwidth.
     * <p>
     * WARNING: targetDir must exist and must be writable.
     *
     * @param targetDir the absolute path to an existing and writable directory
     * @param urls      array of urls as text
     */
    public static void getMedia(String targetDir, Set<String> urls) {
        if (urls == null || urls.isEmpty())
            return;


        int size = urls.size();
        logger.info(() -> "Downloading " + size + " files to " + targetDir);

        final CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)

                .build();
        client.start();

        CountDownLatch latch = new CountDownLatch(size);

        // create async download tasks
        for (var url : urls) {
            createDownloadTask(targetDir, url, latch, client);
        }

        // wait for downloads to finish
        waitForAllDownloads(latch);
        client.close(CloseMode.GRACEFUL);
        logger.info(() -> "Downloading " + size + " files finished.");

    }

    private static void waitForAllDownloads(CountDownLatch latch) {
        long oldCount = latch.getCount();
        long tc = System.currentTimeMillis();
        while (latch.getCount() > 0) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    long count = latch.getCount();
                    float fps = ((float) (oldCount - count)) / (System.currentTimeMillis() - tc) * 1000;
                    oldCount = count;
                    tc = System.currentTimeMillis();
                    logger.info(() -> latch.getCount() + " files to process. " + fps + " files per second. ETA: " + count / fps + " seconds.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected static void createDownloadTask(String targetDir, String url, CountDownLatch latch, CloseableHttpAsyncClient client) {
        final String filename = targetDir + File.separator + url.substring(url.lastIndexOf("/") + 1);
        // don't redownload existing files
        if (Files.exists(Path.of(filename))) {
            latch.countDown();
            return;
        }
        final SimpleHttpRequest request = SimpleRequestBuilder.get().setUri(url).build();

        try {
            semaphore.acquire();
            final long t = System.currentTimeMillis();
            logger.fine(() -> "downloading start     : " + url);
            client.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    new FutureCallback<>() {
                        @Override
                        public void completed(final SimpleHttpResponse response) {
                            try (FileOutputStream fos = new FileOutputStream(filename)) {
                                fos.write(response.getBodyBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            long time = System.currentTimeMillis() - t;

                            logger.fine(() -> "downloading success: " + time + "msec : " + url);
                            latch.countDown();
                            semaphore.release();

                        }

                        @Override
                        public void failed(final Exception ex) {
                            logger.warning(request + "->" + ex);
                            ex.printStackTrace();
                            latch.countDown();
                            semaphore.release();
                        }

                        @Override
                        public void cancelled() {
                            logger.warning(request + " cancelled");
                            latch.countDown();
                            semaphore.release();
                        }

                    });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            semaphore.release();
        }
    }


    public static void main(String[] args) throws IOException {
        InputStream stream = TumblrApiCalls.class.getClassLoader().
                getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaLoader.loadMediaFromFile("sabinedl/videos.txt", "sabinedl/vids");
        MediaLoader.loadMediaFromFile("sabinedl/pics.txt", "sabinedl/pics");
    }
}
