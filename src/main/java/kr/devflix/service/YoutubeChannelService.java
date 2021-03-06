package kr.devflix.service;

import kr.devflix.constant.Status;
import kr.devflix.entity.YoutubeChannel;
import kr.devflix.repository.YoutubeChennelRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class YoutubeChannelService {

    private final YoutubeChennelRepository youtubeChennelRepository;

    public YoutubeChannelService(YoutubeChennelRepository youtubeChennelRepository) {
        this.youtubeChennelRepository = youtubeChennelRepository;
    }

    @Transactional
    public YoutubeChannel createYoutubeChannel(final YoutubeChannel youtubeChannel) {
        return youtubeChennelRepository.save(youtubeChannel);
    }

    @Transactional
    public List<YoutubeChannel> findAllOrderByCrawlingAtAsc() {
        return youtubeChennelRepository.findAll(Sort.by(Sort.Order.asc("crawlingAt")));
    }

    @Transactional
    public void updateYoutubeChannel(final YoutubeChannel channel) {
        youtubeChennelRepository.save(channel);
    }

    @Transactional
    public List<YoutubeChannel> findAllByStatusOrderByCreateAtDesc(Status status) {
        return youtubeChennelRepository.findAllByStatusOrderByCreateAtDesc(status);
    }

    @Transactional
    public List<YoutubeChannel> findAll() {
        return youtubeChennelRepository.findAll(Sort.by(Sort.Order.desc("createAt")));
    }

    @Transactional
    public Optional<YoutubeChannel> findOneById(long id) {
        return youtubeChennelRepository.findById(id);
    }
}
