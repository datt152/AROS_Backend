package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.enums.AuthClientType;
import vn.edu.aros.aroscore.entity.enums.TokenRevokeReason;

public interface RefreshTokenService {

    /** Tạo phiên mới, trả về token gốc (chỉ trả ra đúng một lần). */
    String issue(Account account, AuthClientType clientType);

    /**
     * Đổi token cũ lấy token mới. {@code rawToken} của kết quả là null khi request rơi vào
     * khoảng ân hạn (token vừa bị xoay bởi một request song song) — client giữ token hiện có.
     */
    RotatedRefreshToken rotate(String rawToken);

    /** Đăng xuất phiên hiện tại; token sai/hết hạn thì bỏ qua. */
    void revoke(String rawToken);

    void revokeAll(Long accountId, TokenRevokeReason reason);

    record RotatedRefreshToken(Account account, String rawToken) {
    }
}
