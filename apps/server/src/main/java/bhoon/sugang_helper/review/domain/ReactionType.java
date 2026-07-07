package bhoon.sugang_helper.review.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReactionType {
    LIKE("공감"),
    DISLIKE("비공감");

    private final String description;
}
