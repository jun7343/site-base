package com.devflix.clawler;

import com.devflix.constant.PostStatus;
import com.devflix.constant.PostType;
import com.devflix.entity.DevPost;
import com.devflix.entity.YoutubeChannel;
import com.devflix.service.DevPostService;
import com.devflix.service.YoutubeChannelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/*
    reference - https://developers.google.com/youtube/v3/docs/search
                https://developers.google.com/youtube/v3/docs/channels
 */
@Component
public class YoutubeCrawler implements Crawler {

    private final String API_YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private final String API_YOUTUBE_CHANNEL_URL = "https://www.googleapis.com/youtube/v3/channels";
    private final String YOUTUBE_VIDEO_URL = "https://www.youtube.com/watch?v=";
    // youtube channel id for search, channel parameter
    private final String ID = "id";
    // youtube channel name for search, channer parameter
    private final String FOR_URSE_NAME = "forUserName";
    // next page or previous page token for search, channel parameter
    private final String PAGE_TOKEN = "pageToken";
    // result size(default 5) for search, channel parameter
    private final String MAX_RESULTS = "maxResults";
    private final int DEFAULT_MAX_RESULT_SIZE = 50;
    // search type(channel, playlist, video) for search parmeter
    private final String TYPE = "type";
    // search result ordering(date, rating, relevance, title, videoCount, viewCount) for search parmeter
    private final String ORDER = "order";
    // channel id for search parameter
    private final String CHANNEL_ID = "channelId";
    private final String KEY = "key";
    private final String PART = "part";
    private final String YOUTUBE_API_KEY;
    // youtube date format ISO8601
    private final StdDateFormat youtubeDateFormat = new StdDateFormat();
    private final Logger logger = LoggerFactory.getLogger(YoutubeCrawler.class);
    private final YoutubeChannelService youtubeChannelService;
    private final DevPostService devPostService;

    public YoutubeCrawler(YoutubeChannelService youtubeChannelService, DevPostService devPostService, Environment environment) {
        this.YOUTUBE_API_KEY = environment.getProperty("youtube.data.api-key");
        this.youtubeChannelService = youtubeChannelService;
        this.devPostService = devPostService;
    }

