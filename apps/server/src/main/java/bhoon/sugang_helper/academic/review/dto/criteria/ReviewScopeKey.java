package bhoon.sugang_helper.academic.review.dto.criteria;





public record ReviewScopeKey(String subjectCode, String professor) {

    public static ReviewScopeKey from(String subjectCode, String professor) {
        return new ReviewScopeKey(subjectCode, normalizeProfessor(professor));
    }

    public static String normalizeProfessor(String professor) {
        String normalizedProfessor = professor == null ? "" : professor.trim();
        return "교수 미지정".equals(normalizedProfessor) ? "" : normalizedProfessor;
    }

    public boolean hasIdentifiableProfessor() {
        return professor != null && !professor.isBlank();
    }
}
