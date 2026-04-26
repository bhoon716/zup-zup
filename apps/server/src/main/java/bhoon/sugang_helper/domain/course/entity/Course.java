package bhoon.sugang_helper.domain.course.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import bhoon.sugang_helper.domain.course.enums.CourseAccreditation;
import bhoon.sugang_helper.domain.course.enums.CourseClassification;
import bhoon.sugang_helper.domain.course.enums.CourseStatus;
import bhoon.sugang_helper.domain.course.enums.DisclosureStatus;
import bhoon.sugang_helper.domain.course.enums.GradingMethod;
import bhoon.sugang_helper.domain.course.enums.LectureLanguage;
import bhoon.sugang_helper.domain.course.enums.TargetGrade;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 식별자

    @Column(unique = true, nullable = false, length = 64)
    private String courseKey; // YYYY:학기코드:과목코드:분반

    @Column(nullable = false, length = 20)
    private String subjectCode; // 과목 코드

    @Column(nullable = false, length = 100)
    private String name; // 과목명

    @Column(nullable = false, length = 5)
    private String classNumber; // 분반

    @Column(length = 50)
    private String professor; // 교수명

    @Column(nullable = false)
    private Integer capacity; // 정원

    @Column(nullable = false)
    private Integer current; // 신청인원

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TargetGrade targetGrade; // 대상 학년

    @Column(nullable = false, length = 4, name = "academic_year")
    private String academicYear; // 연도(YY)

    @Column(nullable = false, length = 10)
    private String semester; // 학기 코드(SHTM)

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CourseClassification classification; // 이수구분 (CPTNFGNM)

    @Column(length = 100)
    private String department; // 학과 (SUSTCDNM)

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private GradingMethod gradingMethod; // 성적평가방식 (SCORTRETFGNM)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LectureLanguage lectureLanguage; // 강의언어 (LTLANGFGNM)

    @Column(length = 500)
    private String classTime; // 강의시간 (DAYTMCTNT)

    @Column(length = 10)
    private String credits; // 학점

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DisclosureStatus disclosure; // 공개여부

    @Column(length = 100)
    private String disclosureReason; // 비공개사유

    @Column
    private Integer lectureHours; // 강의 시수

    @Column(length = 50)
    private String generalCategory; // 교양영역구분

    @Column(length = 50)
    private String generalDetail; // 교양영역상세구분

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CourseAccreditation accreditation; // 인증구분

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CourseStatus status; // 설강여부

    @Column(length = 200)
    private String classroom; // 강의실

    @Column(name = "has_syllabus")
    private Boolean hasSyllabus; // 강의계획서 여부

    @Column(length = 50)
    private String generalCategoryByYear; // 입학년도기준교양영역구분

    @Column(length = 500)
    private String courseDirection; // 수업운영방향

    @Column(length = 50)
    private String classDuration; // 수업 시간(예: 50분)

    @Column(nullable = false)
    private LocalDateTime lastCrawledAt; // 마지막 크롤링 시간

    @Column(nullable = false)
    private Float averageRating = 0.0f; // 평균 별점

    @Column(nullable = false)
    private Integer reviewCount = 0; // 리뷰 수

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSchedule> schedules = new ArrayList<>();

    /**
     * 강의 엔티티 생성을 위한 빌더 생성자
     */
    @Builder
    public Course(String courseKey, String subjectCode, String name, String classNumber, String professor,
            Integer capacity, Integer current, TargetGrade targetGrade, String academicYear, String semester,
            CourseClassification classification, String department, GradingMethod gradingMethod,
            String classTime, String credits, LectureLanguage lectureLanguage,
            DisclosureStatus disclosure, String disclosureReason, Integer lectureHours, String generalCategory,
            String generalDetail,
            CourseAccreditation accreditation, CourseStatus status, String classroom, Boolean hasSyllabus,
            String generalCategoryByYear, String courseDirection, String classDuration) {
        this.courseKey = courseKey;
        this.subjectCode = subjectCode;
        this.name = name;
        this.classNumber = classNumber;
        this.professor = professor;
        this.capacity = capacity;
        this.current = current;
        this.targetGrade = targetGrade;
        this.academicYear = academicYear;
        this.semester = semester;
        this.classification = classification;
        this.department = department;
        this.gradingMethod = gradingMethod;
        this.classTime = classTime;
        this.credits = credits;
        this.lectureLanguage = lectureLanguage;
        this.disclosure = disclosure;
        this.disclosureReason = disclosureReason;
        this.lectureHours = lectureHours;
        this.generalCategory = generalCategory;
        this.generalDetail = generalDetail;
        this.accreditation = accreditation;
        this.status = status;
        this.classroom = classroom;
        this.hasSyllabus = hasSyllabus;
        this.generalCategoryByYear = generalCategoryByYear;
        this.courseDirection = courseDirection;
        this.classDuration = classDuration;
        this.lastCrawledAt = LocalDateTime.now();
        this.averageRating = 0.0f;
        this.reviewCount = 0;
    }

    /**
     * 강의 별점 및 리뷰 수 업데이트
     */
    public void updateReviewStats(float averageRating, int reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    /**
     * 현재 수강 신청 가능한 여석 계산
     */
    public int getAvailable() {
        return Math.max(0, capacity - current);
    }

    /**
     * 입력받은 년도와 학기가 현재 강의의 정보와 일치하는지 확인
     */
    public boolean isMatchingTarget(String year, String semester) {
        return this.academicYear.equals(year) && this.semester.equals(semester);
    }

    /**
     * 강의 시간표 정보 추가
     */
    public void addSchedule(CourseSchedule schedule) {
        this.schedules.add(schedule);
        schedule.setCourse(this);
    }

    /**
     * 크롤링된 새로운 정보로 강의 메타데이터 업데이트
     */
    public void updateMetadata(Course other) {
        this.name = other.getName();
        this.professor = other.getProfessor();
        this.capacity = other.getCapacity();
        this.current = other.getCurrent();
        this.targetGrade = other.getTargetGrade();
        this.academicYear = other.getAcademicYear();
        this.semester = other.getSemester();
        this.classification = other.getClassification();
        this.department = other.getDepartment();
        this.gradingMethod = other.getGradingMethod();
        this.lectureLanguage = other.getLectureLanguage();
        this.classTime = other.getClassTime();
        this.credits = other.getCredits();
        this.disclosure = other.getDisclosure();
        this.disclosureReason = other.getDisclosureReason();
        this.lectureHours = other.getLectureHours();
        this.generalCategory = other.getGeneralCategory();
        this.generalDetail = other.getGeneralDetail();
        this.accreditation = other.getAccreditation();
        this.status = other.getStatus();
        this.classroom = other.getClassroom();
        this.hasSyllabus = other.getHasSyllabus();
        this.generalCategoryByYear = other.getGeneralCategoryByYear();
        this.courseDirection = other.getCourseDirection();
        this.classDuration = other.getClassDuration();
        this.lastCrawledAt = LocalDateTime.now();

        this.schedules.clear();
        for (CourseSchedule schedule : other.getSchedules()) {
            this.addSchedule(
                    new CourseSchedule(schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime()));
        }
    }
}
