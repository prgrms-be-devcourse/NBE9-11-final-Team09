package com.back.team9.moyeota.domain.admin.controller.auth;

import com.back.team9.moyeota.domain.admin.dto.auth.AdminLoginRequest;
import com.back.team9.moyeota.domain.admin.dto.auth.AdminLoginResponse;
import com.back.team9.moyeota.domain.admin.service.auth.AdminLoginService;
import com.back.team9.moyeota.domain.admin.service.auth.AdminLogoutService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController implements AdminAuthControllerDocs {

    private final AdminLoginService adminLoginService;
    private final AdminLogoutService adminLogoutService;
    private final JwtTokenResolver jwtTokenResolver;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "ADMIN_LOGIN_SUCCESS",
                "관리자 로그인에 성공했습니다.",
                adminLoginService.login(request)
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request
    ) {
        String accessToken = jwtTokenResolver.findToken(request)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.TOKEN_INVALID)
                );

        adminLogoutService.logout(accessToken);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "ADMIN_LOGOUT_SUCCESS",
                        "관리자 로그아웃에 성공했습니다.",
                        null
                )
        );
    }
}
