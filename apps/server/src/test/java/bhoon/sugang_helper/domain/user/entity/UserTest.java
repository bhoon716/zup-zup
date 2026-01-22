package bhoon.sugang_helper.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("User 생성 테스트")
    void createUser() {
        // given
        String name = "test";
        String email = "test@example.com";
        Role role = Role.USER;

        // when
        User user = User.builder()
                .name(name)
                .email(email)
                .role(role)
                .build();

        // then
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getRole()).isEqualTo(role);
    }

    @Test
    @DisplayName("User 정보 수정 테스트")
    void updateUser() {
        // given
        User user = User.builder()
                .name("test")
                .email("test@example.com")
                .role(Role.USER)
                .build();
        String newName = "updated";

        // when
        user.update(newName);

        // then
        assertThat(user.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("Role Key 조회 테스트")
    void getRoleKey() {
        // given
        User user = User.builder()
                .name("test")
                .email("test@example.com")
                .role(Role.USER)
                .build();

        // when
        String roleKey = user.getRoleKey();

        // then
        assertThat(roleKey).isEqualTo("ROLE_USER");
    }
}
