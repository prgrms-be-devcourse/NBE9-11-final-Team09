package com.back.team9.moyeota.domain.pathinfo.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPathinfo is a Querydsl query type for Pathinfo
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPathinfo extends EntityPathBase<Pathinfo> {

    private static final long serialVersionUID = 166706433L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPathinfo pathinfo = new QPathinfo("pathinfo");

    public final StringPath arrivalAddress = createString("arrivalAddress");

    public final EnumPath<Region> arrivalRegion = createEnum("arrivalRegion", Region.class);

    public final EnumPath<com.back.team9.moyeota.domain.funding.entity.BusType> busType = createEnum("busType", com.back.team9.moyeota.domain.funding.entity.BusType.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath departureAddress = createString("departureAddress");

    public final EnumPath<Region> departureRegion = createEnum("departureRegion", Region.class);

    public final DateTimePath<java.time.LocalDateTime> departureTime = createDateTime("departureTime", java.time.LocalDateTime.class);

    public final EnumPath<Direction> direction = createEnum("direction", Direction.class);

    public final com.back.team9.moyeota.domain.funding.entity.QFunding funding;

    public final NumberPath<Long> pathinfoId = createNumber("pathinfoId", Long.class);

    public final EnumPath<PathinfoStatus> status = createEnum("status", PathinfoStatus.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QPathinfo(String variable) {
        this(Pathinfo.class, forVariable(variable), INITS);
    }

    public QPathinfo(Path<? extends Pathinfo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPathinfo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPathinfo(PathMetadata metadata, PathInits inits) {
        this(Pathinfo.class, metadata, inits);
    }

    public QPathinfo(Class<? extends Pathinfo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.funding = inits.isInitialized("funding") ? new com.back.team9.moyeota.domain.funding.entity.QFunding(forProperty("funding"), inits.get("funding")) : null;
    }

}

