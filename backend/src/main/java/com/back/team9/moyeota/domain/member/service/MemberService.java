package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.domain.member.dto.EmailVerificationConfirmRequest;
import com.back.team9.moyeota.domain.member.dto.MemberSignupRequest;
import com.back.team9.moyeota.domain.member.entity.PendingMemberSignup;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.member.repository.PendingMemberSignupRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_EXPIRATION_MINUTES = 30;

    private static final String CODE_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    private static final Pattern PHONE_NUMBER_PATTERN =
            Pattern.compile("^010-\\d{4}-\\d{4}$");

    private final MemberRepository memberRepository;
    private final PendingMemberSignupRepository pendingSignupRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final PendingMemberSignupService pendingMemberSignupService;


    public void requestSignup(MemberSignupRequest request) {
        validateSignupRequest(request);
        validateMemberDuplicates(request.email(), request.nickname());

        String verificationCode = generateVerificationCode();
        String encodedPassword = passwordEncoder.encode(request.password());
        String verificationCodeHash = passwordEncoder.encode(verificationCode);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(VERIFICATION_EXPIRATION_MINUTES);

        pendingMemberSignupService.saveOrUpdate(
                request,
                encodedPassword,
                verificationCodeHash,
                expiresAt
        );

        emailVerificationService.sendVerificationCode(
                request.email(),
                verificationCode
        );
    }

    @Transactional
    public void confirmEmailVerification(
            EmailVerificationConfirmRequest request
    ) {
        PendingMemberSignup pendingSignup = pendingSignupRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.VERIFICATION_CODE_EXPIRED
                ));

        pendingSignup.validateVerificationCode(
                request.verificationCode(),
                passwordEncoder
        );

        validateMemberDuplicates(
                pendingSignup.getEmail(),
                pendingSignup.getNickname()
        );

        memberRepository.save(pendingSignup.toMember());
        pendingSignupRepository.delete(pendingSignup);
    }

    private void validateSignupRequest(MemberSignupRequest request) {
        if (!EMAIL_PATTERN.matcher(request.email()).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (!PASSWORD_PATTERN.matcher(request.password()).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        if (!PHONE_NUMBER_PATTERN.matcher(request.phoneNumber()).matches()) {
            throw new BusinessException(
                    ErrorCode.INVALID_PHONE_NUMBER_FORMAT
            );
        }
    }

    private void validateMemberDuplicates(String email, String nickname) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (memberRepository.existsByNickname(nickname)) {
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
}