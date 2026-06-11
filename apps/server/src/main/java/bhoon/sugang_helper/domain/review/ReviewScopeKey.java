package bhoon.sugang_helper.domain.review;

import bhoon.sugang_helper.domain.course.entity.Course;

/**
 * 리뷰를 묶는 기준 키입니다.
 * 같은 과목코드와 교수 조합을 하나의 리뷰 그룹으로 봅니다.
 */
public record ReviewScopeKey(String subjectCode, String professor) {

    public static ReviewScopeKey from(Course course) {
        return new ReviewScopeKey(course.getSubjectCode(), normalizeProfessor(course.getProfessor()));
    }

    public static String normalizeProfessor(String professor) {
        String normalizedProfessor = professor == null ? "" : professor.trim();
        return "교수 미지정".equals(normalizedProfessor) ? "" : normalizedProfessor;
    }
}
