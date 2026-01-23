package bhoon.sugang_helper.domain.course.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseSearchCondition {
    private String name;
    private String professor;
    private String subjectCode;
}
