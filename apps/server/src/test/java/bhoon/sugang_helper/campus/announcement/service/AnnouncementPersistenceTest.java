package bhoon.sugang_helper.campus.announcement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import bhoon.sugang_helper.campus.announcement.dto.request.AnnouncementRequest;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.campus.announcement.enums.AnnouncementSearchType;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.seatalert.api.NotificationPublications;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import({AnnouncementService.class, SimpleMeterRegistry.class})
class AnnouncementPersistenceTest {

    private static final String KEYWORD = "수강";
    private static final String TITLE = "수강 공지";
    private static final String BASE_FIELDS = """
            "title": "수강 공지", "content": "수강 신청 안내"
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private AnnouncementService service;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private NotificationPublications publications;

    @Test
    @DisplayName("기본 공개 공지는 알림을 보내지 않고 이후 알림을 선택해도 발송하지 않는다")
    void publicCreationWithoutOptInCannotSendLater() throws JsonProcessingException {
        // given
        AnnouncementDetailResponse created = service.createAnnouncement(request(BASE_FIELDS));
        flushAndClear();

        // when
        AnnouncementDetailResponse edited = service.updateAnnouncement(created.getId(),
                request(BASE_FIELDS + ", \"notify\": true"));
        flushAndClear();

        // then
        assertThat(created.getFirstPublishedAt()).isNotNull();
        assertThat(edited.getFirstPublishedAt()).isEqualTo(service.getPublicAnnouncement(created.getId())
                .getFirstPublishedAt());
        verifyNoInteractions(publications);
    }

    @Test
    @DisplayName("공지 알림은 처음 공개할 때 한 번 발행하며 비공개·삭제·복구는 접근 가능 여부만 바꾼다")
    void announcementPublishesOnceAcrossVisibilityChanges() throws JsonProcessingException {
        // given
        AnnouncementRequest privateRequest = request(BASE_FIELDS + ", \"published\": false, \"notify\": true");
        AnnouncementRequest publicRequest = request(BASE_FIELDS + ", \"published\": true, \"notify\": true");
        AnnouncementDetailResponse draft = service.createAnnouncement(privateRequest);
        Long id = draft.getId();
        flushAndClear();

        // when
        AnnouncementDetailResponse published = service.updateAnnouncement(id, publicRequest);
        flushAndClear();
        service.updateAnnouncement(id, privateRequest);
        flushAndClear();
        service.updateAnnouncement(id, publicRequest);
        flushAndClear();
        service.deleteAnnouncement(id);
        flushAndClear();

        // then
        assertThat(draft.getFirstPublishedAt()).isNull();
        assertThat(published.getFirstPublishedAt()).isNotNull();
        assertThat(service.getAdminAnnouncements()).singleElement().satisfies(deleted -> {
            assertThat(deleted.getDeletedAt()).isNotNull();
            assertThat(deleted.isPublished()).isTrue();
        });
        assertThatThrownBy(() -> service.getPublicAnnouncement(id))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
        assertThat(service.getPublicAnnouncements(null, null)).isEmpty();
        for (AnnouncementSearchType searchType : AnnouncementSearchType.values()) {
            assertThat(service.getPublicAnnouncements(KEYWORD, searchType)).isEmpty();
        }
        assertThat(service.findDashboardAnnouncements(3)).isEmpty();

        AnnouncementDetailResponse restored = service.restoreAnnouncement(id);
        flushAndClear();
        assertThat(restored.getDeletedAt()).isNull();
        assertThat(restored.getFirstPublishedAt()).isEqualTo(service.getPublicAnnouncement(id).getFirstPublishedAt());
        assertThat(service.getPublicAnnouncement(id).getTitle()).isEqualTo(TITLE);
        verify(publications).publishAnnouncement(id, TITLE);
        verify(publications, times(2)).setAnnouncementAvailable(id, false);
        verify(publications, times(2)).setAnnouncementAvailable(id, true);
        verifyNoMoreInteractions(publications);
    }

    private AnnouncementRequest request(String fields) throws JsonProcessingException {
        return mapper.readValue("{" + fields + "}", AnnouncementRequest.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
