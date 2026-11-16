import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import * as courseApi from '@/features/course/api/course.api';
import type { CourseSearchCondition, Course, SearchDefaultSemesterResponse, CourseSearchPageResponse } from '@/shared/types/api';
import { normalizeCourse } from '@/shared/lib/course';
import { useAuthStore } from '@/features/auth/store/useAuthStore';
import type { PublicPageData } from '@/shared/lib/public-metadata';

const COURSE_SEARCH_STALE_TIME_MS = 60_000;
const INITIAL_COURSE_SEARCH_STALE_TIME_MS = 5 * 60 * 1000;

export const useCourses = (
  condition: CourseSearchCondition,
  options?: {
    enabled?: boolean;
    initialPage?: CourseSearchPageResponse;
  },
) => {
  const userId = useAuthStore((state) => state.isAuthenticated ? state.user?.id : undefined);
  const queryKey = condition.isWishedOnly ? ['courses', condition, userId] : ['courses', condition];
  let staleTime = COURSE_SEARCH_STALE_TIME_MS;
  if (options?.initialPage) {
    staleTime = INITIAL_COURSE_SEARCH_STALE_TIME_MS;
  }
  if (condition.isWishedOnly) {
    staleTime = 0;
  }
  return useInfiniteQuery({
    queryKey,
    enabled: (options?.enabled ?? true) && (!condition.isWishedOnly || userId !== undefined),
    queryFn: async ({ pageParam = 0 }) => {
      const response = await courseApi.searchCourses(condition, pageParam as number);
      const sliceData = response;

      let courses: Course[] = [];
      if ('content' in sliceData) {
        courses = sliceData.content;
      } else if (Array.isArray(sliceData)) {
        courses = sliceData;
      }

      const normalizedCourses = courses.map(course => normalizeCourse(course));

      return {
          content: normalizedCourses,
          last: 'last' in sliceData ? sliceData.last : true, 
          number: 'number' in sliceData ? sliceData.number : pageParam
      };
    },
    initialData: options?.initialPage
      ? {
          pages: [
            {
              ...options.initialPage,
              content: options.initialPage.content.map((course) => normalizeCourse(course)),
            },
          ],
          pageParams: [0],
        }
      : undefined,
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined;
      return (lastPage.number as number) + 1;
    },
    initialPageParam: 0,
    staleTime,
  });
};

export const useCourseDetail = (courseKey: string, initialPublicCourse?: PublicPageData<Course> | null) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const hasCheckedSession = useAuthStore((state) => state.hasCheckedSession);
  const publicSeed = !isAuthenticated && initialPublicCourse?.data.courseKey === courseKey
    ? initialPublicCourse
    : undefined;

  return useQuery({
    queryKey: [publicSeed ? 'public-course-detail' : 'course-detail', courseKey],
    queryFn: async () => {
      const response = await courseApi.getCourseDetail(courseKey);
      const course = response;
      if (!course) return null;

      return normalizeCourse(publicSeed ? { ...course, isReviewed: false } : course);
    },
    initialData: publicSeed && normalizeCourse({ ...publicSeed.data, isReviewed: false }),
    initialDataUpdatedAt: publicSeed?.updatedAt,
    enabled: !!courseKey && (!publicSeed || hasCheckedSession),
  });
};

export const useSearchDefaultSemester = () => {
  return useQuery<SearchDefaultSemesterResponse>({
    queryKey: ['courses', 'search-default-semester'],
    queryFn: async () => {
      const response = await courseApi.getSearchDefaultSemester();
      return response;
    },
    staleTime: 1000 * 60 * 60,
  });
};
