package com.back.team9.moyeota.global.jwt;

import com.back.team9.moyeota.domain.chatroom.model.ChatPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenResolver jwtTokenResolver;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            jwtTokenResolver.findToken(authHeader)
                    .flatMap(token ->
                            jwtTokenProvider.findMemberIdFromAccessToken(token)
                                    .filter(memberId ->
                                            !jwtBlacklistService.isBlacklisted(
                                                    jwtTokenProvider.getJti(token)
                                            )
                                    )
                                    .map(memberId -> {
                                        ChatPrincipal principal = new ChatPrincipal(memberId);

                                        return new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                List.of()
                                        );
                                    })
                    )
                    .ifPresentOrElse(
                            accessor::setUser,
                            () -> { throw new AccessDeniedException("유효하지 않은 토큰입니다."); }
                    );
        }

        return message;
    }
}