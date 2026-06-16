package com.back.team9.moyeota.domain.settlement.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSettlement is a Querydsl query type for Settlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSettlement extends EntityPathBase<Settlement> {

    private static final long serialVersionUID = 36761133L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSettlement settlement = new QSettlement("settlement");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final com.back.team9.moyeota.domain.funding.entity.QFunding funding;

    public final NumberPath<Integer> hostPaybackAmount = createNumber("hostPaybackAmount", Integer.class);

    public final com.back.team9.moyeota.domain.member.entity.QMember member;

    public final BooleanPath paybackHold = createBoolean("paybackHold");

    public final DateTimePath<java.time.LocalDateTime> paybackPaidAt = createDateTime("paybackPaidAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> platformFee = createNumber("platformFee", Integer.class);

    public final NumberPath<Long> settlementId = createNumber("settlementId", Long.class);

    public final EnumPath<SettlementStatus> status = createEnum("status", SettlementStatus.class);

    public final NumberPath<Integer> totalAmount = createNumber("totalAmount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QSettlement(String variable) {
        this(Settlement.class, forVariable(variable), INITS);
    }

    public QSettlement(Path<? extends Settlement> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSettlement(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSettlement(PathMetadata metadata, PathInits inits) {
        this(Settlement.class, metadata, inits);
    }

    public QSettlement(Class<? extends Settlement> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.funding = inits.isInitialized("funding") ? new com.back.team9.moyeota.domain.funding.entity.QFunding(forProperty("funding"), inits.get("funding")) : null;
        this.member = inits.isInitialized("member") ? new com.back.team9.moyeota.domain.member.entity.QMember(forProperty("member")) : null;
    }

}

