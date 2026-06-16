package com.back.team9.moyeota.domain.funding.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFunding is a Querydsl query type for Funding
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFunding extends EntityPathBase<Funding> {

    private static final long serialVersionUID = -675937055L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFunding funding = new QFunding("funding");

    public final EnumPath<BusType> busType = createEnum("busType", BusType.class);

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DatePath<java.time.LocalDate> departureDate = createDate("departureDate", java.time.LocalDate.class);

    public final NumberPath<Long> fundingId = createNumber("fundingId", Long.class);

    public final NumberPath<Integer> maxParticipants = createNumber("maxParticipants", Integer.class);

    public final com.back.team9.moyeota.domain.member.entity.QMember member;

    public final NumberPath<Integer> minParticipants = createNumber("minParticipants", Integer.class);

    public final BooleanPath paybackHold = createBoolean("paybackHold");

    public final EnumPath<FundingStatus> status = createEnum("status", FundingStatus.class);

    public final StringPath title = createString("title");

    public final NumberPath<Integer> totalPrice = createNumber("totalPrice", Integer.class);

    public final EnumPath<TripType> tripType = createEnum("tripType", TripType.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QFunding(String variable) {
        this(Funding.class, forVariable(variable), INITS);
    }

    public QFunding(Path<? extends Funding> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFunding(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFunding(PathMetadata metadata, PathInits inits) {
        this(Funding.class, metadata, inits);
    }

    public QFunding(Class<? extends Funding> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.back.team9.moyeota.domain.member.entity.QMember(forProperty("member")) : null;
    }

}

