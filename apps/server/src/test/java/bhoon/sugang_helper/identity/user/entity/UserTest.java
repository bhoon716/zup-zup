package bhoon.sugang_helper.identity.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.identity.user.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("User 생성 테스트")
    void createUser() {

        String name = "test";
        String email = "test@example.com";
        Role role = Role.USER;


        User user = User.builder()
                .name(name)
                .email(email)
                .role(role)
                .build();


        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getRole()).isEqualTo(role);
    }

    @Test
    @DisplayName("User 정보 수정 테스트")
    void updateUser() {

        User user = User.builder()
                .name("test")
                .email("test@example.com")
                .role(Role.USER)
                .build();
        String newName = "updated";


        user.update(newName);


        assertThat(user.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("Role Key 조회 테스트")
    void getRoleKey() {

        User user = User.builder()
                .name("test")
                .email("test@example.com")
                .role(Role.USER)
                .build();


        String roleKey = user.getRoleKey();


        assertThat(roleKey).isEqualTo("ROLE_USER");
    }
}
