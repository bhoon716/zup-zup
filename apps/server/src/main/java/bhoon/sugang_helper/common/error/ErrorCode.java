package bhoon.sugang_helper.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),

    // Global
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G001", "내부 서버 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "G002", "잘못된 입력값입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "G003", "요청한 리소스를 찾을 수 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U002", "인증 정보가 유효하지 않습니다."),

    // Crawler
    CRAWLER_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "C001", "JBNU 수강신청 시스템 연결에 실패했습니다."),
    CRAWLER_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "데이터 파싱 중 오류가 발생했습니다."),
    CRAWLER_NO_DATA(HttpStatus.NOT_FOUND, "C003", "크롤링할 수 있는 데이터가 없습니다."),

    // Notification
    EMAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "N001", "이메일 발송 중 오류가 발생했습니다."),
    FCM_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "N002", "FCM 발송 중 오류가 발생했습니다."),
    WEBPUSH_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "N003", "Web Push 발송 중 오류가 발생했습니다."),

    // Subscription
    MAX_SUBSCRIPTION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "S001", "구독 가능 개수를 초과했습니다."),
    SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "S002", "이미 구독 중인 과목입니다."),

    // Timetable
    MAX_TIMETABLE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T001", "시간표 생성 개수 제한을 초과했습니다."),
    TIMETABLE_COURSE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T002", "시간표 강좌 개수 제한을 초과했습니다."),
    TIMETABLE_SCHEDULE_OVERLAP(HttpStatus.BAD_REQUEST, "T003", "시간표 내에 시간이 겹치는 일정이 존재합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
