package bhoon.sugang_helper.domain.course.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 전북대학교 학과명 표준화 및 별칭 매핑을 담당하는 유틸리티 클래스
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JbnuDepartmentStandardizer {

    /** 학과 명칭 뒤의 학년 정보 제거용 정규식 (예: " 1", " 2학년") */
    private static final Pattern NOISE_SUFFIX_PATTERN = Pattern.compile("\\s+[0-6](?:학년)?(?:\\s*등)?$");

    /** 괄호 및 그 내부 내용 제거용 정규식 (예: "(전주)") */
    public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(.*?\\)");

    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        initializeAISoftwareAliases();
        initializeEngineeringAliases();
        initializeEducationAliases();
        initializeAgricultureAliases();
        initializeCommonAliases();
        initializeMedicalAndSpecialAliases();
    }

    /** AI대학 및 소프트웨어 관련 별칭 초기화 */
    private static void initializeAISoftwareAliases() {
        String software = "소프트웨어공학과";
        ALIAS_MAP.put("컴공", software);
        ALIAS_MAP.put("컴퓨터공학", software);
        ALIAS_MAP.put("컴퓨터공학부", software);
        ALIAS_MAP.put("소프트공", software);
        ALIAS_MAP.put("소프트웨어", software);
        ALIAS_MAP.put("소프트웨어공", software);
        ALIAS_MAP.put("정보기술", software);
        
        String ai = "컴퓨터인공지능학부";
        ALIAS_MAP.put("컴퓨터인공지능", ai);
        ALIAS_MAP.put("IT지능정보공학과", ai);
        ALIAS_MAP.put("피지컬-AI융합공학과", ai);
        ALIAS_MAP.put("에너지-AI융합공학과", ai);
        ALIAS_MAP.put("지능정보융합공학과", ai);
    }

    /** 공과대학 학부제 및 세부 전공 별칭 초기화 */
    private static void initializeEngineeringAliases() {
        String mechanicalDesign = "기계설계공학부";
        ALIAS_MAP.put("기계설계", mechanicalDesign);
        ALIAS_MAP.put("기계설계공학", mechanicalDesign);
        ALIAS_MAP.put("나노바이오기계시스템공학", mechanicalDesign);

        String mechanicalSystem = "기계시스템공학부";
        ALIAS_MAP.put("기계시스템", mechanicalSystem);
        ALIAS_MAP.put("기계시스템공학", mechanicalSystem);

        String civilEnvEnergy = "토목/환경/자원·에너지공학부";
        ALIAS_MAP.put("자원·에너지공학", civilEnvEnergy);
        ALIAS_MAP.put("자원·에너지", civilEnvEnergy);
        ALIAS_MAP.put("에너지공학", civilEnvEnergy);
        ALIAS_MAP.put("토목공학", civilEnvEnergy);
        ALIAS_MAP.put("토목공학과", civilEnvEnergy);
        ALIAS_MAP.put("환경공학", civilEnvEnergy);
        ALIAS_MAP.put("환경공학과", civilEnvEnergy);
        ALIAS_MAP.put("건설환경공학과", civilEnvEnergy);
        ALIAS_MAP.put("환경학", civilEnvEnergy);
        
        String fusionTech = "융합기술공학부";
        ALIAS_MAP.put("융합기술공학", fusionTech);
        ALIAS_MAP.put("IT융합기전공학", fusionTech);
        ALIAS_MAP.put("IT응용시스템", fusionTech);
        ALIAS_MAP.put("메카트로닉스공", fusionTech);
        ALIAS_MAP.put("기계항공전기융합공학과", fusionTech);

        ALIAS_MAP.put("전자공", "전자공학부");
        ALIAS_MAP.put("전자·정보공학부", "전자공학부");
        ALIAS_MAP.put("전자정보재료", "전자공학부");
        ALIAS_MAP.put("전기공", "전기공학과");
        ALIAS_MAP.put("기계공", "기계공학과");
        ALIAS_MAP.put("기계공학", "기계공학과");
        ALIAS_MAP.put("항공우주공", "항공우주공학과");
        ALIAS_MAP.put("도시공", "도시공학과");
        ALIAS_MAP.put("환경계획", "도시공학과");
        ALIAS_MAP.put("건축공", "건축공학과");
        ALIAS_MAP.put("건축공학", "건축공학과");
        ALIAS_MAP.put("건축·도시공학", "건축공학과");
        ALIAS_MAP.put("바이오메디컬공학", "바이오메디컬공학부");
        ALIAS_MAP.put("헬스케어공학과", "바이오메디컬공학부");
        ALIAS_MAP.put("바이오나노시스템공학", "바이오메디컬공학부");
        ALIAS_MAP.put("메카노바이오", "바이오메디컬공학부");
        ALIAS_MAP.put("반도체·화학공학부", "화학공학부");
        ALIAS_MAP.put("화학공학", "화학공학부");
        ALIAS_MAP.put("화공부", "화학공학부");
        ALIAS_MAP.put("탄소융복합재료공학과", "화학공학부");
        ALIAS_MAP.put("환경에너지융합학과", "화학공학부");
        ALIAS_MAP.put("에너지저장·변환공학과", "화학공학부");
        ALIAS_MAP.put("산업정보시스템공", "산업정보시스템공학과");
        ALIAS_MAP.put("산업시스템공학", "산업정보시스템공학과");
        ALIAS_MAP.put("디자인제조공", "산업정보시스템공학과");
        ALIAS_MAP.put("고분자·나노공", "고분자·나노공학과");
        ALIAS_MAP.put("나노융합공학과", "고분자·나노공학과");
        ALIAS_MAP.put("유기소재섬유공학과", "유기소재섬유공학과");
        ALIAS_MAP.put("탄소소재파이버공학과", "유기소재섬유공학과");
        ALIAS_MAP.put("고분자섬유나노", "유기소재섬유공학과");
        ALIAS_MAP.put("고분자섬유나노공학부", "유기소재섬유공학과");
        ALIAS_MAP.put("플라즈마및양자빔응용공학과", "양자시스템공학과");
        ALIAS_MAP.put("재료공학과", "신소재공학부");
        ALIAS_MAP.put("금속공학", "신소재공학부");
        ALIAS_MAP.put("신소재", "신소재공학부");
    }

    /** 사범대학 학부제 및 교육학과 별칭 초기화 */
    private static void initializeEducationAliases() {
        String scienceEdu = "과학교육학부";
        ALIAS_MAP.put("물리교육", scienceEdu);
        ALIAS_MAP.put("생물교육", scienceEdu);
        ALIAS_MAP.put("지구과학교육", scienceEdu);
        ALIAS_MAP.put("화학교육", scienceEdu);
        ALIAS_MAP.put("과학교", scienceEdu);
        ALIAS_MAP.put("과학교육과", scienceEdu);

        String socialEdu = "사회과교육학부";
        ALIAS_MAP.put("역사교육", socialEdu);
        ALIAS_MAP.put("역사교육과", socialEdu);
        ALIAS_MAP.put("윤리교육", socialEdu);
        ALIAS_MAP.put("윤리학", socialEdu);
        ALIAS_MAP.put("일반사회교육", socialEdu);
        ALIAS_MAP.put("일반사회교육과", socialEdu);
        ALIAS_MAP.put("지리교육", socialEdu);
        ALIAS_MAP.put("사회과교육", socialEdu);
        ALIAS_MAP.put("사회과교육과", socialEdu);
        
        ALIAS_MAP.put("국어교육", "국어교육과");
        ALIAS_MAP.put("국어교육학과", "국어교육과");
        ALIAS_MAP.put("한국어교육학과", "국어교육과");
        ALIAS_MAP.put("어문교육학과", "국어교육과");
        ALIAS_MAP.put("영어교육", "영어교육과");
        ALIAS_MAP.put("영어교육학과", "영어교육과");
        ALIAS_MAP.put("수학교육", "수학교육과");
        ALIAS_MAP.put("수학교육학과", "수학교육과");
        ALIAS_MAP.put("독어교육", "독어교육과");
        ALIAS_MAP.put("한문교육", "한문교육과");
        ALIAS_MAP.put("체육교육", "체육교육과");
        ALIAS_MAP.put("교육", "교육학과");
        ALIAS_MAP.put("교육학", "교육학과");
    }

    /** 농업생명과학대학 및 환경생명자원대학 별칭 초기화 */
    private static void initializeAgricultureAliases() {
        ALIAS_MAP.put("생명공", "생명공학부");
        ALIAS_MAP.put("생명공학과", "생명공학부");
        ALIAS_MAP.put("식물방역학", "식물의학과");
        ALIAS_MAP.put("식물방역학과", "식물의학과");
        ALIAS_MAP.put("농화학과", "생물환경화학과");
        ALIAS_MAP.put("농생명과학과", "생물환경화학과");
        ALIAS_MAP.put("생리활성소재과학과", "생물환경화학과");
        ALIAS_MAP.put("생물산업기계", "지역건설공학과");
        ALIAS_MAP.put("농공학", "지역건설공학과");
        ALIAS_MAP.put("농업기계공학", "지역건설공학과");
        ALIAS_MAP.put("축산학과", "동물자원과학과");
        ALIAS_MAP.put("동물자원과", "동물자원과학과");
        ALIAS_MAP.put("동물의과학과", "동물자원과학과");
        ALIAS_MAP.put("농생물학", "농생물학과");
        ALIAS_MAP.put("농생물학과", "농생물학과");
        ALIAS_MAP.put("작물생명과학", "작물생명과학과");
        ALIAS_MAP.put("농학과", "작물생명과학과");
        ALIAS_MAP.put("한약자원학과", "작물생명과학과");
        ALIAS_MAP.put("농업시스템학과", "작물생명과학과");
        ALIAS_MAP.put("생명자원", "생명자원융합학과");
        ALIAS_MAP.put("융합환경생명공학과", "생명자원융합학과");
        ALIAS_MAP.put("농축산식품융합학과", "생명자원융합학과");
        ALIAS_MAP.put("에코농산업벤처시스템", "생명자원융합학과");
        ALIAS_MAP.put("농업경제학", "농경제유통학부");
        ALIAS_MAP.put("조경학", "조경학과");
        ALIAS_MAP.put("조경", "조경학과");
        ALIAS_MAP.put("생태조경디자인", "생태조경디자인학과");
        ALIAS_MAP.put("농촌환경조경학과", "조경학과");
        ALIAS_MAP.put("스마트팜", "스마트팜학과");
        ALIAS_MAP.put("임학과", "산림환경과학과");
        ALIAS_MAP.put("산림자원학과", "산림환경과학과");
    }

    /** 인문/사회/예술 및 일반 학과 별칭 초기화 */
    private static void initializeCommonAliases() {
        ALIAS_MAP.put("경영", "경영학과");
        ALIAS_MAP.put("경영학", "경영학과");
        ALIAS_MAP.put("창업경영학", "경영학과");
        ALIAS_MAP.put("회계", "회계학과");
        ALIAS_MAP.put("회계학", "회계학과");
        ALIAS_MAP.put("무역", "무역학과");
        ALIAS_MAP.put("글로컬커머스", "글로컬커머스학과");
        ALIAS_MAP.put("경제", "경제학부");
        ALIAS_MAP.put("경제학", "경제학부");
        ALIAS_MAP.put("심리", "심리학과");
        ALIAS_MAP.put("사회", "사회학과");
        ALIAS_MAP.put("사회학", "사회학과");
        ALIAS_MAP.put("행정", "행정학과");
        ALIAS_MAP.put("행정학", "행정학과");
        ALIAS_MAP.put("지방자치학과", "행정학과");
        ALIAS_MAP.put("신문방송", "미디어커뮤니케이션학과");
        ALIAS_MAP.put("언론홍보학과", "미디어커뮤니케이션학과");
        ALIAS_MAP.put("미디어PR학과", "미디어커뮤니케이션학과");
        ALIAS_MAP.put("미디어커뮤니케이션학", "미디어커뮤니케이션학과");
        ALIAS_MAP.put("사회복지", "사회복지학과");
        ALIAS_MAP.put("사회복지학", "사회복지학과");
        ALIAS_MAP.put("법", "법학과");
        ALIAS_MAP.put("법학", "법학과");
        ALIAS_MAP.put("국어국문", "국어국문학과");
        ALIAS_MAP.put("국어국문학", "국어국문학과");
        ALIAS_MAP.put("영어영문", "영어영문학과");
        ALIAS_MAP.put("중어중문", "중어중문학과");
        ALIAS_MAP.put("중어중문학", "중어중문학과");
        ALIAS_MAP.put("철학", "철학과");
        ALIAS_MAP.put("사학", "사학과");
        ALIAS_MAP.put("독일학", "독일학과");
        ALIAS_MAP.put("일본학", "일본학과");
        ALIAS_MAP.put("수학", "수학과");
        ALIAS_MAP.put("수", "수학과");
        ALIAS_MAP.put("화학", "화학과");
        ALIAS_MAP.put("화", "화학과");
        ALIAS_MAP.put("물리", "물리학과");
        ALIAS_MAP.put("통계", "통계학과");
        ALIAS_MAP.put("과학", "과학학과");
        ALIAS_MAP.put("생물학과", "생명과학과");
        ALIAS_MAP.put("생명과학부", "생명과학과");
        ALIAS_MAP.put("분자생물", "분자생물학과");
        ALIAS_MAP.put("분자생물학", "분자생물학과");
        ALIAS_MAP.put("지구환경과학", "지구환경과학과");
        ALIAS_MAP.put("반도체과학기술학", "반도체과학기술학과");
        ALIAS_MAP.put("문헌정보", "문헌정보학과");
        ALIAS_MAP.put("정치외교", "정치외교학과");
        ALIAS_MAP.put("정치학과", "정치외교학과");
        ALIAS_MAP.put("아동", "아동학과");
        ALIAS_MAP.put("아동가족", "아동학과");
        ALIAS_MAP.put("의류", "의류학과");
        ALIAS_MAP.put("주거환경", "주거환경학과");
        ALIAS_MAP.put("미술", "미술학과");
        ALIAS_MAP.put("미술학", "미술학과");
        ALIAS_MAP.put("무용", "무용학과");
        ALIAS_MAP.put("음악", "음악과");
        ALIAS_MAP.put("음악학과", "음악과");
        ALIAS_MAP.put("한국음악", "한국음악학과");
        ALIAS_MAP.put("산업디자인", "산업디자인학과");
        ALIAS_MAP.put("산업디자인학", "산업디자인학과");
        ALIAS_MAP.put("스포츠과학", "스포츠과학과");
        ALIAS_MAP.put("체육학과", "스포츠과학과");
        ALIAS_MAP.put("고고문화인", "고고문화인류학과");
        ALIAS_MAP.put("고고문화인류", "고고문화인류학과");
        ALIAS_MAP.put("식품영양", "식품영양학과");
    }

    /** 의학/치학 및 융합 전공 별칭 초기화 */
    private static void initializeMedicalAndSpecialAliases() {
        ALIAS_MAP.put("간호", "간호학과");
        ALIAS_MAP.put("간호학", "간호학과");
        ALIAS_MAP.put("의학", "의학과");
        ALIAS_MAP.put("의", "의학과");
        ALIAS_MAP.put("의예", "의예과");
        ALIAS_MAP.put("의과학과", "의학과");
        ALIAS_MAP.put("라이프스타일의학과", "의학과");
        ALIAS_MAP.put("치의", "치의학과");
        ALIAS_MAP.put("치의예", "치의예과");
        ALIAS_MAP.put("치의과학과", "치의학과");
        ALIAS_MAP.put("수의", "수의학과");
        ALIAS_MAP.put("수의예", "수의예과");
        ALIAS_MAP.put("수의방역학과", "수의학과");
        ALIAS_MAP.put("약학", "약학과");

        ALIAS_MAP.put("융합", "융합학부");
        ALIAS_MAP.put("이차전지", "융합학부");
        ALIAS_MAP.put("첨단방위산업", "융합학부");
        ALIAS_MAP.put("지식재산융합학과", "융합학부");
        ALIAS_MAP.put("스마트농업융합", "융합학부");
        ALIAS_MAP.put("바이오융합과학", "융합학부");
        ALIAS_MAP.put("정보보호공학과", "융합학부");
        ALIAS_MAP.put("융합기술경영학과", "융합학부");
        ALIAS_MAP.put("푸드테크학과", "융합학부");
        ALIAS_MAP.put("바이오나노융합공학과", "융합학부");
        
        ALIAS_MAP.put("융자전", "융합자율전공학부");
        ALIAS_MAP.put("융자전 1", "융합자율전공학부");
        ALIAS_MAP.put("융자전 2", "융합자율전공학부");
        ALIAS_MAP.put("국제이공", "국제이공학부");
        ALIAS_MAP.put("국제개발", "국제학부");
        ALIAS_MAP.put("사이버·지역", "융합자율전공학부");
        ALIAS_MAP.put("지역산업학과", "융합자율전공학부");
        ALIAS_MAP.put("글로컬커머스", "글로컬커머스학과");
        ALIAS_MAP.put("한옥건축학", "한옥학과");
        ALIAS_MAP.put("연금관리학과", "공공인재학부");
        
        ALIAS_MAP.put("공대", "공학계열 1");
        ALIAS_MAP.put("농대", "농업생명과학계열 1");
        ALIAS_MAP.put("인문대", "인문계열 1");
        ALIAS_MAP.put("인문계열", "인문계열 1");
        ALIAS_MAP.put("사회대", "사회과학계열 1");
        ALIAS_MAP.put("사회과학계열", "사회과학계열 1");
        ALIAS_MAP.put("경상계열", "경상계열 1");
        ALIAS_MAP.put("생활과학계열", "생활과학계열 1");
        ALIAS_MAP.put("자연과학계열", "자연과학계열 1");
        ALIAS_MAP.put("농업생명과학계열", "농업생명과학계열 1");
        ALIAS_MAP.put("환경생명자원계열", "환경생명자원계열 1");
    }

    /**
     * 원본 학과명을 표준화된 명칭으로 변환합니다.
     *
     * @param originalName 원본 학과명
     * @return 표준화된 학과명 (제외 대상인 경우 null 반환)
     */
    public static String standardize(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return null;
        }

        // 대학원 및 학부 시스템 제외 대상 필터링
        if (isExcluded(originalName)) {
            return null;
        }

        String normalized = originalName.trim().replace(".", "·");
        String cleaned = NOISE_SUFFIX_PATTERN.matcher(normalized).replaceAll("");

        // 1. 별칭 매핑 확인 (예: "컴공" -> "소프트웨어공학과")
        if (ALIAS_MAP.containsKey(cleaned)) {
            return ALIAS_MAP.get(cleaned);
        }

        // 2. 특수 사례 처리 (괄호 포함된 경우)
        String specialResult = handleSpecialCases(cleaned);
        if (specialResult != null) {
            return specialResult;
        }

        // 3. 괄호 제거 후 재확인
        String result = PARENTHESES_PATTERN.matcher(cleaned).replaceAll("").trim();
        if (ALIAS_MAP.containsKey(result)) {
            return ALIAS_MAP.get(result);
        }

        // 4. 기본형 보정 (학과/학부 누락 시)
        return compensateMissingSuffix(result);
    }

    /** 대학원 및 특수 학과 제외 여부 확인 */
    private static boolean isExcluded(String name) {
        return name.contains("대학원") || name.contains("기록관리") || 
               name.contains("보건학과") || name.contains("언어치료") ||
               name.contains("유연인쇄전자") || name.contains("환경에너지융합") ||
               name.contains("무형유산") || name.contains("바이오융복합") ||
               name.contains("데이터사이언스") || name.contains("의과학과") ||
               name.contains("치의과학과");
    }

    /** 특수 명칭 패턴 처리 */
    private static String handleSpecialCases(String name) {
        if (name.startsWith("산업(")) {
            if (name.contains("화학")) return "화학공학부";
            if (name.contains("건축")) return "건축공학과";
            if (name.contains("산업공")) return "산업정보시스템공학과";
            if (name.contains("기계")) return "기계공학과";
            if (name.contains("토목")) return "토목/환경/자원·에너지공학부";
        }
        
        if (name.startsWith("바이오(")) {
            return "바이오메디컬공학부";
        }

        if (name.startsWith("경영(")) {
            return "경영학과";
        }
        
        return null;
    }

    /** 학과/학부 접미사 누락 시 보정 */
    private static String compensateMissingSuffix(String name) {
        if (name.isEmpty()) return null;
        
        if (!name.endsWith("학과") && !name.endsWith("학부") && !name.endsWith("전공") && !name.endsWith("계열 1")) {
            if (ALIAS_MAP.containsKey(name + "학과")) return ALIAS_MAP.get(name + "학과");
            if (ALIAS_MAP.containsKey(name + "학부")) return ALIAS_MAP.get(name + "학부");
        }
        
        return name;
    }
}
