package bhoon.sugang_helper.domain.timetable.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.timetable.entity.CustomSchedule;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final CustomScheduleRepository customScheduleRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    private static final int MAX_TIMETABLE_COUNT = 10;
    private static final int MAX_COURSE_COUNT = 10;

    @Transactional
    public TimetableResponse createTimetable(TimetableRequest request) {
        User user = getCurrentUser();

        if (timetableRepository.countByUserId(user.getId()) >= MAX_TIMETABLE_COUNT) {
            throw new CustomException(ErrorCode.MAX_TIMETABLE_LIMIT_EXCEEDED,
                    "시간표는 최대 " + MAX_TIMETABLE_COUNT + "개까지 생성 가능합니다.");
        }

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

        // If multiple exist (shouldn't happen), take the first one or most recently
        // updated?
        // Let's take the first one for now.
        return getTimetableDetail(primaryTimetables.get(0));
    }

    public TimetableDetailResponse getTimetableDetail(Long timetableId) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);
        return getTimetableDetail(timetable);
    }

    private TimetableDetailResponse getTimetableDetail(Timetable timetable) {
        List<String> courseKeys = timetable.getEntries().stream()
                .map(TimetableEntry::getCourseKey)
                .toList();

        Map<String, Course> courseMap = courseRepository.findByCourseKeyIn(courseKeys).stream()
                .collect(Collectors.toMap(Course::getCourseKey, Function.identity()));

        List<TimetableCourseResponse> courses = timetable.getEntries().stream()
                .map(entry -> {
                    Course course = courseMap.get(entry.getCourseKey());
                    return course != null ? TimetableCourseResponse.of(course) : null;
                })
                .filter(c -> c != null)
                .toList();

        double totalCredits = courses.stream()
                .mapToDouble(c -> Double.parseDouble(c.getCredits()))
                .sum();

        return TimetableDetailResponse.of(timetable, courses, String.valueOf(totalCredits));
    }

    @Transactional
    public void addCourse(Long timetableId, String courseKey) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);

        if (timetable.getEntries().size() >= MAX_COURSE_COUNT) {
            throw new CustomException(ErrorCode.TIMETABLE_COURSE_LIMIT_EXCEEDED,
                    "시간표당 최대 " + MAX_COURSE_COUNT + "개의 강좌만 담을 수 있습니다.");
        }

        if (!courseRepository.existsByCourseKey(courseKey)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 강좌입니다.");
        }

        // Duplication check
        if (timetableEntryRepository.findByTimetableIdAndCourseKey(timetableId, courseKey).isPresent()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이미 시간표에 존재하는 강좌입니다.");
        }

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
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .color(request.getColor())
                .build();

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
        log.info("[Timetable] Setting primary: timetableId={}", timetableId);
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);

        resetPrimary(timetable.getUserId());
        timetable.setPrimary(true);
        log.info("[Timetable] Primary set successfully: timetableId={}", timetableId);
    }

    @Transactional
    public void deleteTimetable(Long timetableId) {
        Timetable timetable = getTimetable(timetableId);
        validateOwnership(timetable);

        // 대표 시간표를 삭제하는 경우, 다른 시간표가 있다면 하나를 대표로 설정하거나
        // 아니면 그냥 삭제할 수도 있음. 여기서는 그냥 삭제를 허용함.
        timetableRepository.delete(timetable);
    }

    private void resetPrimary(Long userId) {
        List<Timetable> victims = timetableRepository.findByUserIdAndIsPrimaryTrue(userId);
        log.info("[Timetable] Resetting primary for userId={}: found {} candidates", userId, victims.size());
        victims.forEach(t -> {
            log.info("[Timetable] Resetting primary: timetableId={}", t.getId());
            t.setPrimary(false);
        });
    }

    private Timetable getTimetable(Long timetableId) {
        return timetableRepository.findById(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 시간표입니다."));
    }

    private void validateOwnership(Timetable timetable) {
        User user = getCurrentUser();
        if (!timetable.getUserId().equals(user.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