    @Override
    public void crawling() {
        List<YoutubeChannel> findAll = youtubeChannelService.findAllOrderByIdDesc();

        logger.info("Youtube video crawling start ...");
        for (int channelNum = 0; channelNum < findAll.size(); channelNum++) {
            final YoutubeChannel channel = findAll.get(channelNum);
            final DevPost recentlyDevPost = devPostService.findRecentlyDevPost(channel.getCategory(), PostType.YOUTUBE, channel.getChannelTitle());
            int totalCrawling = 0;
            boolean check = false;

            UriComponents build = UriComponentsBuilder.fromHttpUrl(API_YOUTUBE_SEARCH_URL)
                    .queryParam(KEY, YOUTUBE_API_KEY)
                    .queryParam(PART, "id", "snippet")
                    .queryParam(MAX_RESULTS, DEFAULT_MAX_RESULT_SIZE)
                    .queryParam(ORDER, "date")
                    .queryParam(TYPE, "video")
                    .queryParam(CHANNEL_ID, channel.getChannelId())
                    .build();

            try {
                URL url = build.toUri().toURL();

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                ObjectMapper mapper = new ObjectMapper();

                JsonNode result = mapper.readTree(connection.getInputStream());
                JsonNode items = result.get("items");

                for (int i = 0; i < items.size(); i++) {
                    JsonNode id = items.get(i).get("id");
                    JsonNode videoId = id.get("videoId");
                    JsonNode snippet = items.get(i).get("snippet");
                    JsonNode channelTitle = snippet.get("channelTitle");
                    JsonNode title = snippet.get("title");
                    JsonNode description = snippet.get("description");
                    JsonNode publishedAt = snippet.get("publishedAt");
                    JsonNode thumbnails = snippet.get("thumbnails");
                    JsonNode thumbnailshigh = thumbnails.get("high");
                    JsonNode thumbnailshighUrl = thumbnailshigh.get("url");

                    Date publishDate = new Date();

                    try {
                        publishDate = youtubeDateFormat.parse(publishedAt.asText());
                    } catch (ParseException e) {
                        logger.error("Youtube video publish date parsing error !! " + e.getMessage());
                    }

                    if (recentlyDevPost != null) {
                        if (StringUtils.equals(recentlyDevPost.getUrl(), YOUTUBE_VIDEO_URL + videoId.asText())) {
                            check = true;
                            break;
                        }
                    }

                    DevPost post = DevPost.builder()
                            .category(channel.getCategory())
                            .postType(PostType.YOUTUBE)
                            .title(title.asText())
                            .description(description.asText().replaceAll("\\R", ""))
                            .writer(channelTitle.asText())
                            .url(YOUTUBE_VIDEO_URL + videoId.asText())
                            .tag(null)
                            .thumbnail(thumbnailshighUrl.asText())
                            .status(PostStatus.POST)
                            .view(0)
                            .uploadAt(publishDate)
                            .createAt(new Date())
                            .updateAt(new Date())
                            .build();

                    DevPost createPost = devPostService.createDevPost(post);
                    logger.info("Youtube " + channel.getChannelTitle() +" video crawling success !! post = " + createPost.toString());
                    totalCrawling++;
                }

                if (result.has("nextPageToken") && ! check) {
                    JsonNode pageInfo = result.get("pageInfo");
                    JsonNode totalResults = pageInfo.get("totalResults");
                    JsonNode resultsPerPage = pageInfo.get("resultsPerPage");
                    JsonNode nextPageToken = result.get("nextPageToken");

                    int start = 1;
                    int end = totalResults.asInt() / resultsPerPage.asInt();

                    if (totalResults.asInt() % resultsPerPage.asInt() != 0) {
                        end++;
                    }

                    for (; start < end; start++) {
                        UriComponents build1 = UriComponentsBuilder.fromHttpUrl(API_YOUTUBE_SEARCH_URL)
                                .queryParam(KEY, YOUTUBE_API_KEY)
                                .queryParam(PART, "id", "snippet")
                                .queryParam(MAX_RESULTS, DEFAULT_MAX_RESULT_SIZE)
                                .queryParam(ORDER, "date")
                                .queryParam(TYPE, "video")
                                .queryParam(CHANNEL_ID, channel.getChannelId())
                                .queryParam(PAGE_TOKEN, nextPageToken.asText())
                                .build();


                        URL url1 = build1.toUri().toURL();
                        HttpURLConnection connection1 = (HttpURLConnection) url1.openConnection();
                        ObjectMapper mapper1 = new ObjectMapper();

                        JsonNode result1 = mapper1.readTree(connection1.getInputStream());
                        JsonNode items1 = result1.get("items");

                        for (int i = 0; i < items1.size(); i++) {
                            JsonNode id = items1.get(i).get("id");
                            JsonNode videoId = id.get("videoId");
                            JsonNode snippet = items1.get(i).get("snippet");
                            JsonNode channelTitle = snippet.get("channelTitle");
                            JsonNode title = snippet.get("title");
                            JsonNode description = snippet.get("description");
                            JsonNode publishedAt = snippet.get("publishedAt");
                            JsonNode thumbnails = snippet.get("thumbnails");
                            JsonNode thumbnailshigh = thumbnails.get("high");
                            JsonNode thumbnailshighUrl = thumbnailshigh.get("url");

                            Date publishDate = new Date();

                            try {
                                publishDate = youtubeDateFormat.parse(publishedAt.asText());
                            } catch (ParseException e) {
                                logger.error("Youtube " + channel.getChannelTitle() +" video publish date parsing error !! " + e.getMessage());
                            }

                            if (recentlyDevPost != null) {
                                if (StringUtils.equals(recentlyDevPost.getUrl(), YOUTUBE_VIDEO_URL + videoId.asText())) {
                                    check = true;
                                    break;
                                }
                            }

                            DevPost post = DevPost.builder()
                                    .category(channel.getCategory())
                                    .postType(PostType.YOUTUBE)
                                    .title(title.asText())
                                    .description(description.asText().replaceAll("\\R", ""))
                                    .writer(channelTitle.asText())
                                    .url(YOUTUBE_VIDEO_URL + videoId.asText())
                                    .tag(null)
                                    .thumbnail(thumbnailshighUrl.asText())
                                    .status(PostStatus.POST)
                                    .view(0)
                                    .uploadAt(publishDate)
                                    .createAt(new Date())
                                    .updateAt(new Date())
                                    .build();

                            DevPost createPost = devPostService.createDevPost(post);
                            logger.info("Youtube " + channel.getChannelTitle() + " video save success !! post = " + createPost.toString());
                            totalCrawling++;
                        }

                        if (result1.has("nextPageToken") && ! check && result1.get("pageInfo").get("resultsPerPage").asInt() == DEFAULT_MAX_RESULT_SIZE) {
                            nextPageToken = result1.get("nextPageToken");
                        } else {
                            logger.info("Youtube " + channel.getChannelTitle()  +" video crawling done !! total video crawling count = " + totalCrawling);
                            break;
                        }
                    }
                } else {
                    logger.info("Youtube " + channel.getChannelTitle() + " video crawling done !! total video crawling count = " + totalCrawling);
                }
            } catch (MalformedURLException e) {
                logger.error("Youtube " + channel.getChannelTitle() +" url connetion error !! " + e.getMessage());
            } catch (IOException e) {
                logger.error("Youtube " + channel.getChannelTitle() +" video IO Exception error !! " + e.getMessage());
            }
        }
        logger.info("Youtube video crawling end ...");
    }

