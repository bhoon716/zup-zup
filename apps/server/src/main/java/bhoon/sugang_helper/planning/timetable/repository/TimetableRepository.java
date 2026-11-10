package bhoon.sugang_helper.planning.timetable.repository;

import bhoon.sugang_helper.planning.timetable.dto.result.TimetableListItem;
import bhoon.sugang_helper.planning.timetable.entity.Timetable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findByUserId(Long userId);

    @Query("""
            select new bhoon.sugang_helper.planning.timetable.dto.result.TimetableListItem(
                t.id, t.name, t.isPrimary, size(t.entries))
            from Timetable t
            where t.userId = :userId
            """)
    List<TimetableListItem> findListItemsByUserId(@Param("userId") Long userId);

    List<Timetable> findByUserIdAndIsPrimaryTrue(Long userId);

    @EntityGraph(attributePaths = "entries")
    List<Timetable> findWithEntriesByUserIdAndIsPrimaryTrue(Long userId);

    @EntityGraph(attributePaths = "entries")
    Optional<Timetable> findWithEntriesById(Long id);

    long countByUserId(Long userId);
}
