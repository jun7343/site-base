package kr.devflix.clawler;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.DefaultJavaScriptErrorListener;
import kr.devflix.constant.PostType;
import kr.devflix.constant.Status;
import kr.devflix.entity.CrawlingLog;
import kr.devflix.entity.DevPost;
import kr.devflix.service.CrawlingLogService;
import kr.devflix.service.DevPostService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class WoowaDevPostCrawler implements Crawler {

    private final DevPostService devPostService;
    private final CrawlingLogService crawlingLogService;
    private static final SimpleDateFormat woowaDateFormat = new SimpleDateFormat("MMM d yyyy", Locale.ENGLISH);
    private static final String WOOWA_BLOG_URL = "https://woowabros.github.io";
    private static final Logger logger = LoggerFactory.getLogger(WoowaDevPostCrawler.class);
    private static final String DEFAULT_WOOWA_THUMBNAIL = "http://www.woowahan.com/img/mobile/woowabros.jpg";

    public WoowaDevPostCrawler(DevPostService devPostService, CrawlingLogService crawlingLogService) {
        this.devPostService = devPostService;
        this.crawlingLogService = crawlingLogService;
    }

    @Override
    public void crawling() {
        StringBuilder message = new StringBuilder();
        Boolean succcess = false;
        Integer totalCrawling = 0;
        final DevPost recentlyDevPost = devPostService.findRecentlyDevPost("WOOWA", PostType.BLOG);

        logger.info("Woowa dev blog crawling start ....");
        Long startAt = System.currentTimeMillis();
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.setJavaScriptErrorListener(new DefaultJavaScriptErrorListener());
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.waitForBackgroundJavaScript(3000);

            HtmlPage page = webClient.getPage(new URL(WOOWA_BLOG_URL));
            WebResponse response = page.getWebResponse();

            if (response.getStatusCode() == HttpStatus.SC_OK) {
                String content = response.getContentAsString();

                Element body = Jsoup.parse(content).body();

                if (body.getElementsByClass("list").size() > 0) {
                    Elements list = body.getElementsByClass("list").get(0).children();

                    if (list.size() > 0) {
                        Map<String, String> map = new HashMap<>();

                        for (Element element : list) {
                            try {
                                map.put("dateAndWriter", element.getElementsByClass("post-meta").get(0).text());
                            } catch (Exception e) {
                                map.put("writer", "우아한 형제들");
                                map.put("uploadDate", woowaDateFormat.format(new Date()));

                                logger.error("Woowa upload date and writer crawling error !! " + e.getMessage());
                            }

                            try {
                                map.put("url", WOOWA_BLOG_URL + element.getElementsByTag("a").get(0).attr("href"));
                            } catch (Exception e) {
                                map.put("url", WOOWA_BLOG_URL);

                                logger.error("Woowa URL crawling error !! " + e.getMessage());
                            }

                            try {
                                map.put("title", element.getElementsByTag("a").get(0).getElementsByClass("post-link").get(0).text());
                            } catch (Exception e) {
                                map.put("title", "우아한 형제들 기술 블로그");

                                logger.error("Woowa title crawling error !! " + e.getMessage());
                            }

                            try {
                                map.put("desc", element.getElementsByTag("a").get(0).getElementsByTag("p").text());
                            } catch (Exception e) {
                                map.put("desc", "");

                                logger.error("Woowa description crawling error !! " + e.getMessage());
                            }

                            if (map.containsKey("dateAndWriter")) {
                                String dateAndWriter = map.get("dateAndWriter");

                                String[] split = dateAndWriter.split(",");

                                if (split.length == 3) {
                                    String uploadDate = StringUtils.trim(split[0]) + " " + StringUtils.trim(split[1]);

                                    map.put("uploadDate", uploadDate);
                                    map.put("writer", StringUtils.trim(split[2]));
                                } else {
                                    map.put("uploadDate", woowaDateFormat.format(new Date()));
                                    map.put("writer", "우아한 형제들");
                                }
                            }

                            map.put("thumbnail", DEFAULT_WOOWA_THUMBNAIL);

                            Date date = new Date();

                            try {
                                date = woowaDateFormat.parse(map.get("uploadDate"));
                            } catch (ParseException e) {
                                logger.error("upload date parser error !! " + e.getMessage());
                            }

                            // 크롤링된 포스트와 DB 저장된 최근 포스트 일치 여부
                            if (recentlyDevPost != null) {
                                StringTokenizer st = new StringTokenizer(recentlyDevPost.getTitle());
                                StringTokenizer st1 = new StringTokenizer(map.get("title"));

                                int titleTokenSize = st.countTokens();
                                int cnt = 0;

                                while (st.hasMoreTokens()) {
                                    if (st1.hasMoreTokens()) {
                                        if (st.nextToken().equals(st1.nextToken())) {
                                            cnt++;
                                        }
                                    } else {
                                        break;
                                    }
                                }

                                StringTokenizer st2 = new StringTokenizer(recentlyDevPost.getDescription());
                                StringTokenizer st3 = new StringTokenizer(map.get("desc"));

                                int descTokenSize = st2.countTokens();
                                int cnt2 = 0;

                                while (st2.hasMoreTokens()) {
                                    if (st3.hasMoreTokens()) {
                                        if (st2.nextToken().equals(st3.nextToken())) {
                                            cnt2++;
                                        }
                                    } else {
                                        break;
                                    }
                                }

                                // title, description text가 단어 별로 6할 이상 맞으면 최근 게시물로 인정
                                if ((double) cnt / titleTokenSize >= 0.6 && (double) cnt2 / descTokenSize >= 0.6) {
                                    logger.info("Woowa dev blog crawling done !! total crawling count = " + totalCrawling);
                                    break;
                                }
                            }

                            DevPost post = DevPost.builder()
                                    .category("WOOWA")
                                    .postType(PostType.BLOG)
                                    .status(Status.POST)
                                    .title(map.get("title"))
                                    .description(map.get("desc"))
                                    .url(map.get("url"))
                                    .thumbnail(map.get("thumbnail"))
                                    .uploadAt(date)
                                    .view(0)
                                    .writer(map.get("writer"))
                                    .tags(new ArrayList<>())
                                    .createAt(new Date())
                                    .updateAt(new Date())
                                    .build();

                            devPostService.createDevPost(post);

                            map.clear();

                            logger.info("Woowa post crawling success !! URL = " + WOOWA_BLOG_URL + " post = " + post.toString());
                            totalCrawling++;
                        }

                        succcess = true;
                        message.append("Woowa dev blog crawling done !!");
                    } else {
                        logger.warn("Woowa dev post list size zero!!");
                        succcess = false;
                        message.append("Woowa dev post list size zero!!");
                    }
                } else {
                    logger.error("Woowa dev blog get error !! status code = " + response.getStatusCode());
                    succcess = false;
                    message.append("Woowa dev blog get error !! status code = ").append(response.getStatusCode());
                }
            } else {
                logger.error("Woowa deb blog get page error !! status code = " + response.getStatusCode());
                succcess = false;
                message.append("Woowa deb blog get page error !! status code = ").append(response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Woowa blog crawling error !! " + e.getMessage());
            succcess = false;
            message.append("Woowa blog crawling error !! ").append(e.getMessage());
        }

        logger.info("Woowa dev blog crawling end ....");
        Long endAt = System.currentTimeMillis();

        CrawlingLog log = CrawlingLog.builder()
                .jobName("Woowa dev blog crawling")
                .jobStartAt(startAt)
                .jobEndAt(endAt)
                .success(succcess)
                .message(message.toString())
                .totalCrawling(totalCrawling)
                .createAt(new Date())
                .updateAt(new Date())
                .build();

        crawlingLogService.createCrawlingSchedulerLog(log);
    }
}
