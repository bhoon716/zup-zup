package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.College;
import bhoon.sugang_helper.domain.course.entity.Department;
import bhoon.sugang_helper.domain.course.repository.CollegeRepository;
import bhoon.sugang_helper.domain.course.repository.DepartmentRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026년 전북대학교 학과 구조 데이터를 데이터베이스에 초기화 및 동기화(Upsert)하는 클래스
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JbnuDepartmentInitializer implements CommandLineRunner {

    private final CollegeRepository collegeRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[Initializer] 전북대학교 학제 데이터 동기화 시작 (2026년 기준)");

        Map<String, List<String>> hierarchy = createDepartmentHierarchy();

        int collegeCount = 0;
        int departmentCount = 0;

        for (Map.Entry<String, List<String>> entry : hierarchy.entrySet()) {
            String collegeName = entry.getKey();
            College college = collegeRepository.findByName(collegeName)
                    .orElseGet(() -> collegeRepository.save(new College(collegeName)));
            collegeCount++;

            for (String deptName : entry.getValue()) {
                if (departmentRepository.findByNameAndCollege(deptName, college).isEmpty()) {
                    departmentRepository.save(new Department(college, deptName));
                    departmentCount++;
                }
            }
        }

        log.info("[Initializer] 동기화 완료: {}개 단과대 처리, {}개 신규 학과 추가됨.", collegeCount, departmentCount);
    }

    /**
     * 2026년 공식 학제 기준 단과대별 학과(학부) 리스트 생성
     */
    private Map<String, List<String>> createDepartmentHierarchy() {
        Map<String, List<String>> hierarchy = new LinkedHashMap<>();

        hierarchy.put("간호대학", List.of("간호학과"));
        hierarchy.put("경상대학", List.of("경영학과", "경제학부", "무역학과", "회계학과"));
        hierarchy.put("공과대학", List.of(
                "건축공학과", "고분자·나노공학과", "유기소재섬유공학과", "기계공학과", "기계설계공학부",
                "기계시스템공학부", "도시공학과", "바이오메디컬공학부", "산업정보시스템공학과", "신소재공학부",
                "양자시스템공학과", "융합기술공학부", "전기공학과", "전자공학부",
                "토목/환경/자원·에너지공학부", "항공우주공학과", "화학공학부"));
        hierarchy.put("농업생명과학대학", List.of(
                "농경제유통학부", "식물의학과", "동물생명공학과", "동물자원과학과", "목재응용과학과",
                "산림환경과학과", "생명자원융합학과", "생물산업기계공학과", "생물환경화학과", "식품공학과",
                "원예학과", "작물생명과학과", "조경학과", "지역건설공학과", "스마트팜학과"));
        hierarchy.put("본부직속", List.of(
                "융합학부", "국제이공학부", "융합자율전공학부", "한옥학과", "글로컬커머스학과",
                "한국어학과", "K-엔터테인먼트학과", "이차전지공학과", "첨단방위산업학과"));
        hierarchy.put("사범대학", List.of(
                "국어교육과", "과학교육학부", "교육학과", "독어교육과", "사회과교육학부",
                "수학교육과", "영어교육과", "체육교육과"));
        hierarchy.put("사회과학대학", List.of(
                "사회복지학과", "사회학과", "미디어커뮤니케이션학과", "심리학과", "정치외교학과", "행정학과", "공공인재학부"));
        hierarchy.put("생활과학대학", List.of("식품영양학과", "아동학과", "의류학과", "주거환경학과"));
        hierarchy.put("수의과대학", List.of("수의예과", "수의학과"));
        hierarchy.put("약학대학", List.of("약학과"));
        hierarchy.put("예술대학", List.of("무용학과", "미술학과", "산업디자인학과", "음악과", "한국음악학과"));
        hierarchy.put("의과대학", List.of("의예과"));
        hierarchy.put("인문대학", List.of(
                "고고문화인류학과", "국어국문학과", "독일학과", "문헌정보학과", "사학과",
                "스페인·중남미학과", "영어영문학과", "일본학과", "중어중문학과", "철학과",
                "프랑스·아프리카학과", "국제학부"));
        hierarchy.put("자연과학대학", List.of(
                "과학학과", "물리학과", "반도체과학기술학과", "분자생물학과", "생명과학과",
                "수학과", "스포츠과학과", "지구환경과학과", "통계학과", "화학과"));
        hierarchy.put("치과대학", List.of("치의예과"));
        hierarchy.put("환경생명자원대학", List.of("생명공학부", "생태조경디자인학과", "한약자원학과"));
        hierarchy.put("AI대학", List.of("소프트웨어공학과", "컴퓨터인공지능학부"));

        return hierarchy;
    }
}
