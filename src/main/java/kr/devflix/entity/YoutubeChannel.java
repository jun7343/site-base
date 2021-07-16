package kr.devflix.entity;

import kr.devflix.constant.Status;
import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "youtube_channel")
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YoutubeChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column
    private String category;

    @Column(name = "channel_id", unique = true, updatable = false)
    private String channelId;

    @Column(name = "channel_title")
    private String channelTitle;

    @Column
    private String etag;

    @Column
    private String description;

    @Column
    private String thumbnail;

    @Column(name = "publish_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date publishAt;

    @Column(name = "crawling_at")
    private long crawlingAt;

    @Column(name = "create_at", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createAt;

    @Column(name = "update_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateAt;

    @Builder
    public YoutubeChannel(final Long id, Status status, final String channelId, final String channelTitle, final String category,
                          final String etag, final String description, final String thumbnail,
                          final Date publishAt, final long crawlingAt, final Date createAt, final Date updateAt) {
        this.id = id;
        this.status = status;
        this.category = category;
        this.channelId = channelId;
        this.channelTitle = channelTitle;
        this.etag = etag;
        this.description = description;
        this.thumbnail = thumbnail;
        this.publishAt = publishAt;
        this.crawlingAt = crawlingAt;
        this.createAt = createAt;
        this.updateAt = updateAt;
    }
}
