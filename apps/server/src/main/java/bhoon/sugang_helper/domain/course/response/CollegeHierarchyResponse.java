package bhoon.sugang_helper.domain.course.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 단과대학 및 하위 학과 계층 구조 응답 DTO
 */
@Getter
@Builder
public class CollegeHierarchyResponse {
    private Long id;
    private String name;
    private List<DepartmentResponse> departments;

    @Getter
    @Builder
    public static class DepartmentResponse {
        private Long id;
        private String name;
    }
}
