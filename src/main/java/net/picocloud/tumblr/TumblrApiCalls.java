package net.picocloud.tumblr;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        InputStream stream = TumblrApiCalls.class.getClassLoader().
                getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }



       /* logger.info(() -> {
            return Arrays.toString(getFollowableBlognames().toArray());
        });

        */
    }

    /**
     * get all blognames the current user follows
     *
     * @return
     */
    public static Set<String> getFollowedBlogNames() {
        JumblrClient client = ClientFactory.instance();
        return blog2BlogName(getPagedResources(client::userFollowing));
    }

    private static Set<String> blog2BlogName(Set<Blog> blogs) {
        return blogs.stream().map(Blog::getName).collect(Collectors.toSet());
    }

    private static Set<String> user2UserName(Set<User> blogs) {
        return blogs.stream().map(User::getName).collect(Collectors.toSet());
    }

    public static Set<String> getFollowedByUserNames(final String blogname) {
        JumblrClient client = ClientFactory.instance();
        return user2UserName(getPagedResources(b -> client.blogFollowers(blogname)));
    }


    public static Set<String> getMediaUrlsFromBlogPosts(String blogname) {
        JumblrClient client = ClientFactory.instance();
        var posts = getPagedResources(options -> client.blogPosts(blogname, options));
        return getMediaUrlsFromPosts(posts);
    }

    private static Set<String> getMediaUrlsFromPosts(Set<Post> posts) {
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
                    logger.warning(() -> "Unknown post type : " + post.getType());
            }
        }
        return set;
    }

    private static <E extends Resource> Set<E> getPagedResources(Function<Map<String, ?>, List<E>> function) {
        Set<E> set = new HashSet<>();
        Map<String, Object> options = new HashMap<>();
        Integer offset = 0;
        var x = function.apply(options);
        while (!x.isEmpty()) {
            set.addAll(x);

            offset += 20;
            options.put("offset", offset);

            try {
                x = function.apply(options);
            } catch (JumblrException je) {
                if (je.getResponseCode() == 429) {// limit exceed
                    logger.info("Rate Limit reached. Waiting two minutes.");
                    try {
                        Thread.sleep(2 * 60000);
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


    public static Set<String> getFollowableBlognames() {
        var dir = Path.of("sabinedl", "pages");

        Set<String> set = new HashSet<>();
        try {
            return Files.list(dir).map(p -> getFollowableBlognames(p)).flatMap(sets -> sets.stream()).collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Set<String> getFollowableBlognames(Path path) {
        try {
            if (!Files.isDirectory(path)) {

                Document doc = Jsoup.parse(path.toFile(), null);
                return doc.getElementsByClass("reblog_follow_button").stream().map(e -> e.attr("data-tumblelog-name")).collect(Collectors.toSet());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashSet<>();
    }

    public static Blog getInfo(String blogname) {
        return ClientFactory.instance().blogInfo(blogname);
    }

}
