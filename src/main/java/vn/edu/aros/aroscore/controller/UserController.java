package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.aros.aroscore.dto.request.UpdateStudentCodeRequest;
import vn.edu.aros.aroscore.dto.response.UserResponse;
import vn.edu.aros.aroscore.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    /**
     * Sinh viên tự cập nhật mã sinh viên (8 chữ số).
     */
    @PutMapping("/me/student-code")
    public ResponseEntity<UserResponse> updateMyStudentCode(
            @Valid @RequestBody UpdateStudentCodeRequest request) {
        return ResponseEntity.ok(userService.updateMyStudentCode(request));
    }
}
