package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.user.dto.result.UserDisplayName;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDisplayNameQueryService {

    private final UserRepository userRepository;

    public Optional<UserDisplayName> findById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserDisplayName(user.getId(), user.getName()));
    }

    public List<UserDisplayName> findByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(user -> new UserDisplayName(user.getId(), user.getName()))
                .toList();
    }
}
