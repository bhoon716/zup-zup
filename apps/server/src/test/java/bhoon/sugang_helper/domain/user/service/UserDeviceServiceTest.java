package bhoon.sugang_helper.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.entity.UserDevice;
import bhoon.sugang_helper.domain.user.enums.DeviceType;
import bhoon.sugang_helper.domain.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.user.request.UserDeviceRequest;
import bhoon.sugang_helper.domain.user.response.UserDeviceResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @InjectMocks
    private UserDeviceService userDeviceService;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private UserRepository userRepository;

    private static MockedStatic<SecurityUtil> securityUtil;

    private User testUser;
    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        securityUtil = mockStatic(SecurityUtil.class);
        testUser = mock(User.class);
        lenient().when(testUser.getId()).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    private void mockUser() {
        given(SecurityUtil.getCurrentUserEmail()).willReturn(testEmail);
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("디바이스 등록 - 신규 등록 성공")
    void registerDevice_New_Success() {
        // given
        mockUser();
        UserDeviceRequest request = UserDeviceRequest.builder()
                .token("TOKEN")
                .p256dh("P256DH")
                .auth("AUTH")
                .type(DeviceType.WEB)
                .alias("Alias")
                .build();
        given(userDeviceRepository.findByToken("TOKEN")).willReturn(Optional.empty());

        // when
        userDeviceService.registerDevice(request);

        // then
        verify(userDeviceRepository, times(1)).save(any(UserDevice.class));
    }

    @Test
    @DisplayName("디바이스 등록 - 기존 디바이스 정보 업데이트")
    void registerDevice_Update_Success() {
        // given
        mockUser();
        UserDeviceRequest request = UserDeviceRequest.builder()
                .token("TOKEN")
                .p256dh("NEW_P256DH")
                .auth("NEW_AUTH")
                .type(DeviceType.WEB)
                .alias("NewAlias")
                .build();
        UserDevice existingDevice = spy(UserDevice.builder()
                .userId(1L)
                .token("TOKEN")
                .p256dh("OLD_P256DH")
                .auth("OLD_AUTH")
                .alias("OldAlias")
                .build());
        given(userDeviceRepository.findByToken("TOKEN")).willReturn(Optional.of(existingDevice));

        // when
        userDeviceService.registerDevice(request);

        // then
        verify(existingDevice, times(1)).updateToken(1L, "TOKEN", "NEW_P256DH", "NEW_AUTH", "NewAlias");
        verify(userDeviceRepository, times(0)).save(any(UserDevice.class));
    }

    @Test
    @DisplayName("디바이스 등록 - 인증되지 않은 사용자 예외 발생")
    void registerDevice_UserNotFound_ThrowsException() {
        // given
        given(SecurityUtil.getCurrentUserEmail()).willReturn(testEmail);
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.empty());
        UserDeviceRequest request = UserDeviceRequest.builder()
                .token("TOKEN")
                .p256dh("P256DH")
                .auth("AUTH")
                .type(DeviceType.WEB)
                .build();

        // when & then
        assertThatThrownBy(() -> userDeviceService.registerDevice(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_UNAUTHORIZED);
    }

    @Test
    @DisplayName("디바이스 해제 - 성공")
    void unregisterDevice_Success() {
        // given
        UserDevice device = mock(UserDevice.class);
        given(userDeviceRepository.findByToken("TOKEN")).willReturn(Optional.of(device));

        // when
        userDeviceService.unregisterDevice("TOKEN");

        // then
        verify(userDeviceRepository, times(1)).delete(device);
    }

    @Test
    @DisplayName("사용자 디바이스 목록 조회 - 성공")
    void getUserDevices_Success() {
        // given
        UserDevice device = UserDevice.builder()
                .userId(1L)
                .type(DeviceType.WEB)
                .token("TOKEN")
                .alias("Alias")
                .build();
        given(userDeviceRepository.findByUserId(1L)).willReturn(Collections.singletonList(device));

        // when
        List<UserDeviceResponse> result = userDeviceService.getUserDevices(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAlias()).isEqualTo("Alias");
    }
}
