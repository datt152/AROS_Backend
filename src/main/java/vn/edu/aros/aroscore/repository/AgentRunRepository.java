package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.aros.aroscore.entity.AgentRun;

import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {

    @Query("""
            SELECT r FROM AgentRun r
            JOIN FETCH r.exam e
            JOIN FETCH r.classroom
            WHERE r.id = :id AND e.teacher.email = :email
            """)
    Optional<AgentRun> findByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);
}
