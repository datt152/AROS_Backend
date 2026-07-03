package vn.edu.aros.aroscore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.aros.aroscore.dto.response.UserResponse;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.repository.AccountRepository;
import vn.edu.aros.aroscore.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserResponse getMyProfile() {
        // 1. Lấy Email của người dùng đang gửi request từ SecurityContext
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Query DB để lấy Account (Từ Account sẽ lấy được User nhờ quan hệ 1-1)
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản hợp lệ!"));

        User user = account.getUser();

        // 3. Map sang DTO và trả về
        return UserResponse.builder()
                .id(user.getId())
                .accountId(account.getId())
                .fullName(user.getFullName())
                .email(account.getEmail()) // Lấy từ Account để đảm bảo chính xác nhất
                .phone(user.getPhone())
                .studentCode(user.getStudentCode())
                .role(account.getRole())
                .build();
    }
}