package bhoon.sugang_helper.academic.course.dto.response;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseCategoryResponse {

    private final String category;

    private final List<String> details;

    public static CourseCategoryResponse of(String category, List<String> details) {
        return new CourseCategoryResponse(category, details);
    }
}
