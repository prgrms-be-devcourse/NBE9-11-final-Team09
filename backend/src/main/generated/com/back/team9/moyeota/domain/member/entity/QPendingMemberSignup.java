package com.back.team9.moyeota.domain.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPendingMemberSignup is a Querydsl query type for PendingMemberSignup
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPendingMemberSignup extends EntityPathBase<PendingMemberSignup> {

    private static final long serialVersionUID = -1659744268L;

    public static final QPendingMemberSignup pendingMemberSignup = new QPendingMemberSignup("pendingMemberSignup");

    public final StringPath email = createString("email");

    public final StringPath encodedPassword = createString("encodedPassword");

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final NumberPath<Long> pendingMemberSignupId = createNumber("pendingMemberSignupId", Long.class);

    public final StringPath phoneNumber = createString("phoneNumber");

    public final StringPath verificationCodeHash = createString("verificationCodeHash");

    public QPendingMemberSignup(String variable) {
        super(PendingMemberSignup.class, forVariable(variable));
    }

    public QPendingMemberSignup(Path<? extends PendingMemberSignup> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPendingMemberSignup(PathMetadata metadata) {
        super(PendingMemberSignup.class, metadata);
    }

}

