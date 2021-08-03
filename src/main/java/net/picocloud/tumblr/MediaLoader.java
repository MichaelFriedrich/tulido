package net.picocloud.tumblr;

import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *
 */
public class MediaLoader {

    private static final Logger logger = Logger.getLogger(MediaLoader.class.getName());

    final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
            .setSoTimeout(Timeout.ofSeconds(15))
            .build();




    /**
     * downloads all urls in file and stores these files in targetDir
     *
     * WARNING: all files in the target directory can get overwritten
     *
     * @param file the path to the file with all the urls (one url per line)
     * @param targetDir the target directory where the downloads are stored. if the directory does not exist it gets created
     * @throws IOException if directory is not writable or cannot be created
     */
    public void loadMediaFromFile(String file, String targetDir) throws IOException {
        Config.createTargetDir(targetDir);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> list = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
            getMedia(targetDir, list.toArray(new String[0]));
        }
    }




    /**
     * downloads all urls and stores the result in targetDir.
     * The download uses an async http client and can use a lot of bandwidth.
     *
     * WARNING: targetDir must exist and must be writable.
     *
     * @param targetDir the absolute path to an existing and writable directory
     * @param urls array of urls as text
     */
    protected void getMedia(String targetDir, String... urls) {
        if (urls == null || urls.length == 0)
            return;

        final CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)
                .build();
        client.start();
        int size = urls.length;
        CountDownLatch latch = new CountDownLatch(size);

        // create async download tasks
        for (var url : urls) {
            createDownloadTask(targetDir, url, latch,client);
        }

        // wait for downloads to finish
        logger.info(() -> "Downloading " + size + " files.");
        long time = System.currentTimeMillis();
        while (latch.getCount() > 0) {
            try {
                latch.await(10, TimeUnit.SECONDS);
                long count = latch.getCount();
                if (count > 0) {
                    float fps = ((float) (size - count)) / (System.currentTimeMillis() - time) * 1000;
                    logger.info(() -> latch.getCount() + " files to process. " + fps + " files per second. ETA: " + count / fps + " seconds.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        client.close(CloseMode.GRACEFUL);
    }

    protected void createDownloadTask(String targetDir, String url, CountDownLatch latch, CloseableHttpAsyncClient client) {
        try {
            final SimpleHttpRequest request = SimpleRequestBuilder.get().setUri(url).build();

            client.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    new FutureCallback<>() {
                        private final URL uurl = new URL(url);
                        private final File dir = new File(targetDir);

                        @Override
                        public void completed(final SimpleHttpResponse response) {
                            String filename = uurl.getPath();
                            filename = dir.getAbsolutePath() + File.separator + filename.substring(filename.lastIndexOf("/") + 1);

                            try (FileOutputStream fos = new FileOutputStream(filename)){
                                fos.write(response.getBodyBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            latch.countDown();
                        }

                        @Override
                        public void failed(final Exception ex) {
                            logger.warning(request + "->" + ex);
                            ex.printStackTrace();
                            latch.countDown();
                        }

                        @Override
                        public void cancelled() {
                            logger.warning(request + " cancelled");
                            latch.countDown();
                        }

                    });

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException {
        MediaLoader ml = new MediaLoader();
        ml.loadMediaFromFile("pics.txt", "pics");
        ml.loadMediaFromFile("vids.txt", "vids");
    }
}
