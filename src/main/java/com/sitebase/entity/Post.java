package com.sitebase.entity;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(targetEntity = Member.class)
    private Member writer;

    @Column
    private String title;

    @Column
    private String content;

    @Column(name = "created_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Column(name = "updated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    @Builder
    public Post(String title, Member writer, String content, Date createdDate, Date updatedDate) {
        this.title = title;
        this.content = content;
        this.writer = writer;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }
}
