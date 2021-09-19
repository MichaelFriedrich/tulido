package net.picocloud.tumblr;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TumblrApiCalls {

    // ***** logging *****
    private static Logger logger = null;

    static {
        InputStream stream = TumblrApiCalls.class.getClassLoader().
                getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger = Logger.getLogger(TumblrApiCalls.class.getName());
    }

    // *** get singleton JumblrClient
    private static class ClientFactory {

        private static JumblrClient client = null;

        private ClientFactory() {
        }

        static synchronized JumblrClient instance() {
            if (client == null) {
                client = new JumblrClient("lOXYhystpRTSSWxNhTfKwhbIdnAOa78ACsZ50KW3u8qoGsShKA", "LaumlBVj7KY4VCAHq8blsjaJjF5Ic5WkqfrgdetZul4D79h7NV");
                client.setToken(
                        "arcSql5KvYRCOnr2R7IjYjnwrMkic1LKcE0SQDFBWUwsHPm2dU",
                        "VuhVJoqSrtS8V93Lnl3sdSfM2AYSqdgyZfIn2AnzE4aBOy4Re0"
                );
            }
            return client;
        }
    }


    public static void main(String[] args) {

        String blogname = "fgdia";
        logger.info("test");

        JumblrClient client = ClientFactory.instance();
        System.out.println(getMediaUrlsFromPosts(getPagedResources(options -> client.blogPosts(blogname, options))));
        System.out.println(getFollowedBlogNames(getPagedResources(options -> client.userFollowing(options))));

/*        var urls = getBlogPostMediaUrls(blogname);

        try (FileWriter fw = new FileWriter(blogname + ".txt")) {
            for (var s : urls) {
                fw.write(s);
                fw.write('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

*/

        /*
        Post p = client.blogPost("fgdia", 659802448333062144L);
        System.out.println(p.getShortUrl());
        System.out.println(p.getSourceUrl());
        p = client.blogPost("sabinedl", 639362558612193280L);
        System.out.println(p.getRebloggedFromId());
 */
    }

    static Set<String> getFollowedBlogNames(Set<Blog> blogs) {
        return blogs.stream().map(Blog::getName).collect(Collectors.toSet());
    }

    static Set<String> getMediaUrlsFromPosts(Set<Post> posts) {
        Set<String> set = new HashSet<>();
        for (var post : posts) {
            switch (post.getType()) {
                case PHOTO:
                    for (var photo : ((PhotoPost) post).getPhotos()) {
                        set.add(photo.getOriginalSize().getUrl());
                    }
                    break;
                case TEXT:
                    String body = ((TextPost) post).getBody();
                    pickUrls(set, body);
                    break;
                case VIDEO:
                    for (var video : ((VideoPost) post).getVideos()) {
                        pickUrls(set, video.getEmbedCode());
                    }
                    break;
                case ANSWER:
                default:
                    System.out.println("Unknown post type : " + post.getType());
            }
        }
        return set;
    }


     static <E extends Resource> Set<E> getPagedResources(Function<Map, List> function) {
        Set<E> set = new HashSet<>();
        Map options = new HashMap<>();
        int offset = 0;
        var x = function.apply(options);;
        while (!x.isEmpty()) {
            set.addAll(x);

            offset += 20;
            options.put("offset", offset);

            try {
                x = function.apply(options);
            } catch (JumblrException je) {
                if (je.getResponseCode() == 429) {// limit exceed
                    logger.info("Rate Limit reached. Waiting one minute.");
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    logger.log(Level.WARNING, je.getMessage(), je);
                    break;
                }
            }
        }
        return set;
    }


    private static void pickUrls(Set<String> set, String body) {
        final String token = " src=\"";
        int start = body.indexOf(token);
        int end;
        while (start != -1) {
            start += token.length();
            end = body.indexOf("\"", start);
            String url = body.substring(start, end);

            set.add(url);
            body = body.substring(end);
            start = body.indexOf(token);
        }
    }
}
