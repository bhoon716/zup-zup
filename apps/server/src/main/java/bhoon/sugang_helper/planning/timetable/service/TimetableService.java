package bhoon.sugang_helper.planning.timetable.service;

import bhoon.sugang_helper.academic.api.catalog.CourseCatalog;
import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.identity.events.UserRegisteredEvent;
import bhoon.sugang_helper.planning.api.PrimaryTimetableSnapshot;
import bhoon.sugang_helper.planning.api.Timetables;
import bhoon.sugang_helper.planning.timetable.dto.request.CustomScheduleRequest;
import bhoon.sugang_helper.planning.timetable.dto.request.CustomScheduleTimeRequest;
import bhoon.sugang_helper.planning.timetable.dto.request.TimetableRequest;
import bhoon.sugang_helper.planning.timetable.dto.response.TimetableDetailResponse;
import bhoon.sugang_helper.planning.timetable.dto.response.TimetableResponse;
import bhoon.sugang_helper.planning.timetable.entity.CustomSchedule;
import bhoon.sugang_helper.planning.timetable.entity.CustomScheduleTime;
import bhoon.sugang_helper.planning.timetable.entity.Timetable;
import bhoon.sugang_helper.planning.timetable.entity.TimetableEntry;
import bhoon.sugang_helper.planning.timetable.repository.CustomScheduleRepository;
import bhoon.sugang_helper.planning.timetable.repository.TimetableEntryRepository;
import bhoon.sugang_helper.planning.timetable.repository.TimetableRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableService implements Timetables {

    private static final String EVENT_KEY = "event";
    private static final String TIMETABLE_ID_KEY = "timetableId";

    private static final int MAX_TIMETABLE_COUNT = 10;
    private static final int MAX_COURSE_COUNT = 10;

    private final TimetableRepository timetableRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final CustomScheduleRepository customScheduleRepository;
    private final CourseCatalog courseCatalogQuery;
    private final CurrentUserQuery currentUserQuery;

    @Transactional
    public TimetableResponse createTimetable(TimetableRequest request) {
        UserIdentity user = currentUserQuery.requireCurrentUser();
        validateTimetableLimit(user.id());

        if (request.isPrimary()) {
            resetPrimary(user.id());
        }

        Timetable timetable = Timetable.builder()
                .userId(user.id())
                .name(request.getName())
                .isPrimary(request.isPrimary())
                .build();

        return TimetableResponse.from(timetableRepository.save(timetable));
    }

    @EventListener
    @Transactional
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        Timetable timetable = Timetable.builder()
                .userId(event.userId())
                .name("기본 시간표")
                .isPrimary(true)
                .build();
        timetableRepository.save(timetable);
        logTimetableDefaultCreated(event.userId());
    }

    public List<TimetableResponse> getMyTimetables() {
        UserIdentity user = currentUserQuery.requireCurrentUser();
        return timetableRepository.findListItemsByUserId(user.id()).stream()
                .map(TimetableResponse::from)
                .toList();
    }

    public TimetableDetailResponse getPrimaryTimetable() {
        return findPrimaryTimetable().map(this::getTimetableDetail).orElse(null);
    }

    public Optional<PrimaryTimetableSnapshot> findPrimaryForCurrentUser() {
        return findPrimaryTimetable().map(this::toPrimarySnapshot);
    }

    public TimetableDetailResponse getTimetableDetail(Long timetableId) {
        Timetable timetable = timetableRepository.findWithEntriesById(timetableId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 시간표입니다."));
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

        request.getSchedules().forEach(timeRequest -> addCustomScheduleTime(schedule, timeRequest));

        timetable.addCustomSchedule(schedule);
    }

    private void addCustomScheduleTime(CustomSchedule schedule, CustomScheduleTimeRequest timeRequest) {
        CustomScheduleTime time = CustomScheduleTime.builder()
                .customSchedule(schedule)
                .dayOfWeek(timeRequest.getDayOfWeek())
                .startTime(timeRequest.getStartTime())
                .endTime(timeRequest.getEndTime())
                .classroom(timeRequest.getClassroom())
                .build();
        schedule.addTime(time);
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
        logTimetablePrimaryChanged(timetableId);
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

    private Optional<Timetable> findPrimaryTimetable() {
        UserIdentity user = currentUserQuery.requireCurrentUser();
        return timetableRepository.findWithEntriesByUserIdAndIsPrimaryTrue(user.id()).stream().findFirst();
    }

    private TimetableDetailResponse getTimetableDetail(Timetable timetable) {
        List<CourseSummary> courses = findTimetableCourses(timetable);
        List<CustomSchedule> schedules = customScheduleRepository.findWithTimesByTimetableId(timetable.getId());
        return TimetableDetailResponse.from(timetable, courses, schedules, calculateTotalCredits(courses));
    }

    private PrimaryTimetableSnapshot toPrimarySnapshot(Timetable timetable) {
        List<CourseSummary> courses = findTimetableCourses(timetable);
        List<CustomSchedule> schedules = customScheduleRepository.findWithTimesByTimetableId(timetable.getId());
        return TimetableSnapshotMapper.from(timetable, courses, schedules, calculateTotalCredits(courses));
    }

    private List<CourseSummary> findTimetableCourses(Timetable timetable) {
        List<String> courseKeys = timetable.getEntries().stream()
                .map(TimetableEntry::getCourseKey)
                .toList();

        Map<String, CourseSummary> coursesByKey = courseCatalogQuery.findByCourseKeyIn(courseKeys).stream()
                .collect(Collectors.toMap(CourseSummary::courseKey, Function.identity(),
                        (existing, replacement) -> existing));
        return courseKeys.stream()
                .map(coursesByKey::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private String calculateTotalCredits(List<CourseSummary> courses) {
        double totalCredits = courses.stream()
                .map(CourseSummary::credits)
                .mapToDouble(this::toCreditValue)
                .sum();
        return String.valueOf(totalCredits);
    }

    private double toCreditValue(String credits) {
        if (!StringUtils.hasText(credits)) {
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
        if (courseCatalogQuery.existsByCourseKey(courseKey)) {
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
                .ifPresent(this::promotePrimary);
    }

    private void promotePrimary(Timetable timetable) {
        timetable.setPrimary(true);
        logTimetablePrimaryPromoted(timetable.getId());
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
        UserIdentity user = currentUserQuery.requireCurrentUser();
        if (timetable.getUserId().equals(user.id())) {
            return;
        }

        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private void logTimetableDefaultCreated(Long userId) {
        log.atInfo().addKeyValue(EVENT_KEY, "timetable_default_created")
                .addKeyValue("userId", userId)
                .log("[Timetable] 새 사용자 기본 시간표 생성.");
    }

    private void logTimetablePrimaryChanged(Long timetableId) {
        log.atInfo().addKeyValue(EVENT_KEY, "timetable_primary_changed")
                .addKeyValue(TIMETABLE_ID_KEY, timetableId)
                .log("[Timetable] 대표 시간표 변경.");
    }

    private void logTimetablePrimaryPromoted(Long timetableId) {
        log.atInfo().addKeyValue(EVENT_KEY, "timetable_primary_promoted")
                .addKeyValue(TIMETABLE_ID_KEY, timetableId)
                .log("[Timetable] 대표 시간표 자동 변경.");
    }

}
