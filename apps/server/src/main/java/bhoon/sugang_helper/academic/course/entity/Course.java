package bhoon.sugang_helper.academic.course.entity;

import bhoon.sugang_helper.academic.course.enums.CourseAccreditation;
import bhoon.sugang_helper.academic.course.enums.CourseClassification;
import bhoon.sugang_helper.academic.course.enums.CourseStatus;
import bhoon.sugang_helper.academic.course.enums.DisclosureStatus;
import bhoon.sugang_helper.academic.course.enums.GradingMethod;
import bhoon.sugang_helper.academic.course.enums.LectureLanguage;
import bhoon.sugang_helper.academic.course.enums.TargetGrade;
import bhoon.sugang_helper.common.api.persistence.BaseTimeEntity;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("PMD.TooManyFields")
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 식별자

    @Column(unique = true, nullable = false, length = 64)
    private String courseKey; // YYYY:학기코드:과목코드:분반

    @Column(nullable = false, length = 20)
    private String subjectCode; // 과목 코드

    @Column(name = "stdtr_no", length = 50)
    private String stdtrNo; // 학수번호

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

    @Column(columnDefinition = "TEXT")
    private String courseDirection; // 수업운영방향

    @Column(length = 50)
    private String classDuration; // 수업 시간(예: 50분)

    @Column(nullable = false)
    private Float averageRating; // 평균 별점

    @Column(nullable = false)
    private Integer reviewCount; // 리뷰 수

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<CourseSchedule> schedules = new ArrayList<>();

    @Builder
    public Course(String courseKey, String subjectCode, String stdtrNo, String name, String classNumber, String professor,
                  Integer capacity, Integer current, TargetGrade targetGrade, String academicYear, String semester,
                  CourseClassification classification, String department, GradingMethod gradingMethod,
                  String classTime, String credits, LectureLanguage lectureLanguage,
                  DisclosureStatus disclosure, String disclosureReason, Integer lectureHours, String generalCategory,
                  String generalDetail,
                  CourseAccreditation accreditation, CourseStatus status, String classroom, Boolean hasSyllabus,
                  String generalCategoryByYear, String courseDirection, String classDuration,
                  Float averageRating, Integer reviewCount) {
        this.courseKey = courseKey;
        this.subjectCode = subjectCode;
        this.stdtrNo = stdtrNo;
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
        this.averageRating = averageRating != null ? averageRating : 0.0f;
        this.reviewCount = reviewCount != null ? reviewCount : 0;
    }

    public void updateReviewStats(float averageRating, int reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public int getAvailable() {
        return Math.max(0, capacity - current);
    }

    public boolean isMatchingTarget(String year, String semester) {
        return this.academicYear.equals(year) && this.semester.equals(semester);
    }

    public void addSchedule(CourseSchedule schedule) {
        this.schedules.add(schedule);
        schedule.setCourse(this);
    }

    public boolean hasMetadataOrScheduleChanged(Course other) {
        return hasMetadataChanged(other) || !isSameSchedules(other.schedules);
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
    private boolean hasMetadataChanged(Course other) {
        if (other.stdtrNo != null && !Objects.equals(this.stdtrNo, other.stdtrNo)) {
            return true;
        }
        return !Objects.equals(this.name, other.name)
                || !Objects.equals(this.professor, other.professor)
                || !Objects.equals(this.capacity, other.capacity)
                || !Objects.equals(this.current, other.current)
                || !Objects.equals(this.targetGrade, other.targetGrade)
                || !Objects.equals(this.academicYear, other.academicYear)
                || !Objects.equals(this.semester, other.semester)
                || !Objects.equals(this.classification, other.classification)
                || !Objects.equals(this.department, other.department)
                || !Objects.equals(this.gradingMethod, other.gradingMethod)
                || !Objects.equals(this.lectureLanguage, other.lectureLanguage)
                || !Objects.equals(this.classTime, other.classTime)
                || !Objects.equals(this.credits, other.credits)
                || !Objects.equals(this.disclosure, other.disclosure)
                || !Objects.equals(this.disclosureReason, other.disclosureReason)
                || !Objects.equals(this.lectureHours, other.lectureHours)
                || !Objects.equals(this.generalCategory, other.generalCategory)
                || !Objects.equals(this.generalDetail, other.generalDetail)
                || !Objects.equals(this.accreditation, other.accreditation)
                || !Objects.equals(this.status, other.status)
                || !Objects.equals(this.classroom, other.classroom)
                || !Objects.equals(this.hasSyllabus, other.hasSyllabus)
                || !Objects.equals(this.generalCategoryByYear, other.generalCategoryByYear)
                || !Objects.equals(this.courseDirection, other.courseDirection)
                || !Objects.equals(this.classDuration, other.classDuration);
    }

    public boolean updateMetadata(Course other) {
        if (!hasMetadataOrScheduleChanged(other)) {
            return false;
        }
        copyMetadata(other);
        replaceSchedulesIfChanged(other);
        return true;
    }

    private void copyMetadata(Course other) {
        if (other.stdtrNo != null) {
            this.stdtrNo = other.stdtrNo;
        }
        this.name = other.name;
        this.professor = other.professor;
        this.capacity = other.capacity;
        this.current = other.current;
        this.targetGrade = other.targetGrade;
        this.academicYear = other.academicYear;
        this.semester = other.semester;
        this.classification = other.classification;
        this.department = other.department;
        this.gradingMethod = other.gradingMethod;
        this.lectureLanguage = other.lectureLanguage;
        this.classTime = other.classTime;
        this.credits = other.credits;
        this.disclosure = other.disclosure;
        this.disclosureReason = other.disclosureReason;
        this.lectureHours = other.lectureHours;
        this.generalCategory = other.generalCategory;
        this.generalDetail = other.generalDetail;
        this.accreditation = other.accreditation;
        this.status = other.status;
        this.classroom = other.classroom;
        this.hasSyllabus = other.hasSyllabus;
        this.generalCategoryByYear = other.generalCategoryByYear;
        this.courseDirection = other.courseDirection;
        this.classDuration = other.classDuration;
    }

    private void replaceSchedulesIfChanged(Course other) {
        if (isSameSchedules(other.schedules)) {
            return;
        }
        this.schedules.clear();
        for (CourseSchedule schedule : other.schedules) {
            this.addSchedule(
                    new CourseSchedule(schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime()));
        }
    }

    private boolean isSameSchedules(List<CourseSchedule> otherSchedules) {
        if (this.schedules.size() != otherSchedules.size()) {
            return false;
        }
        Comparator<CourseSchedule> scheduleOrder = Comparator
                .comparing((CourseSchedule schedule) -> schedule.getDayOfWeek().ordinal())
                .thenComparing(CourseSchedule::getStartTime)
                .thenComparing(CourseSchedule::getEndTime);
        List<CourseSchedule> currentSchedules = this.schedules.stream().sorted(scheduleOrder).toList();
        List<CourseSchedule> comparedSchedules = otherSchedules.stream().sorted(scheduleOrder).toList();
        for (int i = 0; i < currentSchedules.size(); i++) {
            if (!currentSchedules.get(i).isSameSchedule(comparedSchedules.get(i))) {
                return false;
            }
        }
        return true;
    }
}
