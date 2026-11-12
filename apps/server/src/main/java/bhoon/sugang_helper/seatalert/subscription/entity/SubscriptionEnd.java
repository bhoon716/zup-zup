package bhoon.sugang_helper.seatalert.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription_ends")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionEnd {

    @EmbeddedId
    private Key id;

    public SubscriptionEnd(Key id) {
        this.id = id;
    }

    @Embeddable
    @AllArgsConstructor
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {

        @Column(nullable = false)
        private Long userId;

        @Column(nullable = false, length = 64)
        private String courseKey;
    }
}
