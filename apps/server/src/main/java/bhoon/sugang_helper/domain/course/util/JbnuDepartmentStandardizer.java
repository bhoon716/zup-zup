package bhoon.sugang_helper.domain.course.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 전북대학교 학과명 표준화 및 별칭 매핑을 담당하는 유틸리티 클래스
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JbnuDepartmentStandardizer {

    private static final Pattern TRAILING_GRADE_PATTERN = Pattern
            .compile("^(?<before>.*?)(?:\\s+)(?<grade>[1-6])(?:학년)?(?:\\s*등)?$");

    /**
     * 학과명 표준화 매핑 사전 (2026년 기준 축약어 및 구 명칭 대응)
     */
    private static final Map<String, String> ALIAS_MAP = Map.ofEntries(
            Map.entry("간호", "간호학과"),
            Map.entry("건축공", "건축공학과"),
            Map.entry("경영", "경영학과"),
            Map.entry("과학", "과학과"),
            Map.entry("고고문화인", "고고문화인류학과"),
            Map.entry("고분자.나노공", "고분자나노공학과"),
            Map.entry("기계시스템", "기계시스템공학부"),
            Map.entry("국어국문", "국어국문학과"),
            Map.entry("영어영문", "영어영문학과"),
            Map.entry("식품영양", "식품영양학과"),
            Map.entry("심리", "심리학과"),
            Map.entry("아동", "아동학과"),
            Map.entry("영어교육", "영어교육과"),
            Map.entry("의", "의학과"),
            Map.entry("의예", "의예과"),
            Map.entry("치의", "치의학과"),
            Map.entry("치의예", "치의예과"),
            Map.entry("화", "화학과"),
            Map.entry("전기공", "전기공학과"),
            Map.entry("조경", "조경학과"),
            Map.entry("항공우주공", "항공우주공학과"),
            Map.entry("행정", "행정학과"),
            Map.entry("회계", "회계학과"),
            Map.entry("수학", "수학과"),
            Map.entry("물리", "물리학과"),
            Map.entry("화공부", "화학공학부"),
            Map.entry("의류", "의류학과"),
            Map.entry("중어중문", "중어중문학과"),
            Map.entry("일어일문", "일어일문학과"),
            Map.entry("정치외교", "정치외교학과"),
            Map.entry("사회", "사회학과"),
            Map.entry("문헌정보", "문헌정보학과"),
            Map.entry("식품공", "식품공학과"),
            Map.entry("생명과학", "생명과학과"),
            Map.entry("생명공", "생명공학과"),
            Map.entry("신문방송", "미디어커뮤니케이션학과"),
            Map.entry("농생물", "식물의학과"),
            Map.entry("음악학과", "음악과"),
            Map.entry("사회복지", "사회복지학과"),
            Map.entry("국제학", "국제학부"),
            Map.entry("소학", "소프트웨어공학과"),
            Map.entry("컴공", "컴퓨터공학부"),
            Map.entry("컴퓨터공학", "컴퓨터공학부")
    );

    /**
     * 원본 학과명 문자열을 입력받아 학년 숫자를 제거하고 표준 명칭으로 변환
     *
     * @param rawToken    정규화할 원본 학과명 토큰 (예: '경영 4')
     * @param gradeNumber 대상 학년 숫자 (예: '4')
     * @return 표준화된 학과명 (예: '경영학과')
     */
    public static String normalize(String rawToken, String gradeNumber) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        String current = rawToken.trim();

        if (gradeNumber != null) {
            current = removeGradeSuffix(current, gradeNumber);
        }

        return ALIAS_MAP.getOrDefault(current, current);
    }

    /**
     * 학과명 뒤에 붙은 특정 학년 표기 제거
     */
    private static String removeGradeSuffix(String token, String gradeNumber) {
        String result = token;
        while (true) {
            Matcher matcher = TRAILING_GRADE_PATTERN.matcher(result);
            if (!matcher.matches()) {
                break;
            }

            String grade = matcher.group("grade");
            String before = matcher.group("before");

            if (grade.equals(gradeNumber)) {
                result = before.trim();
            } else {
                break;
            }
        }
        return result;
    }
}
