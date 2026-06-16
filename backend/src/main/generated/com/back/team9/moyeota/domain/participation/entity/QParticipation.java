package com.back.team9.moyeota.domain.participation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QParticipation is a Querydsl query type for Participation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QParticipation extends EntityPathBase<Participation> {

    private static final long serialVersionUID = 1668400609L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QParticipation participation = new QParticipation("participation");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> finalAmount = createNumber("finalAmount", Integer.class);

    public final com.back.team9.moyeota.domain.funding.entity.QFunding funding;

    public final com.back.team9.moyeota.domain.member.entity.QMember member;

    public final com.back.team9.moyeota.domain.seat.entity.QSeat outboundSeat;

    public final NumberPath<Long> participationId = createNumber("participationId", Long.class);

    public final EnumPath<ParticipationPaymentStatus> paymentStatus = createEnum("paymentStatus", ParticipationPaymentStatus.class);

    public final com.back.team9.moyeota.domain.seat.entity.QSeat returnSeat;

    public final EnumPath<ParticipationStatus> status = createEnum("status", ParticipationStatus.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QParticipation(String variable) {
        this(Participation.class, forVariable(variable), INITS);
    }

    public QParticipation(Path<? extends Participation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QParticipation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QParticipation(PathMetadata metadata, PathInits inits) {
        this(Participation.class, metadata, inits);
    }

    public QParticipation(Class<? extends Participation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.funding = inits.isInitialized("funding") ? new com.back.team9.moyeota.domain.funding.entity.QFunding(forProperty("funding"), inits.get("funding")) : null;
        this.member = inits.isInitialized("member") ? new com.back.team9.moyeota.domain.member.entity.QMember(forProperty("member")) : null;
        this.outboundSeat = inits.isInitialized("outboundSeat") ? new com.back.team9.moyeota.domain.seat.entity.QSeat(forProperty("outboundSeat"), inits.get("outboundSeat")) : null;
        this.returnSeat = inits.isInitialized("returnSeat") ? new com.back.team9.moyeota.domain.seat.entity.QSeat(forProperty("returnSeat"), inits.get("returnSeat")) : null;
    }

}

