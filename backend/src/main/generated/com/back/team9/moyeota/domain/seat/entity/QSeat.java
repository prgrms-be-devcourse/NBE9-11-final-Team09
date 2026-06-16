package com.back.team9.moyeota.domain.seat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSeat is a Querydsl query type for Seat
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSeat extends EntityPathBase<Seat> {

    private static final long serialVersionUID = 743831397L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSeat seat = new QSeat("seat");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final com.back.team9.moyeota.domain.participation.entity.QParticipation participation;

    public final com.back.team9.moyeota.domain.pathinfo.entity.QPathinfo pathinfo;

    public final NumberPath<Long> seatId = createNumber("seatId", Long.class);

    public final StringPath seatNumber = createString("seatNumber");

    public final EnumPath<SeatStatus> status = createEnum("status", SeatStatus.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QSeat(String variable) {
        this(Seat.class, forVariable(variable), INITS);
    }

    public QSeat(Path<? extends Seat> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSeat(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSeat(PathMetadata metadata, PathInits inits) {
        this(Seat.class, metadata, inits);
    }

    public QSeat(Class<? extends Seat> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.participation = inits.isInitialized("participation") ? new com.back.team9.moyeota.domain.participation.entity.QParticipation(forProperty("participation"), inits.get("participation")) : null;
        this.pathinfo = inits.isInitialized("pathinfo") ? new com.back.team9.moyeota.domain.pathinfo.entity.QPathinfo(forProperty("pathinfo"), inits.get("pathinfo")) : null;
    }

}

