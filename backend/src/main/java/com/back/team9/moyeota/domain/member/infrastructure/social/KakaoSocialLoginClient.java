package com.back.team9.moyeota.domain.member.infrastructure.social;

import com.back.team9.moyeota.domain.member.dto.auth.KakaoUserInfoResponse;
import com.back.team9.moyeota.domain.member.dto.auth.KakaoTokenResponse;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Component
public class KakaoSocialLoginClient {

    @Value("${social.kakao.client-id}")
    private String clientId;

    @Value("${social.kakao.token-uri}")
    private String tokenUri;

    private final RestClient restClient;

    @Value("${social.kakao.user-info-uri}")
    private String userInfoUri;

    @Value("${social.kakao.client-secret:}")
    private String clientSecret;

    public KakaoSocialLoginClient() {
        this.restClient = RestClient.builder().build();
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        try {
            return restClient
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

    public KakaoTokenResponse getToken(
            String code,
            String redirectUri
    ) {
        try {
            MultiValueMap<String, String> formData =
                    new LinkedMultiValueMap<>();

            formData.add("grant_type", "authorization_code");
            formData.add("client_id", clientId);
            formData.add("redirect_uri", redirectUri);
            formData.add("code", code);

            if (StringUtils.hasText(clientSecret)) {
                formData.add("client_secret", clientSecret);
            }

            return restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_SOCIAL_ACCESS_TOKEN
            );
        }
    }
}