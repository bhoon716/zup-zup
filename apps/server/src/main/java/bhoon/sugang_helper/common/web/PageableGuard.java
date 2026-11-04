package bhoon.sugang_helper.common.web;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


public final class PageableGuard {

    private static final Logger log = LoggerFactory.getLogger(PageableGuard.class);

    public static final int PAGE_SIZE = 30;
    public static final int DEFAULT_MAX_PAGE_SIZE = 100;
    public static final long DEFAULT_MAX_OFFSET = 10_000;

    private PageableGuard() {
        throw new IllegalStateException("Utility class");
    }

    public static Pageable fixedSize(Pageable pageable) {
        if (pageable == null || !pageable.isPaged() || pageable.getPageNumber() < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "페이지 번호는 0 이상이어야 합니다.");
        }
        return requireBounded(PageRequest.of(pageable.getPageNumber(), PAGE_SIZE, pageable.getSort()),
                PAGE_SIZE, DEFAULT_MAX_OFFSET);
    }

    public static Pageable requireBounded(Pageable pageable) {
        return requireBounded(pageable, DEFAULT_MAX_PAGE_SIZE, DEFAULT_MAX_OFFSET);
    }

    public static Pageable requireBounded(Pageable pageable, int maxPageSize, long maxOffset) {
        if (pageable == null || !pageable.isPaged() || pageable.getPageNumber() < 0 || pageable.getPageSize() < 1
                || pageable.getPageSize() > maxPageSize || pageable.getOffset() > maxOffset) {
            log.debug("[API] 페이지 범위를 벗어난 요청 거절. reason=PAGEABLE_LIMIT");
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "페이지 크기는 1~" + maxPageSize + ", offset은 " + maxOffset + " 이하로 요청해주세요.");
        }
        return pageable;
    }
}
