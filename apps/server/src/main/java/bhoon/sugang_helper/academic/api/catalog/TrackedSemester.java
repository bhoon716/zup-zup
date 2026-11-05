package bhoon.sugang_helper.academic.api.catalog;

/** 현재 추적 학기의 조회 값. 구독 허용 정책은 소비 모듈이 소유한다. */
public record TrackedSemester(String year, String semesterCode) {
}
