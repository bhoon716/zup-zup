package bhoon.sugang_helper.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.audit.entity.AdminActionLog;
import bhoon.sugang_helper.audit.enums.ActionType;
import bhoon.sugang_helper.audit.enums.TargetType;
import bhoon.sugang_helper.audit.repository.AdminActionLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
        // given
        String beforeContent = "비밀 \"답변\"\nemail=alice@example.com\nBearer token-123";
        String afterContent = "새 \"답변\"\n유니코드 😀";

        // when
        adminAuditService.recordReplyUpdated(ADMIN_ID, REPLY_ID, FEEDBACK_ID, beforeContent, afterContent);

        // then
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
        // when
        adminAuditService.recordStatusChange(ADMIN_ID, FEEDBACK_ID, "PENDING", "COMPLETED");
        adminAuditService.recordReplyCreated(ADMIN_ID, REPLY_ID, FEEDBACK_ID, "답변");
        adminAuditService.recordReplyDeleted(ADMIN_ID, REPLY_ID, FEEDBACK_ID, "답변");
        adminAuditService.recordAttachmentAccess(ADMIN_ID, 30L, FEEDBACK_ID);
        adminAuditService.recordDeliveryReplay(ADMIN_ID, 40L, "DLQ", 2);

        // then
        ArgumentCaptor<AdminActionLog> captor = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(adminActionLogRepository, Mockito.times(5)).save(captor.capture());
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
}
