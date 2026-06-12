package com.back.team9.moyeota.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // [COM] 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST,"COM001", "잘못된 입력값입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED,"COM002", "인증이 필요합니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN,"COM003", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"COM004", "서버 내부 오류가 발생했습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,"COM005", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED,"COM006", "유효하지 않은 토큰입니다."),

    // [USR] 회원
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"USR001", "존재하지 않는 사용자입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT,"USR002", "이미 가입된 이메일입니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST,"USR003", "이메일 형식이 올바르지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN,"USR004", "이메일 인증이 완료되지 않았습니다."),
    USER_ALREADY_WITHDRAWN(HttpStatus.FORBIDDEN,"USR005", "탈퇴한 회원입니다."),
    SOCIAL_LOGIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"USR006", "소셜 로그인 처리 중 오류가 발생했습니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USR007", "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "USR008", "비밀번호는 영문+숫자+특수문자 8자리 이상이어야 합니다."),
    INVALID_PHONE_NUMBER_FORMAT(HttpStatus.BAD_REQUEST, "USR009", "전화번호 형식이 올바르지 않습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "USR010", "인증코드가 올바르지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.GONE, "USR011", "인증코드가 만료되었습니다."),

    // [ADM] 관리자
    ADMIN_PERMISSION_REQUIRED(HttpStatus.FORBIDDEN,"ADM001", "관리자 권한이 필요합니다."),
    DUPLICATE_ADMIN_ACCOUNT(HttpStatus.CONFLICT,"ADM002", "이미 존재하는 관리자 계정입니다."),
    ADMIN_TARGET_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "ADM003", "이미 탈퇴 처리된 회원입니다."),
    USER_ALREADY_SUSPENDED(HttpStatus.BAD_REQUEST,"ADM004", "이미 정지된 회원입니다."),
    INVALID_USER_STATUS_CHANGE(HttpStatus.BAD_REQUEST,"ADM005", "올바르지 않은 회원 상태 변경 요청입니다."),

    // [FND] 펀딩
    FUNDING_NOT_FOUND(HttpStatus.NOT_FOUND,"FND001", "존재하지 않는 펀딩입니다."),
    FUNDING_NOT_RECRUITING(HttpStatus.BAD_REQUEST,"FND002", "현재 모집 중인 펀딩이 아닙니다."),
    FUNDING_MIN_MAX_INVALID(HttpStatus.BAD_REQUEST,"FND003", "최소 인원을 최대 인원보다 높게 설정할 수 없습니다."),
    FUNDING_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST,"FND004", "이미 취소/삭제된 펀딩입니다."),

    // [PTH] 운행노선
    SAME_DEPARTURE_ARRIVAL(HttpStatus.BAD_REQUEST,"PTH001", "출발지와 도착지가 동일할 수 없습니다."),
    PATH_NOT_FOUND(HttpStatus.NOT_FOUND,"PTH002", "존재하지 않는 노선입니다."),
    PATH_INVALID_STATUS(HttpStatus.BAD_REQUEST,"PTH003", "운행이 마감되거나 취소된 노선입니다."),

    // [PTC] 참여
    PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND,"PTC001", "탑승 참여 내역을 찾을 수 없습니다."),
    DUPLICATE_PARTICIPATION(HttpStatus.CONFLICT,"PTC002", "이미 해당 펀딩에 참여 중입니다."),
    FUNDING_RECRUITMENT_CLOSED(HttpStatus.BAD_REQUEST,"PTC003", "모집이 마감된 펀딩입니다."),
    FUNDING_CANCELLED(HttpStatus.BAD_REQUEST, "PTC004", "취소된 펀딩입니다."),


    // [SEA] 좌석
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND,"SEA001", "존재하지 않는 좌석입니다."),
    SEAT_ALREADY_OCCUPIED(HttpStatus.CONFLICT,"SEA002", "이미 선택된 좌석입니다. 새로고침 후 다시 시도해주세요."),
    SEAT_NOT_IN_PATH(HttpStatus.BAD_REQUEST,"SEA003", "해당 노선에 속하지 않는 좌석입니다."),
    ROUND_TRIP_SEAT_REQUIRED(HttpStatus.BAD_REQUEST,"SEA004", "왕복 펀딩은 가는 편과 오는 편 좌석을 모두 선택해야합니다."),
    ONE_WAY_RETURN_SEAT_NOT_ALLOWED(HttpStatus.BAD_REQUEST,"SEA005", "편도 펀딩은 오는 편 좌석을 선택할 수 없습니다."),

    // [PAY] 결제
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST,"PAY001", "결제 요청 금액이 일치하지 않습니다."),
    TOSS_PAYMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"PAY002", "토스페이먼츠 결제 승인에 실패했습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT,"PAY003", "환불 가능한 상태가 아닙니다."),
    DEPOSIT_NOT_PAID(HttpStatus.BAD_REQUEST,"PAY004", "보증금 미결제 상태입니다."),
    REFUND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"PAY005", "환불 처리 중 오류가 발생했습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND,"PAY006", "존재하지 않는 주문입니다."),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT,"PAY007", "중복 결제 요청입니다."),
    BALANCE_PAYMENT_EXPIRED(HttpStatus.BAD_REQUEST,"PAY008", "잔액 결제 기한이 만료되었습니다."),
    ALREADY_REFUNDED(HttpStatus.CONFLICT, "PAY009", "이미 환불된 결제입니다."),

    // [STL] 정산
    SETTLEMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST,"STL001", "이미 방장 페이백이 완료된 펀딩입니다."),
    SETTLEMENT_ON_HOLD(HttpStatus.FORBIDDEN,"STL002", "정산이 보류 중인 펀딩입니다."),
    SETTLEMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN,"STL003", "정산 내역 조회 권한이 없습니다."),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND,"STL004", "정산 내역이 존재하지 않습니다."),
    SETTLEMENT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST,"STL005", "정산 가능한 상태가 아닙니다."),

    // [NTF] 알림
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"NTF001", "알림 발송에 실패했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"NTF002", "이메일 발송 중 오류가 발생했습니다."),

    // [CHT] 채팅
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND,"CHT001", "존재하지 않는 채팅방입니다."),
    CHAT_ROOM_ALREADY_DELETED(HttpStatus.BAD_REQUEST,"CHT002", "이미 삭제된 채팅방입니다."),
    MESSAGE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"CHT003", "메시지 전송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}