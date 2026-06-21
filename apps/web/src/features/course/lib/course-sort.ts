export type CourseSortOption = "name" | "popular" | "rating" | "current" | "available";
export type CourseSortOrder = "asc" | "desc";

const DEFAULT_SORT_ORDER: Record<CourseSortOption, CourseSortOrder> = {
  name: "asc",
  popular: "desc",
  rating: "desc",
  current: "desc",
  available: "desc",
};

export function getDefaultCourseSortOrder(sortBy: CourseSortOption): CourseSortOrder {
  return DEFAULT_SORT_ORDER[sortBy];
}
