package bhoon.sugang_helper.domain.course.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import bhoon.sugang_helper.domain.course.enums.CourseAccreditation;
import bhoon.sugang_helper.domain.course.enums.CourseClassification;
import bhoon.sugang_helper.domain.course.enums.CourseStatus;
import bhoon.sugang_helper.domain.course.enums.DisclosureStatus;
import bhoon.sugang_helper.domain.course.enums.GradingMethod;
import bhoon.sugang_helper.domain.course.enums.LectureLanguage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
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
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String courseKey; // 'YYYY:Semester:Code:Class'

    @Column(nullable = false, length = 20)
    private String subjectCode; // 과목코드

    @Column(nullable = false, length = 100)
    private String name; // 과목명

    @Column(nullable = false, length = 5)
    private String classNumber; // 분반 (e.g., "01")

    @Column(length = 50)
    private String professor; // 교수 이름

    @Column(nullable = false)
    private Integer capacity; // 정원

    @Column(nullable = false)
    private Integer current; // 신청인원

    @Column(length = 20)
    private String targetGrade; // 대상 학년

    @Column(nullable = false, length = 4, name = "academic_year")
    private String academicYear; // 연도 (YY)

    @Column(nullable = false, length = 10)
    private String semester; // 학기 (SHTM)

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
    private String credits; // CSV 추가 필드
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DisclosureStatus disclosure; // 공개여부

    @Column(length = 100)
    private String disclosureReason; // 비공개사유

    @Column
    private Integer lectureHours; // 시간 (Number of hours)

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
    private Boolean hasSyllabus; // 강의계획서여부 (Y/N)

    @Column(length = 50)
    private String generalCategoryByYear; // 입학년도기준교양영역구분

    @Column(length = 500)
    private String courseDirection; // 수업운영방향

    @Column(length = 50)
    private String classDuration; // 수업 시간 (duration, e.g. "50분")

    @Column(nullable = false)
    private LocalDateTime lastCrawledAt; // 마지막 크롤링 시간

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSchedule> schedules = new ArrayList<>();

    @Builder
    public Course(String courseKey, String subjectCode, String name, String classNumber, String professor,
            Integer capacity, Integer current, String targetGrade, String academicYear, String semester,
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
    }

    public int getAvailable() {
        return Math.max(0, capacity - current);
    }

    public void addSchedule(CourseSchedule schedule) {
        this.schedules.add(schedule);
        schedule.setCourse(this);
    }

    public void updateStatus(Integer capacity, Integer current) {
        this.capacity = capacity;
        this.current = current;
        this.lastCrawledAt = LocalDateTime.now();
    }

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

        // Update schedules
        this.schedules.clear();
        for (CourseSchedule schedule : other.getSchedules()) {
            this.addSchedule(new CourseSchedule(schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime()));
        }
    }
}
