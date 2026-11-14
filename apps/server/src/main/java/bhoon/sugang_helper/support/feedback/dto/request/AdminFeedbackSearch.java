package bhoon.sugang_helper.support.feedback.dto.request;

import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackSearchType;

public record AdminFeedbackSearch(String keyword, AdminFeedbackSearchType type) {

    public AdminFeedbackSearch {
        keyword = keyword == null ? "" : keyword.strip();
    }
}
