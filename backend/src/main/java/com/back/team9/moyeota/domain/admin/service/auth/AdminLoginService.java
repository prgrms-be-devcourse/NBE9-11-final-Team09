package com.back.team9.moyeota.domain.admin.service.auth;

import com.back.team9.moyeota.domain.admin.dto.auth.AdminLoginRequest;
import com.back.team9.moyeota.domain.admin.dto.auth.AdminLoginResponse;
import com.back.team9.moyeota.domain.admin.entity.Admin;
import com.back.team9.moyeota.domain.admin.entity.AdminStatus;
import com.back.team9.moyeota.domain.admin.repository.auth.AdminRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.dto.JwtAccessTokenResponse;
import com.back.team9.moyeota.global.jwt.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLoginService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ADMIN_INVALID_LOGIN_CREDENTIALS
                ));

        if (admin.getPassword() == null
                || !passwordEncoder.matches(
                request.password(),
                admin.getPassword()
        )) {
            throw new BusinessException(
                    ErrorCode.ADMIN_INVALID_LOGIN_CREDENTIALS
            );
        }

        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.ADMIN_SUSPENDED
            );
        }

        if (admin.getRole() == null) {
            throw new BusinessException(
                    ErrorCode.ADMIN_PERMISSION_REQUIRED
            );
        }

        JwtAccessTokenResponse token =
                jwtTokenProvider.createAdminAccessToken(
                        admin.getAdminId(),
                        admin.getRole().name()
                );

        return AdminLoginResponse.from(admin, token);
    }
}
