package bhoon.sugang_helper.academic.crawling.domain;

public enum CrawlerFailureStage {
    FETCH_PARSE,
    PERSIST,
    JUMP_BOOTSTRAP,
    COURSE_API
}
