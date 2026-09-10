package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.BulkCreateAccountsRequest;
import vn.edu.aros.aroscore.dto.response.BulkCreateAccountsResultResponse;
import vn.edu.aros.aroscore.dto.response.BulkCreateAccountsResultResponse.Item;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.Classroom;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.UserRole;
import vn.edu.aros.aroscore.repository.AccountRepository;
import vn.edu.aros.aroscore.repository.ClassroomRepository;
import vn.edu.aros.aroscore.service.BulkAccountService;
import vn.edu.aros.aroscore.service.MailService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkAccountServiceImpl implements BulkAccountService {

    private final ClassroomRepository classroomRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Override
    @Transactional
    public BulkCreateAccountsResultResponse createAccountsForClassroom(
            Long classId, BulkCreateAccountsRequest request) {

        Classroom classroom = classroomRepository.findByIdWithStudents(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));
        assertLecturerAccess(classroom);
        if (Boolean.FALSE.equals(classroom.getIsActive())) {
            throw new IllegalArgumentException("Lớp học đã ngừng hoạt động!");
        }

        List<User> students = classroom.getStudents() == null
                ? List.of()
                : new ArrayList<>(classroom.getStudents());

        Set<Long> filterIds = null;
        if (request != null && request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            filterIds = new HashSet<>(request.getStudentIds());
        }

        int created = 0;
        int skipped = 0;
        int failed = 0;
        int mailQueued = 0;
        List<Item> results = new ArrayList<>();

        for (User student : students) {
            if (filterIds != null && !filterIds.contains(student.getId())) {
                continue;
            }

            String email = student.getEmail() != null
                    ? student.getEmail().trim().toLowerCase(Locale.ROOT)
                    : null;
            String fullName = student.getFullName();

            try {
                if (email == null || email.isBlank()) {
                    throw new IllegalArgumentException("Sinh viên thiếu email");
                }

                Account existingByUser = student.getAccount();
                if (existingByUser != null) {
                    skipped++;
                    results.add(item(student, email, fullName, "SKIPPED",
                            "Đã có tài khoản — bỏ qua"));
                    continue;
                }

                var existingByEmail = accountRepository.findByEmailIgnoreCase(email);
                if (existingByEmail.isPresent()) {
                    Account acc = existingByEmail.get();
                    if (acc.getRole() != UserRole.STUDENT) {
                        failed++;
                        results.add(item(student, email, fullName, "FAILED",
                                "Email thuộc tài khoản không phải sinh viên"));
                        continue;
                    }
                    // Account STUDENT đã tồn tại (có thể chưa gắn user này) — bỏ qua tạo mới
                    skipped++;
                    results.add(item(student, email, fullName, "SKIPPED",
                            "Email đã có tài khoản sinh viên — bỏ qua"));
                    continue;
                }

                String rawPassword = generateTemporaryPassword();
                Account account = new Account();
                account.setEmail(email);
                account.setPassword(passwordEncoder.encode(rawPassword));
                account.setRole(UserRole.STUDENT);
                account.setUser(student);
                accountRepository.save(account);

                mailService.sendStudentAccountCreated(email, fullName, rawPassword);
                mailQueued++;
                created++;
                results.add(item(student, email, fullName, "CREATED",
                        "Đã tạo tài khoản và xếp hàng gửi email"));
            } catch (Exception ex) {
                failed++;
                results.add(item(student, email, fullName, "FAILED",
                        ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định"));
                log.warn("Bulk create account failed for studentId={}: {}", student.getId(), ex.getMessage());
            }
        }

        int total = created + skipped + failed;
        return BulkCreateAccountsResultResponse.builder()
                .total(total)
                .created(created)
                .skipped(skipped)
                .failed(failed)
                .mailQueued(mailQueued)
                .results(results)
                .build();
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private Item item(User student, String email, String fullName, String status, String message) {
        return Item.builder()
                .studentId(student.getId())
                .email(email)
                .fullName(fullName)
                .status(status)
                .message(message)
                .build();
    }

    private void assertLecturerAccess(Classroom classroom) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!classroom.getSubject().getLecturer().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền truy cập lớp học này!");
        }
    }
}
