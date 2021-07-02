package net.picocloud.tumblr;

import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MediaLoader {
    public boolean loadMedia(String file, String targetDir) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            return false;
        }
        List<String> list = new ArrayList<String>();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
        } catch (IOException e) {
            return false;
        }
        try {
            getMedium(targetDir, list.toArray(new String[0]));
        } catch (MalformedURLException e) {
            return false;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    //private static final var logger = Logger.getLogger(MediaLoader.class.getName())
    protected void getMedium(String targetDir, String... urls) throws MalformedURLException, FileNotFoundException, IOException {
        Path path = Path.of(targetDir);
        File dir = path.toFile();
        if (!dir.isDirectory() || dir.isHidden() || !Files.isWritable(path)) {
            //TDOD: Handle error
            System.err.println("Target dir not writeable!");
            return;
        }

        for (var url : urls) {
            URL uurl = new URL(url);
            HttpClient client = HttpClient.Factory.createDefault().createClient(uurl);
            HttpRequest request = new HttpRequest(HttpMethod.GET, url);
            HttpResponse response = client.execute(request);
            InputStream is = response.getContentStream();
            String filename = uurl.getPath();
            filename = dir.getAbsolutePath() + File.separator + filename.substring(filename.lastIndexOf("/") + 1);
            System.out.println(filename);
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(is.readAllBytes());
            fos.close();
            is.close();
        }
    }


    public static void main(String[] args) {
        MediaLoader ml = new MediaLoader();
        ml.loadMedia("old2/pics.txt","loadTarget");
    }
}
