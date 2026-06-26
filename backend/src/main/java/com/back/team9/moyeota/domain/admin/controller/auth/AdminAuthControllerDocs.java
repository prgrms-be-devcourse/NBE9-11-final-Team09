package com.back.team9.moyeota.domain.admin.controller.auth;

import com.back.team9.moyeota.domain.admin.dto.auth.AdminLoginRequest;
import com.back.team9.moyeota.domain.admin.dto.auth.AdminLoginResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Auth", description = "관리자 인증 관련 API")
public interface AdminAuthControllerDocs {

    @Operation(
            summary = "관리자 로그인",
            description = "관리자 계정(이메일·비밀번호)으로 로그인합니다. 응답의 Access Token을 Authorization 헤더에 사용하세요."
    )
    ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @Valid @RequestBody AdminLoginRequest request
    );

    @Operation(
            summary = "관리자 로그아웃",
            description = "관리자 세션을 종료합니다. 현재 Access Token을 블랙리스트에 등록합니다."
    )
    ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request
    );
}
