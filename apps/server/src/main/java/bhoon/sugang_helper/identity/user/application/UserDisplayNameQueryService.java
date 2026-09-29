package bhoon.sugang_helper.identity.user.application;

import bhoon.sugang_helper.identity.user.contract.UserDisplayName;
import bhoon.sugang_helper.identity.user.contract.UserDisplayNameQuery;
import bhoon.sugang_helper.identity.user.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDisplayNameQueryService implements UserDisplayNameQuery {

    private final UserRepository userRepository;

    @Override
    public Optional<UserDisplayName> findById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserDisplayName(user.getId(), user.getName()));
    }

    @Override
    public List<UserDisplayName> findByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(user -> new UserDisplayName(user.getId(), user.getName()))
                .toList();
    }
}
