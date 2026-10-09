package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.aros.aroscore.entity.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("""
            SELECT t FROM PasswordResetToken t
            JOIN FETCH t.account
            WHERE t.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    Optional<PasswordResetToken> findTopByAccountIdOrderByCreatedAtDesc(Long accountId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.account.id = :accountId")
    void deleteAllByAccountId(@Param("accountId") Long accountId);
}
