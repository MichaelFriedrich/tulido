# Tumblr Likes Downloader (TuLiDo)

This program allows you to download all your likes form your tumblr account even if you have a restricted tumblr account.

## Prerequisites

* You need to have [Google Chrome](https://www.google.de/chrome/) installed.
* You need to have [Java 11](https://www.java.com) or above installed.
* On Mac computers, you have to trust the chromedriver with the command:<br>
  ``xattr -d com.apple.quarantine /usr/local/bin/chromedriver``<br>
  with the correct path to the binary

## Usage:
````
java -jar tulido.jar <username> <password> <blogname> [\<options\> ...]

Options:
-pages   : do not create all like pages
-vids    : do not create vids.txt
-posts   : do not create posts.txt
-pics    : do not create pics.txt
-firefox : use Firefox (excludes: -chrome)
-chrome  : use Chrome (default, excludes: -firefox)

Downloads all likes from the tumblr blog <blogname> with the given <username> and <password>.
If no options are given, the following files are created:
- directory pages containing all pages with the likes as html files,
- pics.txt containing all the urls of the pictures liked,
- posts.txt containing all the urls of the posts liked,
- vids.txt containing all the urls of the videos liked.

You can download the pictures or other files with "cat xxx.txt | xargs wget"
```