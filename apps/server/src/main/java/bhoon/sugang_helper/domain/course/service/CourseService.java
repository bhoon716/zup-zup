package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import bhoon.sugang_helper.domain.course.response.CourseResponse;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import bhoon.sugang_helper.domain.course.response.CourseSeatHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseSeatHistoryRepository courseSeatHistoryRepository;

    public Page<CourseResponse> searchCourses(CourseSearchCondition condition, Pageable pageable) {
        return courseRepository.searchCourses(condition, pageable)
                .map(CourseResponse::from);
    }

    public List<CourseSeatHistoryResponse> getCourseHistory(String courseKey) {
        return courseSeatHistoryRepository.findByCourseKeyOrderByCreatedAtDesc(courseKey)
                .stream()
                .map(CourseSeatHistoryResponse::from)
                .collect(Collectors.toList());
    }

    public CourseResponse getCourse(String courseKey) {
        return courseRepository.findById(courseKey)
                .map(CourseResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과목입니다."));
    }
}
