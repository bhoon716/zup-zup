package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.repository.CourseSeatHistoryRepository;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import bhoon.sugang_helper.domain.course.response.CourseCategoryResponse;
import bhoon.sugang_helper.domain.course.response.CourseDetailResponse;
import bhoon.sugang_helper.domain.course.response.CourseResponse;
import bhoon.sugang_helper.domain.course.response.CourseSeatHistoryResponse;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseSeatHistoryRepository courseSeatHistoryRepository;
    private final UserRepository userRepository;
    private final CourseCrawlerTargetService crawlerTargetService;

    /**
     * 필터 조건과 페이징 정보를 사용하여 강의 목록을 검색
     */
    public Slice<CourseResponse> searchCourses(CourseSearchCondition condition, Pageable pageable) {
        CourseCrawlerTargetService.CrawlTarget target = crawlerTargetService.getCurrentTargetValue();
        if (Boolean.TRUE.equals(condition.getIsWishedOnly())) {
            String email = SecurityUtil.getCurrentUserEmail();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
            condition.setUserId(user.getId());
        }
        return courseRepository.searchCourses(condition, pageable)
                .map(course -> CourseResponse.from(course, target.year(), target.semester()));
    }

    /**
     * 특정 강의의 여석 변화 이력을 조회
     */
    public List<CourseSeatHistoryResponse> getCourseHistory(String courseKey) {
        return courseSeatHistoryRepository.findByCourseKeyOrderByCreatedAtDesc(courseKey)
                .stream()
                .map(CourseSeatHistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 존재하는 모든 교양 영역 및 상세 카테고리 목록을 조회
     */
    public List<CourseCategoryResponse> getCourseCategories() {
        return courseRepository.findDistinctGeneralCategories()
                .stream()
                .map(category -> CourseCategoryResponse.builder()
                        .category(category)
                        .details(courseRepository.findDistinctGeneralDetailsByCategory(category))
                        .build())
                .toList();
    }

    /**
     * 특정 강의의 상세 정보를 조회
     */
    public CourseDetailResponse getCourse(String courseKey) {
        Course course = courseRepository.findByCourseKey(courseKey)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "강의를 찾을 수 없습니다: " + courseKey));
        CourseCrawlerTargetService.CrawlTarget target = crawlerTargetService.getCurrentTargetValue();
        return CourseDetailResponse.from(course, target.year(), target.semester());
    }
}
