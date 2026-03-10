package bhoon.sugang_helper.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전반에서 발생하는 예외 상황을 정의하는 에러 코드 Enum입니다.
 * 각 상수는 HTTP 상태 코드, 고유 에러 코드, 사용자에게 전달될 메시지를 포함합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /* 인증 관련 에러 */
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),

    /* 공통 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G001", "내부 서버 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "G002", "잘못된 입력값입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "G003", "요청한 리소스를 찾을 수 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "G004", "요청이 너무 많습니다."),

    /* 사용자 관련 에러 */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U002", "인증 정보가 유효하지 않습니다."),
    UNVERIFIED_EMAIL(HttpStatus.BAD_REQUEST, "U003", "이메일 인증이 완료되지 않았습니다."),

    /* 크롤러 관련 에러 */
    CRAWLER_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "C001", "JBNU 수강신청 시스템 연결에 실패했습니다."),
    CRAWLER_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "데이터 파싱 중 오류가 발생했습니다."),
    CRAWLER_NO_DATA(HttpStatus.NOT_FOUND, "C003", "크롤링할 수 있는 데이터가 없습니다."),
    FAILED_TO_CRAWL_COURSES(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "강의 크롤링 작업에 실패했습니다."),

    /* 알림 관련 에러 */
    EMAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "N001", "이메일 발송 중 오류가 발생했습니다."),
    FCM_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "N002", "FCM 발송 중 오류가 발생했습니다."),
    WEB_PUSH_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "N003", "Web Push 발송 중 오류가 발생했습니다."),
    DISCORD_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "N004", "디스코드 알림 발송 중 오류가 발생했습니다."),

    /* 웹푸시 전용 에러 */
    WEB_PUSH_NOT_INITIALIZED(HttpStatus.INTERNAL_SERVER_ERROR, "N010", "Web Push 서비스가 초기화되지 않았습니다. (서버 설정 확인 필요)"),
    WEB_PUSH_MISSING_KEYS(HttpStatus.BAD_REQUEST, "N011", "Web Push 암호화 키(p256dh, auth)가 누락되었습니다."),
    WEB_PUSH_INVALID_SUBSCRIPTION(HttpStatus.NOT_FOUND, "N012", "유효하지 않은 Web Push 구독 정보입니다. (재구독 필요)"),

    /* 구독 관련 에러 */
    MAX_SUBSCRIPTION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "S001", "구독 가능 개수를 초과했습니다."),
    SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "S002", "이미 구독 중인 과목입니다."),

    /* 시간표 관련 에러 */
    MAX_TIMETABLE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T001", "시간표 생성 개수 제한을 초과했습니다."),
    TIMETABLE_COURSE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T002", "시간표 강좌 개수 제한을 초과했습니다."),
    TIMETABLE_SCHEDULE_OVERLAP(HttpStatus.BAD_REQUEST, "T003", "시간표 내에 시간이 겹치는 일정이 존재합니다."),

    /* 강의 평가 관련 에러 */
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "강의 리뷰를 찾을 수 없습니다."),
    REVIEW_UNAUTHORIZED(HttpStatus.FORBIDDEN, "E002", "해당 리뷰에 대한 권한이 없습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "E003", "이미 이 강의에 리뷰를 작성하셨습니다."),

    /* 피드백 관련 에러 */
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "건의사항을 찾을 수 없습니다."),
    FEEDBACK_REPLY_NOT_FOUND(HttpStatus.NOT_FOUND, "F002", "답변 코멘트를 찾을 수 없습니다."),
    FEEDBACK_UNAUTHORIZED(HttpStatus.FORBIDDEN, "F003", "해당 피드백에 대한 권한이 없습니다."),
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "F004", "파일 업로드 중 오류가 발생했습니다."),
    MAX_FILE_UPLOAD_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "F005", "최대 첨부 가능한 파일 수를 초과했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "F006", "지원하지 않는 파일 형식입니다. 이미지 파일만 업로드 가능합니다."),
    DAILY_FEEDBACK_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "F007", "일일 건의사항 작성 가능 횟수를 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
