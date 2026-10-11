package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.RefreshToken;
import vn.edu.aros.aroscore.entity.enums.AuthClientType;
import vn.edu.aros.aroscore.entity.enums.TokenRevokeReason;
import vn.edu.aros.aroscore.exception.UnauthorizedException;
import vn.edu.aros.aroscore.repository.RefreshTokenRepository;
import vn.edu.aros.aroscore.service.RefreshTokenService;
import vn.edu.aros.aroscore.utils.JwtUtils;
import vn.edu.aros.aroscore.utils.SecureTokenUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Refresh token không hợp lệ hoặc đã hết hạn";

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    /** Nhiều tab web dùng chung cookie có thể refresh cùng lúc; trong khoảng này không coi là token bị đánh cắp. */
    @Value("${auth.refresh-token.reuse-grace-seconds:30}")
    private long reuseGraceSeconds;

    @Override
    @Transactional
    public String issue(Account account, AuthClientType clientType) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteExpiredByAccountId(account.getId(), now);

        String rawToken = SecureTokenUtils.generate();
        refreshTokenRepository.save(RefreshToken.builder()
                .account(account)
                .tokenHash(SecureTokenUtils.sha256Hex(rawToken))
                .clientType(clientType)
                .createdAt(now)
                .expiresAt(now.plusSeconds(jwtUtils.getRefreshExpirationSeconds()))
                .build());
        return rawToken;
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshToken token = findOrThrow(rawToken);
        LocalDateTime now = LocalDateTime.now();

        if (token.getRevokedAt() != null) {
            if (token.getRevokeReason() == TokenRevokeReason.ROTATED) {
                if (token.getRevokedAt().plusSeconds(reuseGraceSeconds).isAfter(now)) {
                    return new RotatedRefreshToken(token.getAccount(), null);
                }
                // Token đã xoay mà vẫn bị dùng lại sau ân hạn => nghi bị đánh cắp, hủy mọi phiên của tài khoản
                revokeAll(token.getAccount().getId(), TokenRevokeReason.REUSE_DETECTED);
            }
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        if (token.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        token.setRevokedAt(now);
        token.setRevokeReason(TokenRevokeReason.ROTATED);
        refreshTokenRepository.save(token);

        return new RotatedRefreshToken(token.getAccount(), issue(token.getAccount(), token.getClientType()));
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(SecureTokenUtils.sha256Hex(rawToken.trim()))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> {
                    t.setRevokedAt(LocalDateTime.now());
                    t.setRevokeReason(TokenRevokeReason.LOGOUT);
                    refreshTokenRepository.save(t);
                });
    }

    @Override
    @Transactional
    public void revokeAll(Long accountId, TokenRevokeReason reason) {
        refreshTokenRepository.revokeAllActiveByAccountId(accountId, LocalDateTime.now(), reason);
    }

    private RefreshToken findOrThrow(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN_MESSAGE);
        }
        return refreshTokenRepository.findByTokenHash(SecureTokenUtils.sha256Hex(rawToken.trim()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN_MESSAGE));
    }
}
