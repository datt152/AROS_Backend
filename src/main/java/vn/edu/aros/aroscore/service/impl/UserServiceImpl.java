package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.UpdateProfileRequest;
import vn.edu.aros.aroscore.dto.request.UpdateStudentCodeRequest;
import vn.edu.aros.aroscore.dto.response.UserResponse;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.UserRole;
import vn.edu.aros.aroscore.repository.AccountRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.StudentCodeService;
import vn.edu.aros.aroscore.service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final StudentCodeService studentCodeService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return toResponse(loadCurrentAccount());
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        Account account = loadCurrentAccount();
        User user = account.getUser();

        user.setFullName(request.getFullName().trim());

        if (request.getStudentCode() != null && !request.getStudentCode().isBlank()) {
            if (account.getRole() != UserRole.STUDENT) {
                throw new IllegalArgumentException("Chỉ sinh viên mới cập nhật mã sinh viên!");
            }
            String normalized = studentCodeService.validateForAllClassroomsOfUser(
                    request.getStudentCode(), user.getId());
            user.setStudentCode(normalized);
        }

        userRepository.save(user);
        return toResponse(account);
    }

    @Override
    @Transactional
    public UserResponse updateMyStudentCode(UpdateStudentCodeRequest request) {
        Account account = loadCurrentAccount();
        if (account.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Quyền truy cập không hợp lệ!");
        }

        User user = account.getUser();
        String normalized = studentCodeService.validateForAllClassroomsOfUser(
                request.getStudentCode(), user.getId());

        user.setStudentCode(normalized);
        userRepository.save(user);

        return toResponse(account);
    }

    private Account loadCurrentAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản hợp lệ!"));
    }

    private UserResponse toResponse(Account account) {
        User user = account.getUser();
        String code = user.getStudentCode();
        boolean complete = code != null && !code.isBlank();
        return UserResponse.builder()
                .id(user.getId())
                .accountId(account.getId())
                .fullName(user.getFullName())
                .email(account.getEmail())
                .studentCode(code)
                .profileComplete(complete)
                .role(account.getRole())
                .build();
    }
}
