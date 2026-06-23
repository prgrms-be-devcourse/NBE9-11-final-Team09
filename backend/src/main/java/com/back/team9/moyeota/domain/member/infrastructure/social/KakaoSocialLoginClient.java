package com.back.team9.moyeota.domain.member.infrastructure.social;

import com.back.team9.moyeota.domain.member.dto.auth.KakaoUserInfoResponse;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class KakaoSocialLoginClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${social.kakao.user-info-uri}")
    private String userInfoUri;

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        try {
            return restClientBuilder.build()
                    .get()
                    .uri(userInfoUri)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_SOCIAL_ACCESS_TOKEN
            );
        }
    }
}