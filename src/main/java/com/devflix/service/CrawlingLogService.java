package com.devflix.service;

import com.devflix.entity.CrawlingLog;
import com.devflix.repository.CrawlingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlingLogService {

    private final CrawlingLogRepository crawlingLogRepository;

    @Transactional
    public CrawlingLog createCrawlingSchedulerLog(final CrawlingLog crawlingSchedulerLog) {
        return crawlingLogRepository.save(crawlingSchedulerLog);
    }
}