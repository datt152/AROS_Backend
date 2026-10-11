package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.aros.aroscore.entity.RefreshToken;
import vn.edu.aros.aroscore.entity.enums.TokenRevokeReason;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("""
            SELECT t FROM RefreshToken t
            JOIN FETCH t.account
            WHERE t.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken t
            SET t.revokedAt = :now, t.revokeReason = :reason
            WHERE t.account.id = :accountId AND t.revokedAt IS NULL
            """)
    int revokeAllActiveByAccountId(@Param("accountId") Long accountId,
                                   @Param("now") LocalDateTime now,
                                   @Param("reason") TokenRevokeReason reason);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.account.id = :accountId AND t.expiresAt < :now")
    void deleteExpiredByAccountId(@Param("accountId") Long accountId, @Param("now") LocalDateTime now);
}
