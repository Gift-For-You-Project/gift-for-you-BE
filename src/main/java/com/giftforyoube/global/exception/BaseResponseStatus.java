package com.giftforyoube.global.exception;

import lombok.Getter;

@Getter
public enum BaseResponseStatus {

    /**
     * 1. 요청이 성공한 경우(2000)
     */

    // 0. 공통
    SUCCESS(true, 2000, "요청에 성공하였습니다."),

    // 1-1. 회원가입 / 로그인 / 회원탈퇴 관련
    REGISTER_ACCOUNT_SUCCESS(true, 2000, "회원가입이 완료되었습니다"),
    LOGIN_SUCCESS(true, 2000, "로그인이 완료되었습니다."),
    KAKAO_LOGIN_SUCCESS(true, 2000, "카카오 로그인이 완료되었습니다."),
    GOOGLE_LOGIN_SUCCESS(true, 2000, "구글 로그인이 완료되었습니다."),
    DELETE_ACCOUNT_SUCCESS(true, 2000, "회원탈퇴가 완료되었습니다."),

    /**
     * 2. 클라이언트 에러가 발생한 경우(4000)
     */

    // 0. 공통
    BAD_REQUEST(false, 4000, "잘못된 요청입니다."),

    // 2-1. 회원가입 / 로그인 / 회원탈퇴 관련
    USER_NOT_FOUND(false, 4000, "가입된 사용자 정보가 없습니다."),
    PASSWORD_MISMATCH(false, 4000, "비밀번호가 일치하지 않습니다."),
    EMAIL_ALREADY_EXISTS(false, 4000, "이미 가입된 이메일입니다."),
    PHONENUMBER_ALREADY_EXISTS(false, 4000, "이미 가입된 휴대폰 번호입니다."),
    LOGIN_FAILURE(false, 4000, "로그인에 실패했습니다."),
    REGISTOR_ACCOUNT_FAILURE(false, 4000, "회원가입에 실패했습니다."),
    DELETE_ACCOUNT_FAILURE(false, 4000, "회원탈퇴에 실패했습니다."),

    // 2-2. 인증 및 인가 관련
    AUTHENTICATION_FAILED(false, 4000, "인증에 실패했습니다"),
    INVALID_TOKEN(false, 4000, "JWT 토큰이 유효하지 않습니다."),
    NOT_FOUND_TOKEN(false, 4000, "JWT 토큰을 찾을 수 없습니다."),
    EXPIRED_TOKEN(false, 4000, "JWT 토큰이 만료되었습니다."),

    // 2-3. 알림
    NOTIFICATION_NOT_FOUND(false, 4000, "알림을 찾을 수 없습니다."),
    NOTIFICATION_NOT_READ(false, 4000, "알림을 읽지 못했습니다."),
    NOTIFICATION_NOT_DELETED(false, 4000, "알림을 삭제할 수 없습니다."),
    SEND_FAILURE(false, 4000, "메세지 전송에 실패하였습니다."),
    EMAIL_SEND_FAILURE(false, 4000, "이메일 발송에 실패하였습니다."),
    UNAUTHORIZED_READ_NOTIFICATION(false, 4000, "알림 조회 권한이 없습니다."),
    UNAUTHORIZED_DELETE_NOTIFICATION(false, 4000, "알림 삭제 권한이 없습니다."),


    /**
     * 3. 서버 에러가 발생한 경우(5000)
     */

    // 0. 공통
    SERVER_ERROR(false, 5000, "서버와 연결에 실패하였습니다."),
    UNEXPECTED_ERROR(false, 5000, "예상치 못한 에러가 발생했습니다."),
    FAIL_TO_ENCODING(false, 5000, "요청 인코딩에 실패했습니다."),
    FAIL_TO_JSON(false, 5000, "JSON 파싱 에러가 발생했습니다.");

    private final boolean isSuccess;
    private final int code;
    private final String message;

    BaseResponseStatus(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }
}