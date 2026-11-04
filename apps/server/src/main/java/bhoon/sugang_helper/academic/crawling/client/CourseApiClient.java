package bhoon.sugang_helper.academic.crawling.client;

import java.io.InputStream;




public interface CourseApiClient {

    InputStream fetchCourseDataStream(String year, String semester, String certDivision);
}
