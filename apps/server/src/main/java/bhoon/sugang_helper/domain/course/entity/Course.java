package bhoon.sugang_helper.domain.course.entity;

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
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전북대학교 강의 정보를 저장하는 엔티티 클래스
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "courses", indexes = {
        @Index(name = "idx_course_key", columnList = "courseKey", unique = true),
        @Index(name = "idx_subject_code", columnList = "subjectCode"),
        @Index(name = "idx_department", columnList = "department"),
        @Index(name = "idx_college_id", columnList = "collegeId"),
        @Index(name = "idx_department_id", columnList = "departmentId")
})
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String courseKey; // 고유 키 (년도:학기:과목코드:분반)

    @Column(nullable = false)
    private String subjectCode; // 과목코드

    @Column(nullable = false)
    private String name; // 과목명

    @Column(nullable = false)
    private String classNumber; // 분반

    private String professor; // 담당교수

    private Integer capacity; // 제한인원

    private Integer current; // 수강인원

    @Enumerated(EnumType.STRING)
    private TargetGrade targetGrade; // 대상학년

    private String academicYear; // 학년도

    private String semester; // 학기 코드

    @Enumerated(EnumType.STRING)
    private CourseClassification classification; // 이수구분

    private String department; // 개설학과 (표준화된 명칭)

    @Enumerated(EnumType.STRING)
    private GradingMethod gradingMethod; // 성적부여방식

    private String classTime; // 강의시간 (원본 문자열)

    private String credits; // 학점

    @Enumerated(EnumType.STRING)
    private LectureLanguage lectureLanguage; // 강의언어

    @Enumerated(EnumType.STRING)
    private DisclosureStatus disclosure; // 공개여부

    @Column(length = 1000)
    private String disclosureReason; // 비공개사유

    private Integer lectureHours; // 수업 시간

    private String generalCategory; // 교양영역

    private String generalDetail; // 교양세부영역

    @Enumerated(EnumType.STRING)
    private CourseAccreditation accreditation; // 인증구분

    @Enumerated(EnumType.STRING)
    private CourseStatus status; // 개설상태

    private String classroom; // 강의실 위치

    private Boolean hasSyllabus; // 강의계획서 유무

    private String generalCategoryByYear; // 학년도별 교양영역 정보

    private String courseDirection; // 강좌방향

    private String classDuration; // 수업운영주차

    @Column(nullable = false)
    private LocalDateTime lastCrawledAt; // 마지막 크롤링 시각

    @Column(nullable = false)
    private Float averageRating = 0.0f; // 평균 별점

    @Column(nullable = false)
    private Integer reviewCount = 0; // 리뷰 총 개수

    private Long collegeId; // 단과대 ID

    private Long departmentId; // 학과 ID

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSchedule> schedules = new ArrayList<>();

    @Builder
    public Course(String courseKey, String subjectCode, String name, String classNumber, String professor,
            Integer capacity, Integer current, TargetGrade targetGrade, String academicYear, String semester,
            CourseClassification classification, String department, Long collegeId, Long departmentId,
            GradingMethod gradingMethod,
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
        this.collegeId = collegeId;
        this.departmentId = departmentId;
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
        this.schedules = new ArrayList<>();
    }

    public void updateMetadata(Course other) {
        this.name = other.getName();
        this.professor = other.getProfessor();
        this.capacity = other.getCapacity();
        this.current = other.getCurrent();
        this.targetGrade = other.getTargetGrade();
        this.classification = other.getClassification();
        this.department = other.getDepartment();
        this.collegeId = other.getCollegeId();
        this.departmentId = other.getDepartmentId();
        this.gradingMethod = other.getGradingMethod();
        this.classTime = other.getClassTime();
        this.credits = other.getCredits();
        this.lectureLanguage = other.getLectureLanguage();
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
        if (other.getSchedules() != null) {
            other.getSchedules().forEach(this::addSchedule);
        }
    }

    public boolean isMatchingTarget(String year, String semester) {
        return this.academicYear.equals(year) && this.semester.equals(semester);
    }

    public void updateReviewStats(float averageRating, int reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public int getAvailable() {
        return Math.max(0, (capacity != null ? capacity : 0) - (current != null ? current : 0));
    }

    public void addSchedule(CourseSchedule schedule) {
        this.schedules.add(schedule);
        schedule.setCourse(this);
    }
}
