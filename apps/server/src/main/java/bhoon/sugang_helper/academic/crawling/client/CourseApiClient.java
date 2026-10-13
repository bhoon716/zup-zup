package bhoon.sugang_helper.academic.crawling.client;

import java.io.InputStream;

/**
 * 강의 수집 대상에서 학기별 원본 데이터를 읽는 outbound port입니다.
 */
public interface CourseApiClient {

    InputStream fetchCourseDataStream(String year, String semester, String certDivision);
}
