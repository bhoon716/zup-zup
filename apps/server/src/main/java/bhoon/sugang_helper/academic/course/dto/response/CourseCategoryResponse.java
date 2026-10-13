package bhoon.sugang_helper.academic.course.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourseCategoryResponse {

    private String category;

    private List<String> details;
}
