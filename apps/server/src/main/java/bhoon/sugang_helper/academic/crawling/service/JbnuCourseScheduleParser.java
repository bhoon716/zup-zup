package bhoon.sugang_helper.academic.crawling.service;

import bhoon.sugang_helper.academic.course.dto.external.ParsedCourseSchedule;
import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class JbnuCourseScheduleParser {

    private static final int FIRST_PERIOD_HOUR = 8;
    private static final int LAST_PERIOD_SLOT = 15;
    private static final int HALF_PERIOD_MINUTES = 30;

    private static final String FIRST_HALF = "A";
    private static final String SECOND_HALF = "B";
    private static final Pattern HALF_PERIOD_PATTERN = Pattern.compile("^(\\d{1,2})-?([ABab])$");
    private static final Pattern HALF_PERIOD_RANGE_PATTERN = Pattern.compile(
            "^(\\d{1,2})([ABab])\\s*[-~]\\s*(\\d{1,2})([ABab])$");
    private static final Pattern PERIOD_RANGE_PATTERN = Pattern.compile("^(\\d{1,2})\\s*[-~]\\s*(\\d{1,2})$");
    private static final Pattern CLOCK_RANGE_PATTERN = Pattern.compile(
            "^(\\d{1,2}:\\d{2})\\s*[-~]\\s*(\\d{1,2}:\\d{2})$");

    private JbnuCourseScheduleParser() {
    }

    static List<ParsedCourseSchedule> parse(String classTime) {
        List<ParsedCourseSchedule> schedules = new ArrayList<>();
        if (!StringUtils.hasText(classTime)) {
            return schedules;
        }

        for (String token : classTime.split(",")) {
            ParsedCourseSchedule schedule = parseSchedule(token.trim());
            if (schedule != null) {
                schedules.add(schedule);
            }
        }
        return mergeConsecutiveSchedules(schedules);
    }

    private static ParsedCourseSchedule parseSchedule(String token) {
        String[] parts = token.split("\\s+");
        if (parts.length < 2) {
            return null;
        }

        CourseDayOfWeek day = CourseDayOfWeek.from(parts[0]);
        TimeRange timeRange = parseTimeRange(parts[1].trim());
        if (day == null || timeRange == null) {
            return null;
        }
        return new ParsedCourseSchedule(day, timeRange.start(), timeRange.end());
    }

    private static TimeRange parseTimeRange(String token) {
        Matcher halfPeriod = HALF_PERIOD_PATTERN.matcher(token);
        if (halfPeriod.matches()) {
            return halfPeriodTimeRange(Integer.parseInt(halfPeriod.group(1)), halfPeriod.group(2));
        }

        Matcher halfPeriodRange = HALF_PERIOD_RANGE_PATTERN.matcher(token);
        if (halfPeriodRange.matches()) {
            return periodTimeRange(Integer.parseInt(halfPeriodRange.group(1)), halfPeriodRange.group(2),
                    Integer.parseInt(halfPeriodRange.group(3)), halfPeriodRange.group(4));
        }

        Matcher periodRange = PERIOD_RANGE_PATTERN.matcher(token);
        if (periodRange.matches()) {
            return periodTimeRange(Integer.parseInt(periodRange.group(1)), FIRST_HALF,
                    Integer.parseInt(periodRange.group(2)), SECOND_HALF);
        }

        Matcher clockRange = CLOCK_RANGE_PATTERN.matcher(token);
        if (!clockRange.matches()) {
            return null;
        }
        try {
            return new TimeRange(LocalTime.parse(clockRange.group(1)), LocalTime.parse(clockRange.group(2)));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static TimeRange periodTimeRange(int startSlot, String startHalf, int endSlot, String endHalf) {
        TimeRange start = halfPeriodTimeRange(startSlot, startHalf);
        TimeRange end = halfPeriodTimeRange(endSlot, endHalf);
        if (start == null || end == null) {
            return null;
        }
        return new TimeRange(start.start(), end.end());
    }

    private static TimeRange halfPeriodTimeRange(int slot, String half) {
        if (slot < 0 || slot > LAST_PERIOD_SLOT) {
            return null;
        }

        int hour = FIRST_PERIOD_HOUR + slot;
        if (FIRST_HALF.equalsIgnoreCase(half)) {
            return new TimeRange(LocalTime.of(hour, 0), LocalTime.of(hour, HALF_PERIOD_MINUTES));
        }
        if (slot == LAST_PERIOD_SLOT) {
            return new TimeRange(LocalTime.of(hour, HALF_PERIOD_MINUTES), LocalTime.of(hour, 59));
        }
        return new TimeRange(LocalTime.of(hour, HALF_PERIOD_MINUTES), LocalTime.of(hour + 1, 0));
    }

    private static List<ParsedCourseSchedule> mergeConsecutiveSchedules(List<ParsedCourseSchedule> schedules) {
        if (schedules.isEmpty()) {
            return schedules;
        }

        schedules.sort(Comparator.comparing(ParsedCourseSchedule::dayOfWeek)
                .thenComparing(ParsedCourseSchedule::startTime));
        List<ParsedCourseSchedule> merged = new ArrayList<>();
        ParsedCourseSchedule current = schedules.getFirst();

        for (int i = 1; i < schedules.size(); i++) {
            ParsedCourseSchedule next = schedules.get(i);
            if (current.dayOfWeek() == next.dayOfWeek() && current.endTime().equals(next.startTime())) {
                current = new ParsedCourseSchedule(current.dayOfWeek(), current.startTime(), next.endTime());
                continue;
            }
            merged.add(current);
            current = next;
        }

        merged.add(current);
        return merged;
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
