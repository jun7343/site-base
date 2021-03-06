package kr.devflix.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.devflix.constant.Status;
import kr.devflix.entity.DevPost;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static kr.devflix.entity.QDevPost.devPost;
import static kr.devflix.entity.QDevPostTag.devPostTag;

public class DevPostRepositoryImpl implements DevPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public DevPostRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<DevPost> findAllByCategoryOrTagOrLikeTitleAndStatusLimitOffset(final String category,
                                                                               final String tag,
                                                                               final String title,
                                                                               Status status,
                                                                               final int page,
                                                                               final int perPage) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.isNoneBlank(category)) {
            builder.or(devPost.category.eq(category));
        }

        if (StringUtils.isNoneBlank(tag)) {
            builder.or(devPost.tags.any().tag.eq(tag));
        }

        if (StringUtils.isNoneBlank(title)) {
            builder.or(devPost.title.containsIgnoreCase(title));
        }

        builder.and(devPost.status.eq(status));

        return queryFactory.selectFrom(devPost)
                .leftJoin(devPost.tags, devPostTag)
                .fetchJoin()
                .where(builder)
                .orderBy(devPost.uploadAt.desc())
                .limit(perPage)
                .offset(page * perPage)
                .fetch();
    }

    @Override
    public Long countByCategoryOrTagOrListTitleAndStatus(final String category,
                                                    final String tag,
                                                    final String title,
                                                    Status status) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.isNoneBlank(category)) {
            builder.or(devPost.category.eq(category));
        }

        if (StringUtils.isNoneBlank(tag)) {
            builder.or(devPost.tags.any().tag.eq(tag));
        }

        if (StringUtils.isNoneBlank(title)) {
            builder.or(devPost.title.like("%" + title + "%"));
        }

        builder.and(devPost.status.eq(status));

        return queryFactory.selectFrom(devPost)
                .where(builder)
                .fetchCount();
    }

    @Override
    public Long updateViewById(Long id) {
        return queryFactory.update(devPost)
                .set(devPost.view, devPost.view.add(1))
                .where(devPost.id.eq(id))
                .execute();
    }
}
