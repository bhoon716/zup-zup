package bhoon.sugang_helper.domain.timetable.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.timetable.entity.CustomSchedule;
import bhoon.sugang_helper.domain.timetable.entity.CustomScheduleTime;
import bhoon.sugang_helper.domain.timetable.entity.Timetable;
import bhoon.sugang_helper.domain.timetable.entity.TimetableEntry;
import bhoon.sugang_helper.domain.timetable.repository.CustomScheduleRepository;
import bhoon.sugang_helper.domain.timetable.repository.TimetableEntryRepository;
import bhoon.sugang_helper.domain.timetable.repository.TimetableRepository;
import bhoon.sugang_helper.domain.timetable.request.CustomScheduleRequest;
import bhoon.sugang_helper.domain.timetable.request.TimetableRequest;
import bhoon.sugang_helper.domain.timetable.response.TimetableCourseResponse;
import bhoon.sugang_helper.domain.timetable.response.TimetableDetailResponse;
import bhoon.sugang_helper.domain.timetable.response.TimetableResponse;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableService {

    private static final int MAX_TIMETABLE_COUNT = 10;
    private static final int MAX_COURSE_COUNT = 10;

    private final TimetableRepository timetableRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final CustomScheduleRepository customScheduleRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public TimetableResponse createTimetable(TimetableRequest request) {
        User user = getCurrentUser();
        validateTimetableLimit(user.getId());

        if (request.isPrimary()) {
            resetPrimary(user.getId());
        }

        Timetable timetable = Timetable.builder()
                .userId(user.getId())
                .name(request.getName())
                .isPrimary(request.isPrimary())
                .build();

        return TimetableResponse.of(timetableRepository.save(timetable));
    }

    public List<TimetableResponse> getMyTimetables() {
        User user = getCurrentUser();
        return timetableRepository.findByUserId(user.getId()).stream()
                .map(TimetableResponse::of)
                .toList();
    }

    public TimetableDetailResponse getPrimaryTimetable() {
        User user = getCurrentUser();
        List<Timetable> primaryTimetables = timetableRepository.findByUserIdAndIsPrimaryTrue(user.getId());
        if (primaryTimetables.isEmpty()) {
            return null;
        }
        return getTimetableDetail(primaryTimetables.get(0));
    }

    public TimetableDetailResponse getTimetableDetail(Long timetableId) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);
        return getTimetableDetail(timetable);
    }

    @Transactional
    public void addCourse(Long timetableId, String courseKey) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);
        validateCourseLimit(timetable.getEntries().size());
        validateCourseExists(courseKey);
        validateCourseNotDuplicated(timetableId, courseKey);

        TimetableEntry entry = TimetableEntry.builder()
                .timetable(timetable)
                .courseKey(courseKey)
                .build();
        timetable.addEntry(entry);
    }

    @Transactional
    public void deleteCourse(Long timetableId, String courseKey) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);

        TimetableEntry entry = timetableEntryRepository.findByTimetableIdAndCourseKey(timetableId, courseKey)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "시간표에 존재하지 않는 강좌입니다."));

        timetable.removeEntry(entry);
        timetableEntryRepository.delete(entry);
    }

    @Transactional
    public void addCustomSchedule(Long timetableId, CustomScheduleRequest request) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);

        CustomSchedule schedule = CustomSchedule.builder()
                .timetable(timetable)
                .title(request.getTitle())
                .professor(request.getProfessor())
                .build();

        request.getSchedules().forEach(timeRequest -> {
            CustomScheduleTime time = CustomScheduleTime.builder()
                    .customSchedule(schedule)
                    .dayOfWeek(timeRequest.getDayOfWeek())
                    .startTime(timeRequest.getStartTime())
                    .endTime(timeRequest.getEndTime())
                    .classroom(timeRequest.getClassroom())
                    .build();
            schedule.addTime(time);
        });

        timetable.addCustomSchedule(schedule);
    }

    @Transactional
    public void deleteCustomSchedule(Long timetableId, Long scheduleId) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);

        CustomSchedule schedule = customScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 일정입니다."));

        if (!schedule.getTimetable().getId().equals(timetableId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "해당 시간표의 일정이 아닙니다.");
        }

        timetable.removeCustomSchedule(schedule);
        customScheduleRepository.delete(schedule);
    }

    @Transactional
    public void setPrimary(Long timetableId) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);

        resetPrimary(timetable.getUserId());
        timetable.setPrimary(true);
        log.info("[시간표] 대표 시간표를 변경했습니다. timetableId={}", timetableId);
    }

    @Transactional
    public void deleteTimetable(Long timetableId) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);

        boolean wasPrimary = timetable.isPrimary();
        Long userId = timetable.getUserId();
        timetableRepository.delete(timetable);

        if (!wasPrimary) {
            return;
        }

        promoteNextPrimary(userId, timetableId);
    }

    private TimetableDetailResponse getTimetableDetail(Timetable timetable) {
        Map<String, Course> courseMap = findCoursesByKey(timetable);
        List<TimetableCourseResponse> courses = mapTimetableCourses(timetable, courseMap);
        String totalCredits = calculateTotalCredits(courses);
        return TimetableDetailResponse.of(timetable, courses, totalCredits);
    }

    private Map<String, Course> findCoursesByKey(Timetable timetable) {
        List<String> courseKeys = timetable.getEntries().stream()
                .map(TimetableEntry::getCourseKey)
                .toList();

        return courseRepository.findByCourseKeyIn(courseKeys).stream()
                .collect(Collectors.toMap(Course::getCourseKey, Function.identity()));
    }

    private List<TimetableCourseResponse> mapTimetableCourses(Timetable timetable, Map<String, Course> courseMap) {
        return timetable.getEntries().stream()
                .map(TimetableEntry::getCourseKey)
                .map(courseMap::get)
                .filter(course -> course != null)
                .map(TimetableCourseResponse::of)
                .toList();
    }

    private String calculateTotalCredits(List<TimetableCourseResponse> courses) {
        double totalCredits = courses.stream()
                .map(TimetableCourseResponse::getCredits)
                .mapToDouble(this::toCreditValue)
                .sum();
        return String.valueOf(totalCredits);
    }

    private double toCreditValue(String credits) {
        if (credits == null || credits.isBlank()) {
            return 0.0;
        }

        try {
            return Double.parseDouble(credits);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private void validateTimetableLimit(Long userId) {
        long currentCount = timetableRepository.countByUserId(userId);
        if (currentCount < MAX_TIMETABLE_COUNT) {
            return;
        }

        throw new CustomException(ErrorCode.MAX_TIMETABLE_LIMIT_EXCEEDED,
                "시간표는 최대 " + MAX_TIMETABLE_COUNT + "개까지 생성 가능합니다.");
    }

    private void validateCourseLimit(int currentSize) {
        if (currentSize < MAX_COURSE_COUNT) {
            return;
        }

        throw new CustomException(ErrorCode.TIMETABLE_COURSE_LIMIT_EXCEEDED,
                "시간표당 최대 " + MAX_COURSE_COUNT + "개의 강좌만 담을 수 있습니다.");
    }

    private void validateCourseExists(String courseKey) {
        if (courseRepository.existsByCourseKey(courseKey)) {
            return;
        }

        throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 강좌입니다.");
    }

    private void validateCourseNotDuplicated(Long timetableId, String courseKey) {
        if (timetableEntryRepository.findByTimetableIdAndCourseKey(timetableId, courseKey).isEmpty()) {
            return;
        }

        throw new CustomException(ErrorCode.INVALID_INPUT, "이미 시간표에 존재하는 강좌입니다.");
    }

    private void promoteNextPrimary(Long userId, Long deletedTimetableId) {
        timetableRepository.findByUserId(userId).stream()
                .filter(timetable -> !timetable.getId().equals(deletedTimetableId))
                .findFirst()
                .ifPresent(timetable -> {
                    timetable.setPrimary(true);
                    log.info("[시간표] 대표 시간표를 자동 승계했습니다. timetableId={}", timetable.getId());
                });
    }

    private void resetPrimary(Long userId) {
        List<Timetable> primaryTimetables = timetableRepository.findByUserIdAndIsPrimaryTrue(userId);
        for (Timetable primaryTimetable : primaryTimetables) {
            primaryTimetable.setPrimary(false);
        }
    }

    private Timetable getTimetable(Long timetableId) {
        return timetableRepository.findById(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 시간표입니다."));
    }

    private void validateOwnership(Timetable timetable) {
        User user = getCurrentUser();
        if (timetable.getUserId().equals(user.getId())) {
            return;
        }

        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
