package bhoon.sugang_helper.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RepositoryContract<T, ID> {
    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findAllById(Iterable<ID> ids);

    Page<T> findAll(Pageable pageable);

    <S extends T> S save(S entity);

    <S extends T> S saveAndFlush(S entity);

    void delete(T entity);

    void deleteById(ID id);

    void flush();

    long count();
}
