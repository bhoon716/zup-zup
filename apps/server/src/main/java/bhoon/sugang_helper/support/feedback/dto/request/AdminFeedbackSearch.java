package bhoon.sugang_helper.support.feedback.dto.request;

import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackSearchType;

public record AdminFeedbackSearch(String keyword, AdminFeedbackSearchType type) {

    public AdminFeedbackSearch(String keyword, AdminFeedbackSearchType type) {
        this.keyword = keyword == null ? "" : keyword.strip();
        this.type = type;
    }
}
