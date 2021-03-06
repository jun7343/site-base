package kr.devflix.admin.controller;

import kr.devflix.constant.RoleType;
import kr.devflix.entity.CrawlingLog;
import kr.devflix.service.CrawlingLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@Secured(RoleType.MANAGER)
public class AdminCrawlingLogController {

    private final CrawlingLogService crawlingLogService;
    private final int DEFAULT_PER_PAGE_SIZE = 20;

    public AdminCrawlingLogController(CrawlingLogService crawlingLogService) {
        this.crawlingLogService = crawlingLogService;
    }

    @RequestMapping(path = "/dfa/crawling-log", method = RequestMethod.GET)
    public String crawlingLogList(@RequestParam(name = "page", required = false, defaultValue = "0")final int page,
                                  @RequestParam(name = "job-name", required = false)final String jobName,
                                  Model model) {
        Page<CrawlingLog> findList = null;

        if (StringUtils.isBlank(jobName)) {
            findList = crawlingLogService.findAll(page, DEFAULT_PER_PAGE_SIZE);
        } else {
            findList = crawlingLogService.findAllBySearch(jobName, page, DEFAULT_PER_PAGE_SIZE);
        }

        model.addAttribute("search-job-name", jobName);

        List<Integer> pageNumList = new ArrayList<>();

        model.addAttribute("list", findList);
        model.addAttribute("page", page);

        if (findList.getNumber() / 5 != 0 && ((findList.getNumber() / 5) * 5 - 1) > 0) {
            model.addAttribute("previousPageNum", (findList.getNumber() / 5) * 5 - 1);
        }

        if ((findList.getNumber() / 5) * 5 + 6 <= findList.getTotalPages()) {
            model.addAttribute("nextPageNum", (findList.getNumber() / 5 + 1) * 5);
        }

        int start = (findList.getNumber() / 5) * 5 + 1;
        int end = Math.min((findList.getNumber() / 5 + 1) * 5, findList.getTotalPages());

        for (int i = start; i <= end; i++) {
            pageNumList.add(i);
        }

        model.addAttribute("pageNumList", pageNumList);
        model.addAttribute("currentPageNum", findList.getNumber() + 1);
        model.addAttribute("pagination", findList.getTotalPages() > 1);

        return "/admin/crawling-log/list";
    }
}
