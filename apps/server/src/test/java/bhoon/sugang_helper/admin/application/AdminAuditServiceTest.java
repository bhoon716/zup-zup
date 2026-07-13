package bhoon.sugang_helper.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.feedback.domain.ActionType;
import bhoon.sugang_helper.feedback.domain.AdminActionLog;
import bhoon.sugang_helper.feedback.domain.AdminActionLogRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.feedback.domain.TargetType;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.user.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Audit-envelope assertions intentionally repeat stable contract values.
class AdminAuditServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long FEEDBACK_ID = 10L;
    private static final long REPLY_ID = 20L;

    @Mock
    private AdminActionLogRepository adminActionLogRepository;
    @Captor
    private ArgumentCaptor<AdminActionLog> actionLogCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminAuditContentFingerprint contentFingerprint = new AdminAuditContentFingerprint("test-secret");
    private AdminAuditService adminAuditService;

    @BeforeEach
    void setUp() {
        adminAuditService = new AdminAuditService(adminActionLogRepository, objectMapper, contentFingerprint);
    }

    @Test
    @DisplayName("따옴표·개행·Unicode가 있는 답변 변경도 원문 없이 유효한 구조화 감사 로그가 된다.")
    void recordReplyUpdate_usesEscapedStructuredMetadataWithoutRawContent() throws Exception {
        User admin = User.builder().id(ADMIN_ID).build();
        String beforeContent = "비밀 \"답변\"\nemail=alice@example.com\nBearer token-123";
        String afterContent = "새 \"답변\"\n유니코드 😀";

        adminAuditService.recordReplyUpdated(admin, REPLY_ID, FEEDBACK_ID, beforeContent, afterContent);

        verify(adminActionLogRepository).save(actionLogCaptor.capture());
        AdminActionLog savedLog = actionLogCaptor.getValue();
        JsonNode metadata = objectMapper.readTree(savedLog.getMetaData());

        assertThat(savedLog.getActionType()).isEqualTo(ActionType.REPLY_UPDATE);
        assertThat(savedLog.getTargetType()).isEqualTo(TargetType.REPLY);
        assertThat(savedLog.getTargetId()).isEqualTo(REPLY_ID);
        assertThat(metadata.path("schema").asText()).isEqualTo("admin-action");
        assertThat(metadata.path("version").asInt()).isEqualTo(1);
        assertThat(metadata.path("event").asText()).isEqualTo("REPLY_UPDATED");
        assertThat(metadata.path("data").path("feedbackId").asLong()).isEqualTo(FEEDBACK_ID);
        assertThat(metadata.path("data").path("beforeContent").path("length").asInt())
                .isEqualTo(beforeContent.length());
        assertThat(metadata.path("data").path("afterContent").path("length").asInt())
                .isEqualTo(afterContent.length());
        assertThat(metadata.path("data").path("beforeContent").path("fingerprint").asText())
                .isEqualTo(contentFingerprint.summarize(beforeContent).fingerprint());
        assertThat(metadata.path("data").path("afterContent").path("fingerprint").asText())
                .isEqualTo(contentFingerprint.summarize(afterContent).fingerprint());
        assertThat(metadata.toString())
                .doesNotContain(beforeContent)
                .doesNotContain(afterContent)
                .doesNotContain("alice@example.com")
                .doesNotContain("token-123");
    }

    @Test
    @DisplayName("상태·답변·첨부파일 접근·향후 DLQ 재처리가 같은 감사 모델로 저장된다.")
    void recordSensitiveAdminActions_usesTheSharedAuditModel() throws Exception {
        User admin = User.builder().id(ADMIN_ID).email("admin@example.com").build();

        adminAuditService.recordStatusChange(admin, FEEDBACK_ID, FeedbackStatus.PENDING, FeedbackStatus.COMPLETED);
        adminAuditService.recordReplyCreated(admin, REPLY_ID, FEEDBACK_ID, "답변");
        adminAuditService.recordReplyDeleted(admin, REPLY_ID, FEEDBACK_ID, "답변");
        adminAuditService.recordAttachmentAccess(admin, 30L, FEEDBACK_ID);
        adminAuditService.recordDeliveryReplay(admin, 40L, SeatNotificationDeliveryStatus.DLQ, 2);

        ArgumentCaptor<AdminActionLog> captor = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(adminActionLogRepository, org.mockito.Mockito.times(5)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AdminActionLog::getActionType)
                .containsExactly(
                        ActionType.STATUS_CHANGE,
                        ActionType.REPLY_CREATE,
                        ActionType.REPLY_DELETE,
                        ActionType.ATTACHMENT_ACCESS,
                        ActionType.DELIVERY_REPLAY);
        for (AdminActionLog actionLog : captor.getAllValues()) {
            JsonNode metadata = objectMapper.readTree(actionLog.getMetaData());
            assertThat(metadata.path("schema").asText()).isEqualTo("admin-action");
            assertThat(metadata.path("version").asInt()).isEqualTo(1);
            assertThat(metadata.path("data").toString())
                    .doesNotContain("답변")
                    .doesNotContain("secret-idempotency-key");
        }
    }

    @Test
    @DisplayName("감사 로그 목록은 최신순 안정 정렬과 최대 100건 페이지 크기를 사용하고, legacy 원문은 반환하지 않는다.")
    void getActionLogs_boundsPagingAndRedactsUnexpectedLegacyMetadata() {
        User admin = User.builder().id(ADMIN_ID).build();
        AdminActionLog legacyLog = AdminActionLog.builder()
                .admin(admin)
                .actionType(ActionType.REPLY_UPDATE)
                .targetType(TargetType.REPLY)
                .targetId(REPLY_ID)
                .metaData("{\"oldContent\":\"private reply alice@example.com\"}")
                .build();
        ReflectionTestUtils.setField(legacyLog, "id", 99L);
        given(adminActionLogRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(legacyLog), PageRequest.of(3, 100), 301));

        Page<AdminActionLogResponse> response = adminAuditService.getActionLogs(PageRequest.of(3, 100));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminActionLogRepository).findAllByOrderByCreatedAtDescIdDesc(pageableCaptor.capture());
        Pageable requestedPage = pageableCaptor.getValue();
        assertThat(requestedPage.getPageNumber()).isEqualTo(3);
        assertThat(requestedPage.getPageSize()).isEqualTo(100);
        assertThat(requestedPage.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(requestedPage.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(response.getContent()).singleElement().satisfies(log -> {
            assertThat(log.adminId()).isEqualTo(ADMIN_ID);
            assertThat(log.metadata().event()).isEqualTo("LEGACY_SANITIZED");
            assertThat(log.metadata().data().reason()).isEqualTo("legacy_metadata_removed");
            assertThat(log.toString())
                    .doesNotContain("private reply")
                    .doesNotContain("alice@example.com")
                    .doesNotContain("admin@example.com");
        });
    }
}
