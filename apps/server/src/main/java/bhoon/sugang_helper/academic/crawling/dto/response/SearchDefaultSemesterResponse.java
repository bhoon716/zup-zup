package bhoon.sugang_helper.academic.crawling.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchDefaultSemesterResponse {

    private final String semester;

    public static SearchDefaultSemesterResponse of(String semester) {
        return new SearchDefaultSemesterResponse(semester);
    }
}
