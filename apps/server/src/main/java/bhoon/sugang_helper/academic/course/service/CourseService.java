package bhoon.sugang_helper.academic.course.service;

import bhoon.sugang_helper.academic.review.service.CourseReviewQueryService;
import bhoon.sugang_helper.academic.crawling.service.CrawlerCourseSearchContextProvider;
import bhoon.sugang_helper.academic.course.dto.request.CourseSearchCondition;
import bhoon.sugang_helper.academic.course.dto.result.CourseSearchContext;
import bhoon.sugang_helper.academic.course.dto.criteria.CourseSearchCriteria;
import bhoon.sugang_helper.academic.course.dto.response.CourseCategoryResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseDetailResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseResponse;
import bhoon.sugang_helper.academic.course.dto.response.CourseSeatHistoryResponse;
import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.course.repository.CourseSearchReadRepository;
import bhoon.sugang_helper.academic.course.repository.CourseSeatHistoryRepository;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.web.PageableGuard;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final CurrentUserQuery currentUserQuery;
    private final CourseReviewQueryService courseReviewQuery;

    private final CrawlerCourseSearchContextProvider searchContextProvider;

    public Slice<CourseResponse> searchCourses(CourseSearchCondition condition, Pageable pageable) {
        condition.validateSearchValues();
        CourseSearchContext context = searchContextProvider.getCurrentContext();
        Long userId = condition.getUserId();
        if (Boolean.TRUE.equals(condition.getIsWishedOnly())) {
            userId = currentUserQuery.requireCurrentUser().id();
        }
        CourseSearchCriteria criteria = condition.toCriteria(userId);
        return courseSearchReadRepository.searchCourses(criteria, PageableGuard.requireBounded(pageable))
                .map(course -> CourseResponse.from(
                        course, context.year(), context.semester().getCode(), context.lastCrawledAt()));
    }

    public Slice<CourseSeatHistoryResponse> getCourseHistory(String courseKey, Pageable pageable) {
        PageableGuard.requireBounded(pageable, MAX_HISTORY_PAGE_SIZE, PageableGuard.DEFAULT_MAX_OFFSET);
        Pageable boundedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return courseSeatHistoryRepository.findByCourseKeyOrderByCreatedAtDescIdDesc(courseKey, boundedPageable)
                .map(CourseSeatHistoryResponse::from);
    }

    public List<CourseCategoryResponse> getCourseCategories() {
        Map<String, List<String>> detailsByCategory = new LinkedHashMap<>();
        for (var row : courseRepository.findGeneralCategoryDetails()) {
            List<String> details = detailsByCategory.computeIfAbsent(row.getCategory(), category -> new ArrayList<>());
            if (row.getDetail() != null) {
                details.add(row.getDetail());
            }
        }
        return detailsByCategory.entrySet().stream()
                .map(entry -> CourseCategoryResponse.of(entry.getKey(), entry.getValue()))
                .toList();
    }

    public CourseDetailResponse getCourse(String courseKey) {
        Course course = courseRepository.findWithSchedulesByCourseKey(courseKey)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "강의를 찾을 수 없습니다: " + courseKey));
        CourseSearchContext context = searchContextProvider.getCurrentContext();
        boolean isReviewed = currentUserQuery.findCurrentUser()
                .map(user -> courseReviewQuery.hasReviewByUser(
                        course.getCourseKey(), course.getSubjectCode(), course.getProfessor(), user.id()))
                .orElse(false);

        return CourseDetailResponse.from(
                course, context.year(), context.semester().getCode(), isReviewed, context.lastCrawledAt());
    }
}