    public void targetCrawling(final String channelId, final String category) {
        YoutubeChannel findChannel = youtubeChannelService.getByChannelId(channelId);

        if (findChannel == null) {
            findChannel = saveChannelInfoByChannelId(channelId, category);
        }

        final DevPost recentlyDevPost = devPostService.findRecentlyDevPost(findChannel.getCategory(), PostType.YOUTUBE, findChannel.getChannelTitle());
        int totalCrawling = 0;
        boolean check = false;

        UriComponents build = UriComponentsBuilder.fromHttpUrl(API_YOUTUBE_SEARCH_URL)
                .queryParam(KEY, YOUTUBE_API_KEY)
                .queryParam(PART, "id", "snippet")
                .queryParam(MAX_RESULTS, DEFAULT_MAX_RESULT_SIZE)
                .queryParam(ORDER, "date")
                .queryParam(TYPE, "video")
                .queryParam(CHANNEL_ID, findChannel.getChannelId())
                .build();

        logger.info("Youtube " + findChannel.getChannelTitle() + " video crawling start ...");
        try {
            URL url = build.toUri().toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            ObjectMapper mapper = new ObjectMapper();

            JsonNode result = mapper.readTree(connection.getInputStream());
            JsonNode items = result.get("items");

            for (int i = 0; i < items.size(); i++) {
                JsonNode id = items.get(i).get("id");
                JsonNode videoId = id.get("videoId");
                JsonNode snippet = items.get(i).get("snippet");
                JsonNode channelTitle = snippet.get("channelTitle");
                JsonNode title = snippet.get("title");
                JsonNode description = snippet.get("description");
                JsonNode publishedAt = snippet.get("publishedAt");
                JsonNode thumbnails = snippet.get("thumbnails");
                JsonNode thumbnailshigh = thumbnails.get("high");
                JsonNode thumbnailshighUrl = thumbnailshigh.get("url");

                Date publishDate = new Date();

                try {
                    publishDate = youtubeDateFormat.parse(publishedAt.asText());
                } catch (ParseException e) {
                    logger.error("Youtube " + findChannel.getChannelTitle() + " video publish date parsing error !! " + e.getMessage());
                }

                if (recentlyDevPost != null) {
                    if (recentlyDevPost != null) {
                        if (StringUtils.equals(recentlyDevPost.getUrl(), YOUTUBE_VIDEO_URL + videoId.asText())) {
                            check = true;
                            break;
                        }
                    }
                }

                DevPost post = DevPost.builder()
                        .category(findChannel.getCategory())
                        .postType(PostType.YOUTUBE)
                        .title(title.asText())
                        .description(description.asText().replaceAll("\\R", ""))
                        .writer(channelTitle.asText())
                        .url(YOUTUBE_VIDEO_URL + videoId.asText())
                        .tag(null)
                        .thumbnail(thumbnailshighUrl.asText())
                        .status(PostStatus.POST)
                        .view(0)
                        .uploadAt(publishDate)
                        .createAt(new Date())
                        .updateAt(new Date())
                        .build();

                DevPost createPost = devPostService.createDevPost(post);
                logger.info("Youtube " + findChannel.getChannelTitle() + " video crawling success !! post = " + createPost.toString());
                totalCrawling++;
            }

            if (result.has("nextPageToken") && ! check) {
                JsonNode pageInfo = result.get("pageInfo");
                JsonNode totalResults = pageInfo.get("totalResults");
                JsonNode resultsPerPage = pageInfo.get("resultsPerPage");
                JsonNode nextPageToken = result.get("nextPageToken");

                int start = 1;
                int end = totalResults.asInt() / resultsPerPage.asInt();

                if (totalResults.asInt() % resultsPerPage.asInt() != 0) {
                    end++;
                }

                for (; start < end; start++) {
                    UriComponents build1 = UriComponentsBuilder.fromHttpUrl(API_YOUTUBE_SEARCH_URL)
                            .queryParam(KEY, YOUTUBE_API_KEY)
                            .queryParam(PART, "id", "snippet")
                            .queryParam(MAX_RESULTS, DEFAULT_MAX_RESULT_SIZE)
                            .queryParam(ORDER, "date")
                            .queryParam(TYPE, "video")
                            .queryParam(CHANNEL_ID, findChannel.getChannelId())
                            .queryParam(PAGE_TOKEN, nextPageToken.asText())
                            .build();


                    URL url1 = build1.toUri().toURL();
                    HttpURLConnection connection1 = (HttpURLConnection) url1.openConnection();
                    ObjectMapper mapper1 = new ObjectMapper();

                    JsonNode result1 = mapper1.readTree(connection1.getInputStream());
                    JsonNode items1 = result1.get("items");

                    for (int i = 0; i < items1.size(); i++) {
                        JsonNode id = items1.get(i).get("id");
                        JsonNode videoId = id.get("videoId");
                        JsonNode snippet = items1.get(i).get("snippet");
                        JsonNode channelTitle = snippet.get("channelTitle");
                        JsonNode title = snippet.get("title");
                        JsonNode description = snippet.get("description");
                        JsonNode publishedAt = snippet.get("publishedAt");
                        JsonNode thumbnails = snippet.get("thumbnails");
                        JsonNode thumbnailshigh = thumbnails.get("high");
                        JsonNode thumbnailshighUrl = thumbnailshigh.get("url");

                        Date publishDate = new Date();

                        try {
                            publishDate = youtubeDateFormat.parse(publishedAt.asText());
                        } catch (ParseException e) {
                            logger.error("Youtube " + findChannel.getChannelTitle() + "  video publish date parsing error !! " + e.getMessage());
                        }

                        if (recentlyDevPost != null) {
                            if (recentlyDevPost != null) {
                                if (StringUtils.equals(recentlyDevPost.getUrl(), YOUTUBE_VIDEO_URL + videoId.asText())) {
                                    check = true;
                                    break;
                                }
                            }
                        }

                        DevPost post = DevPost.builder()
                                .category(findChannel.getCategory())
                                .postType(PostType.YOUTUBE)
                                .title(title.asText())
                                .description(description.asText().replaceAll("\\R", ""))
                                .writer(channelTitle.asText())
                                .url(YOUTUBE_VIDEO_URL + videoId.asText())
                                .tag(null)
                                .thumbnail(thumbnailshighUrl.asText())
                                .status(PostStatus.POST)
                                .view(0)
                                .uploadAt(publishDate)
                                .createAt(new Date())
                                .updateAt(new Date())
                                .build();

                        DevPost createPost = devPostService.createDevPost(post);
                        logger.info("Youtube " + findChannel.getChannelTitle() + " video save success !! post = " + createPost.toString());
                        totalCrawling++;
                    }

                    if (result1.has("nextPageToken") && ! check && result1.get("pageInfo").get("resultsPerPage").asInt() == DEFAULT_MAX_RESULT_SIZE) {
                        nextPageToken = result1.get("nextPageToken");
                    } else {
                        logger.info("Youtube " + findChannel.getChannelTitle() + " video crawling done !! total video crawling count = " + totalCrawling);
                        break;
                    }
                }
            } else {
                logger.info("Youtube " + findChannel.getChannelTitle() + "video crawling done !! total video crawling count = " + totalCrawling);
            }
        } catch (MalformedURLException e) {
            logger.error("Youtube " + findChannel.getChannelTitle() + " video url connetion error !! " + e.getMessage());
        } catch (IOException e) {
            logger.error("Youtube " + findChannel.getChannelTitle() + " video IO Exception error !! " + e.getMessage());
        }
        logger.info("Youtube " + findChannel.getChannelTitle() + " video crawling end ...");
    }

