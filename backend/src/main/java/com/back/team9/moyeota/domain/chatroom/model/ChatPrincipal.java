package com.back.team9.moyeota.domain.chatroom.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

@Getter
@RequiredArgsConstructor
public class ChatPrincipal implements Principal {

    private final Long memberId;

    @Override
    public String getName() {
        return memberId.toString();
    }
}