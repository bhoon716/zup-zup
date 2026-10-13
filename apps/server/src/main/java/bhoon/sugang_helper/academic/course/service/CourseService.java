package bhoon.sugang_helper.academic.course.service;

import bhoon.sugang_helper.academic.course.dto.CourseSearchCondition;
import bhoon.sugang_helper.academic.course.dto.CourseSearchContext;
import bhoon.sugang_helper.academic.course.dto.CourseSearchCriteria;
import bhoon.sugang_helper.academic.course.dto.response.CourseCategoryResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseDetailResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseSeatHistoryResponse;
import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.course.repository.CourseSearchReadRepository;
import bhoon.sugang_helper.academic.course.repository.CourseSeatHistoryRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.identity.user.dto.UserIdentity;
import bhoon.sugang_helper.identity.user.service.UserIdentityQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private static final int MAX_HISTORY_PAGE_SIZE = 100;

    private final CourseRepository courseRepository;
    private final CourseSearchReadRepository courseSearchReadRepository;
    private final CourseSeatHistoryRepository courseSeatHistoryRepository;
    private final UserIdentityQuery userIdentityQuery;
    private final CourseReviewQuery courseReviewQuery;

    private final CourseSearchContextProvider searchContextProvider;

    /**
     * 필터 조건과 페이징 정보를 사용하여 강의 목록을 검색
     */
    public Slice<CourseResponse> searchCourses(CourseSearchCondition condition, Pageable pageable) {
        condition.validateSearchValues();
        CourseSearchContext context = searchContextProvider.getCurrentContext();
        Long userId = condition.getUserId();
        if (Boolean.TRUE.equals(condition.getIsWishedOnly())) {
            String email = SecurityUtil.getCurrentUserEmail();
            UserIdentity user = userIdentityQuery.findByEmail(email)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
            userId = user.id();
        }
        CourseSearchCriteria criteria = condition.toCriteria(userId);
        return courseSearchReadRepository.searchCourses(criteria, PageableGuard.requireBounded(pageable))
                .map(course -> CourseResponse.from(
                        course, context.year(), context.semester().getCode(), context.lastCrawledAt()));
    }

    /**
     * 특정 강의의 여석 변화 이력을 조회
     */
    public Slice<CourseSeatHistoryResponse> getCourseHistory(String courseKey, Pageable pageable) {
        PageableGuard.requireBounded(pageable, MAX_HISTORY_PAGE_SIZE, 10_000);
        Pageable boundedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return courseSeatHistoryRepository.findByCourseKeyOrderByCreatedAtDescIdDesc(courseKey, boundedPageable)
                .map(CourseSeatHistoryResponse::from);
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
        CourseSearchContext context = searchContextProvider.getCurrentContext();
        boolean isReviewed = false;
        String email = SecurityUtil.getCurrentUserEmailOrNull();
        if (email != null) {
            isReviewed = userIdentityQuery.findByEmail(email)
                    .map(user -> {
                        return courseReviewQuery.hasReviewByUser(
                                course.getCourseKey(), course.getSubjectCode(), course.getProfessor(), user.id());
                    })
                    .orElse(false);
        }

        return CourseDetailResponse.from(
                course, context.year(), context.semester().getCode(), isReviewed, context.lastCrawledAt());
    }
}
