package bhoon.sugang_helper.common.web;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Consistent bounds for every pageable endpoint to protect DB offset and response memory. */
public final class PageableGuard {

    private static final Logger log = LoggerFactory.getLogger(PageableGuard.class);

    public static final int DEFAULT_MAX_PAGE_SIZE = 100;
    public static final long DEFAULT_MAX_OFFSET = 10_000;

    private PageableGuard() {
        throw new IllegalStateException("Utility class");
    }

    public static Pageable requireBounded(Pageable pageable) {
        return requireBounded(pageable, DEFAULT_MAX_PAGE_SIZE, DEFAULT_MAX_OFFSET);
    }

    public static Pageable requireBounded(Pageable pageable, int maxPageSize, long maxOffset) {
        if (pageable == null || pageable.getPageNumber() < 0 || pageable.getPageSize() < 1
                || pageable.getPageSize() > maxPageSize || pageable.getOffset() > maxOffset) {
            log.warn("[API] Pageable request rejected. reason=PAGEABLE_LIMIT");
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "페이지 크기는 1~" + maxPageSize + ", offset은 " + maxOffset + " 이하로 요청해주세요.");
        }
        return pageable;
    }
}