    public YoutubeChannel saveChannelInfoByChannelId(final String channelId, final String category) {
        YoutubeChannel saveChannel = null;

        if (StringUtils.isBlank(YOUTUBE_API_KEY)) {
            logger.error("Youtube channel save error !! youtube api key is null");
            return saveChannel;
        }

        UriComponents components = UriComponentsBuilder.fromHttpUrl(API_YOUTUBE_CHANNEL_URL)
                .queryParam(KEY, YOUTUBE_API_KEY)
                .queryParam(PART, "id", "snippet")
                .queryParam(ID, channelId)
                .build();

        logger.info("Youtube channel save start .... channel id = " + channelId);
        try {
            URL url = components.toUri().toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode result = mapper.readTree(connection.getInputStream());

                if (result.has("items")) {
                    JsonNode items = result.get("items").get(0);

                    JsonNode id = items.get("id");
                    JsonNode etag = items.get("etag");
                    JsonNode snippet = items.get("snippet");
                    JsonNode title = snippet.get("title");
                    JsonNode description = snippet.get("description");
                    JsonNode publishedAt = snippet.get("publishedAt");
                    JsonNode thumbnails = snippet.get("thumbnails");
                    JsonNode thumbnailsHigh = thumbnails.get("high");
                    JsonNode thumbnailsHighUrl = thumbnailsHigh.get("url");

                    Date publishDate = new Date();

                    try {
                        publishDate = youtubeDateFormat.parse(publishedAt.asText());
                    } catch (ParseException e) {
                        logger.error("Youtube channel publish date parsing error !! " + e.getMessage());
                    }

                    YoutubeChannel channel = YoutubeChannel.builder()
                            .category(category)
                            .channelId(id.asText())
                            .etag(etag.asText())
                            .channelTitle(title.asText())
                            .description(description.asText().replaceAll("\\R", ""))
                            .thumbnail(thumbnailsHighUrl.asText())
                            .publishAt(publishDate)
                            .createAt(new Date())
                            .updateAt(new Date())
                            .build();

                    try {
                        saveChannel = youtubeChannelService.createYoutubeChannel(channel);

                        logger.info("Youtube channel save success !! channel info = " + saveChannel.toString());
                    } catch (Exception e) {
                        logger.error("Youtube channel save error !! ", e.getMessage());
                    }
                } else {
                    logger.error("Youtbue channel save error !! no items");
                }
            }
        } catch (MalformedURLException e) {
            logger.error("Youtube save channel URL Exception !! " + e.getMessage());
        } catch (IOException e) {
            logger.error("Yuotube save channel IO Exception error !! " + e.getMessage());
        }
        logger.info("Youtube channel save end ....");

        return saveChannel;
    }
}
