package bhoon.sugang_helper.course.application;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.course.domain.Course;
import bhoon.sugang_helper.course.domain.CourseSearchCriteria;
import bhoon.sugang_helper.course.domain.CourseRepository;
import bhoon.sugang_helper.course.domain.CourseSeatHistoryRepository;
import bhoon.sugang_helper.course.application.CourseSearchCondition;
import bhoon.sugang_helper.course.application.CourseCategoryResponse;
import bhoon.sugang_helper.course.application.CourseDetailResponse;
import bhoon.sugang_helper.course.application.CourseResponse;
import bhoon.sugang_helper.course.application.CourseSeatHistoryResponse;
import bhoon.sugang_helper.crawling.application.CrawlTargetInfo;
import bhoon.sugang_helper.crawling.application.CourseCrawlerTargetService;
import bhoon.sugang_helper.review.domain.ReviewScopeKey;
import bhoon.sugang_helper.review.domain.CourseReviewRepository;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.util.List;
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
    private final CourseReviewRepository reviewRepository;

    private final CourseCrawlerTargetService crawlerTargetService;

    /**
     * 필터 조건과 페이징 정보를 사용하여 강의 목록을 검색
     */
    public Slice<CourseResponse> searchCourses(CourseSearchCondition condition, Pageable pageable) {
        CrawlTargetInfo target = crawlerTargetService.getCurrentTargetValue();
        Long userId = condition.getUserId();
        if (Boolean.TRUE.equals(condition.getIsWishedOnly())) {
            String email = SecurityUtil.getCurrentUserEmail();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
            userId = user.getId();
        }
        CourseSearchCriteria criteria = condition.toCriteria(userId);
        return courseRepository.searchCourses(criteria, pageable)
                .map(course -> CourseResponse.from(course, target.year(), target.semester().getCode()));
    }

    /**
     * 특정 강의의 여석 변화 이력을 조회
     */
    public List<CourseSeatHistoryResponse> getCourseHistory(String courseKey) {
        return courseSeatHistoryRepository.findByCourseKeyOrderByCreatedAtDesc(courseKey)
                .stream()
                .map(CourseSeatHistoryResponse::from)
                .toList();
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
        CrawlTargetInfo target = crawlerTargetService.getCurrentTargetValue();
        boolean isReviewed = false;
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email != null) {
            isReviewed = userRepository.findByEmail(email)
                    .map(user -> {
                        ReviewScopeKey scope = ReviewScopeKey.from(course);
                        return reviewRepository.countBySubjectCodeAndProfessorAndUserId(
                                scope.subjectCode(), scope.professor(), user.getId()) > 0;
                    })
                    .orElse(false);
        }

        return CourseDetailResponse.from(course, target.year(), target.semester().getCode(), isReviewed);
    }
}
