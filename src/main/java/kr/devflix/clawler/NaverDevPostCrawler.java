package kr.devflix.clawler;

import kr.devflix.constant.Status;
import kr.devflix.constant.PostType;
import kr.devflix.entity.CrawlingLog;
import kr.devflix.entity.DevPost;
import kr.devflix.service.CrawlingLogService;
import kr.devflix.service.DevPostService;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.DefaultJavaScriptErrorListener;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
@RequiredArgsConstructor
public class NaverDevPostCrawler implements Crawler {

    private final DevPostService devPostService;
    private final SimpleDateFormat naverDateFormat = new SimpleDateFormat("yyyy.MM.dd");
    private final String NAVER_BLOG_URL = "https://d2.naver.com";
    private final String DEFAULT_NAVER_THUMBNAIL = "https://d2.naver.com/static/img/app/common/sns_share_big_img1.png";
    private final CrawlingLogService crawlingLogService;
    private final Logger logger = LoggerFactory.getLogger(NaverDevPostCrawler.class);
    private final int DEFAULT_CRAWLING_MAX_PAGE = 3;

    @Override
    public void crawling() {
        final DevPost recentlyDevPost = devPostService.findRecentlyDevPost("NAVER", PostType.BLOG);
        int totalCrawling = 0;
        boolean success = false;
        String message = "";

        logger.info("Naver dev blog crawling start ....");
        long startAt = System.currentTimeMillis();
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.setJavaScriptErrorListener(new DefaultJavaScriptErrorListener());
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.waitForBackgroundJavaScript(3000);

            for (int page = 0; page <= DEFAULT_CRAWLING_MAX_PAGE; page++) {
                HtmlPage htmlPage = webClient.getPage(new URL(NAVER_BLOG_URL + "/home?page=" + page));
                WebResponse response = htmlPage.getWebResponse();

                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    String content = htmlPage.getBody().asXml();
                    Element body = Jsoup.parse(content).body();
                    Elements contents = body.getElementsByClass("contents");

                    if (contents.size() > 0) {
                        Elements elements = contents.get(0).children();

                        if (elements.size() > 0) {
                            for (int i = 0; i < elements.size(); i++) {
                                if (elements.get(i).hasClass("post_article")) {
                                    Map<String, String> map = new HashMap<>();

                                    try {
                                        map.put("title", elements.get(i).getElementsByTag("h2").get(0).text());
                                    } catch (Exception e) {
                                        map.put("title", "네이버 기술 블로그");
                                        logger.error("Naver title crawling error !! " + e.getMessage());
                                    }

                                    try {
                                        map.put("desc", elements.get(i).getElementsByClass("post_txt").get(0).text());
                                    } catch (Exception e) {
                                        map.put("desc", "");
                                        logger.error("Naver description crawling error !! " + e.getMessage());
                                    }

                                    try {
                                        map.put("url", NAVER_BLOG_URL + elements.get(i).getElementsByTag("h2").get(0).getElementsByTag("a").get(0).attr("href"));
                                    } catch (Exception e) {
                                        map.put("url", NAVER_BLOG_URL);

                                        logger.error("Naver URL crawling error !! " + e.getMessage());
                                    }

                                    try {
                                        map.put("uploadDate", elements.get(i).getElementsByTag("dl").get(0).getElementsByTag("dd").get(0).text());
                                    } catch (Exception e) {
                                        map.put("uploadDate", naverDateFormat.format(new Date()));
                                        logger.error("Naver upload date crawling error !! " + e.getMessage());
                                    }

                                    try {
                                        map.put("thumbnail", NAVER_BLOG_URL + elements.get(i).getElementsByClass("cont_img").get(0).getElementsByTag("img").get(0).attr("src"));
                                    } catch (Exception e) {
                                        map.put("thumbnail", DEFAULT_NAVER_THUMBNAIL);
                                        logger.error("Naevr thumbnail crawling error !! " + e.getMessage());
                                    }

                                    Date date = new Date();

                                    try {
                                        date = naverDateFormat.parse(map.get("uploadDate"));
                                    } catch (ParseException e) {
                                        logger.error("Naver upload date parsing error !! " + e.getMessage());
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
                                        int cnt1 = 0;

                                        while (st2.hasMoreTokens()) {
                                            if (st3.hasMoreTokens()) {
                                                if (st2.nextToken().equals(st3.nextToken())) {
                                                    cnt1++;
                                                } else {
                                                    break;
                                                }
                                            }
                                        }

                                        // title, description text가 단어 별로 6할 이상 맞으면 최근 게시물로 인정
                                        if ((double) cnt / titleTokenSize >= 0.6 && (double) cnt1 / descTokenSize >= 0.6) {
                                            logger.info("Naver dev blog crawling done !! total crawling count = " + totalCrawling);
                                            success = true;
                                            message = "Naver dev blog crawling done !!";
                                            break;
                                        }
                                    }

                                    Map<String, Object> writerAndTag = getWriterAndTags(map.get("url"));

                                    DevPost post = DevPost.builder()
                                            .category("NAVER")
                                            .postType(PostType.BLOG)
                                            .status(Status.POST)
                                            .title(map.get("title"))
                                            .description(map.get("desc"))
                                            .url(map.get("url"))
                                            .thumbnail(map.get("thumbnail"))
                                            .writer(writerAndTag.get("writer") == null? "네이버" : String.valueOf(writerAndTag.get("writer")))
                                            .tag(writerAndTag.get("tagList") == null? new ArrayList<>() : (List<String>) writerAndTag.get("tagList"))
                                            .view(0)
                                            .uploadAt(date)
                                            .createAt(new Date())
                                            .updateAt(new Date())
                                            .build();

                                    devPostService.createDevPost(post);
                                    logger.info("Naver post crawling success !! URL = " + NAVER_BLOG_URL + "/helloworld?page=" + page + " post = " + post.toString());
                                    totalCrawling++;
                                }
                            }

                            if (success) {
                                break;
                            }
                        } else {
                            logger.error("Naver post item size zero !!");
                            success = false;
                            message = "Naver post item size zero !!";
                            break;
                        }
                    } else {
                        logger.error("Naver post list document not found !!");
                        success = false;
                        message = "Naver post list document not found !!";
                        break;
                    }
                } else {
                    logger.error("Naver dev blog get error !! status code = " + response.getStatusCode());
                    success = false;
                    message = "Naver dev blog get error !! status code = " + response.getStatusCode();
                    break;
                }

                if (DEFAULT_CRAWLING_MAX_PAGE == page) {
                    success = true;
                    message = "Naver dev blog crawling done !!";
                }
            }
        } catch (Exception e) {
            logger.error("Naver dev blog Webclient error !! " + e.getMessage());
            success = false;
            message = "Naver dev blog Webclient error !! " + e.getMessage();
        }

        logger.info("Naver dev blog crawling end ....");
        long endAt = System.currentTimeMillis();

        CrawlingLog log = CrawlingLog.builder()
                .jobName("Naver dev blog crawling")
                .jobStartAt(startAt)
                .jobEndAt(endAt)
                .message(message)
                .success(success)
                .totalCrawling(totalCrawling)
                .createAt(new Date())
                .updateAt(new Date())
                .build();

        crawlingLogService.createCrawlingSchedulerLog(log);
    }

    private Map<String, Object> getWriterAndTags(final String url) {
        Map<String, Object> map = new HashMap<>();

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.setJavaScriptErrorListener(new DefaultJavaScriptErrorListener());
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.waitForBackgroundJavaScript(3000);

            List<String> tagList = new ArrayList<>();
            StringBuilder writers = new StringBuilder();

            HtmlPage page = webClient.getPage(new URL(url));

            String s = page.getBody().asXml();

            Document parse = Jsoup.parse(s);

            Elements tagElement = parse.getElementsByClass("tag_list");
            Elements wrterElement = parse.getElementsByClass("post_writer_info");

            if (tagElement.size() > 0) {
                Elements children = tagElement.get(0).children();

                for (Element e : children) {
                    tagList.add(e.text());
                }
            }

            if (wrterElement.size() > 0) {
                Elements children = wrterElement.get(0).children();

                for (Element e : children) {
                    Elements name = e.getElementsByClass("name");

                    if (name.size() > 0) {
                        writers.append(name.get(0).text()).append(",");
                    }
                }
            }
            
            map.put("writer", writers.toString().length() > 0? writers.substring(0, writers.toString().length() - 1) : "네이버");
            map.put("tagList", tagList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}