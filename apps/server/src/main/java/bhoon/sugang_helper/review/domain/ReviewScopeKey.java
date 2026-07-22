package bhoon.sugang_helper.review.domain;

import bhoon.sugang_helper.course.domain.Course;

/**
 * 리뷰를 묶는 기준 키입니다. 같은 과목코드와 식별 가능한 교수 조합을 하나의 리뷰 그룹으로 봅니다.
 * 교수 식별자가 없으면 해당 강의의 courseKey만 리뷰 그룹으로 봅니다.
 */
public record ReviewScopeKey(String subjectCode, String professor) {

    public static ReviewScopeKey from(Course course) {
        return new ReviewScopeKey(course.getSubjectCode(), normalizeProfessor(course.getProfessor()));
    }

    public static String normalizeProfessor(String professor) {
        String normalizedProfessor = professor == null ? "" : professor.trim();
        return "교수 미지정".equals(normalizedProfessor) ? "" : normalizedProfessor;
    }

    public boolean hasIdentifiableProfessor() {
        return professor != null && !professor.isBlank();
    }
}
