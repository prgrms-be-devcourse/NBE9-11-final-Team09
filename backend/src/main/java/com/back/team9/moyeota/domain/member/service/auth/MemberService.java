package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.EmailVerificationConfirmRequest;
import com.back.team9.moyeota.domain.member.dto.auth.EmailVerificationRequest;
import com.back.team9.moyeota.domain.member.dto.auth.MemberSignupRequest;
import com.back.team9.moyeota.domain.member.infrastructure.redis.EmailVerificationData;
import com.back.team9.moyeota.domain.member.infrastructure.redis.EmailVerificationRedisRepository;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.domain.member.infrastructure.redis.PendingSignupData;
import com.back.team9.moyeota.domain.member.infrastructure.redis.PendingSignupRedisRepository;
import com.back.team9.moyeota.domain.member.infrastructure.redis.EmailVerificationRateLimitRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.regex.Pattern;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final int VERIFICATION_CODE_LENGTH = 6;

    private static final int MAX_VERIFICATION_ATTEMPTS = 5;

    private final EmailVerificationCodeHasher verificationCodeHasher;

    private static final String CODE_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    private static final Pattern PHONE_NUMBER_PATTERN =
            Pattern.compile("^010-\\d{4}-\\d{4}$");

    private final MemberRepository memberRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final PendingSignupRedisRepository pendingSignupRepository;
    private final EmailVerificationRedisRepository verificationRepository;
    private final MemberRegistrationService memberRegistrationService;
    private final EmailVerificationRateLimitRedisRepository rateLimitRepository;

    public void requestSignup(MemberSignupRequest request) {
        String email = normalizeEmail(request.email());

        log.info("회원가입 요청 저장 시작 (email={})", maskEmail(email));

        validateSignupRequest(request, email);
        validateMemberDuplicates(email, request.nickname());

        PendingSignupData signupData = new PendingSignupData(
                email,
                passwordEncoder.encode(request.password()),
                request.name(),
                request.nickname(),
                request.phoneNumber()
        );

        pendingSignupRepository.save(signupData);

        log.info("회원가입 요청 저장 완료 (email={})", maskEmail(email));
    }

    public void requestEmailVerification(
            EmailVerificationRequest request
    ) {
        String email = normalizeEmail(request.email());

        log.info("이메일 인증 요청 (email={})", maskEmail(email));

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            log.warn("이메일 인증 요청 실패 - 이메일 형식 오류 (email={})", maskEmail(email));
            throw new BusinessException(
                    ErrorCode.INVALID_EMAIL_FORMAT
            );
        }

        PendingSignupData signupData = pendingSignupRepository
                .findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SIGNUP_REQUEST_NOT_FOUND
                ));

        validateMemberDuplicates(
                signupData.email(),
                signupData.nickname()
        );

        pendingSignupRepository.save(signupData);

        String verificationCode = generateVerificationCode();

        EmailVerificationData verificationData =
                new EmailVerificationData(
                        verificationCodeHasher.hash(verificationCode)
                );

        if (!rateLimitRepository.tryLockRequest(email)) {
            log.warn("이메일 인증 요청 제한 초과 (email={})", maskEmail(email));
            throw new BusinessException(
                    ErrorCode.EMAIL_VERIFICATION_REQUEST_TOO_FREQUENT
            );
        }

        try {
            verificationRepository.save(email, verificationData);

            emailVerificationService.sendVerificationCode(
                    email,
                    verificationCode
            );
            log.info("이메일 인증 코드 발송 완료 (email={})", maskEmail(email));
        } catch (RuntimeException exception) {
            log.error("이메일 인증 메일 발송 실패 (email={})",
                    maskEmail(email), exception);
            safelyDeleteVerification(email);
            safelyDeleteRateLimit(email);
            throw exception;
        }
    }

    public void confirmEmailVerification(
            EmailVerificationConfirmRequest request
    ) {
        String email = normalizeEmail(request.email());

        log.info("이메일 인증 확인 요청 (email={})", maskEmail(email));

        PendingSignupData signupData = pendingSignupRepository
                .findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SIGNUP_REQUEST_NOT_FOUND
                ));

        EmailVerificationData verificationData =
                verificationRepository.findByEmail(email)
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.VERIFICATION_CODE_EXPIRED
                        ));

        if (!verificationCodeHasher.matches(
                request.verificationCode(),
                verificationData.verificationCodeHash()
        )) {
            long failedAttempts =
                    verificationRepository.incrementFailedAttempts(email);
            log.warn("이메일 인증 코드 불일치 (email={}, failedAttempts={})",
                    maskEmail(email), failedAttempts);

            if (failedAttempts >= MAX_VERIFICATION_ATTEMPTS) {
                safelyDeleteVerification(email);
                log.warn("이메일 인증 시도 횟수 초과 (email={})", maskEmail(email));

                throw new BusinessException(
                        ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED
                );
            }

            throw new BusinessException(
                    ErrorCode.INVALID_VERIFICATION_CODE
            );
        }

        memberRegistrationService.register(signupData);
        log.info("이메일 인증 완료 및 회원가입 확정 (email={})", maskEmail(email));

        safelyDeleteVerification(email);
        safelyDeletePendingSignup(email);
        safelyDeleteRateLimit(email);
    }

    private void validateSignupRequest(
            MemberSignupRequest request,
            String normalizedEmail
    ) {
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            log.warn("회원가입 실패 - 이메일 형식 오류 (email={})",
                    maskEmail(normalizedEmail));
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (!PASSWORD_PATTERN.matcher(request.password()).matches()) {
            log.warn("회원가입 실패 - 비밀번호 형식 오류 (email={})",
                    maskEmail(normalizedEmail));
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        if (!PHONE_NUMBER_PATTERN.matcher(request.phoneNumber()).matches()) {
            log.warn("회원가입 실패 - 전화번호 형식 오류 (email={})",
                    maskEmail(normalizedEmail));
            throw new BusinessException(
                    ErrorCode.INVALID_PHONE_NUMBER_FORMAT
            );
        }
    }

    private void validateMemberDuplicates(String email, String nickname) {
        if (memberRepository.existsByEmail(email)) {
            log.warn("회원가입 실패 - 이메일 중복 (email={})",
                    maskEmail(email));
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (memberRepository.existsByNickname(nickname)) {
            log.warn("회원가입 실패 - 닉네임 중복 (nickname={})",
                    nickname);
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(VERIFICATION_CODE_LENGTH);

        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            int index = random.nextInt(CODE_CHARACTERS.length());
            code.append(CODE_CHARACTERS.charAt(index));
        }

        return code.toString();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String maskEmail(String email) {
        if (email == null) {
            return "null";
        }
        return email.replaceAll("(?<=.{3}).(?=.*@)", "*");
    }

    private void safelyDeletePendingSignup(String email) {
        try {
            pendingSignupRepository.deleteByEmail(email);
        } catch (BusinessException exception) {
            // TTL에 의해 최종 삭제되므로 회원가입 성공을 실패로 바꾸지 않는다.
            log.warn("회원가입 대기 정보 Redis 삭제 실패 (email={})",
                    maskEmail(email), exception);
        }
    }

    private void safelyDeleteVerification(String email) {
        try {
            verificationRepository.deleteByEmail(email);
        } catch (BusinessException exception) {
            log.warn("이메일 인증 정보 Redis 삭제 실패 (email={})",
                    maskEmail(email), exception);
        }
    }

    private void safelyDeleteRateLimit(String email) {
        try {
            rateLimitRepository.deleteRequestLock(email);
        } catch (BusinessException exception) {
            log.warn("이메일 인증 요청 제한 정보 Redis 삭제 실패 (email={})",
                    maskEmail(email), exception);
        }
    }
}
