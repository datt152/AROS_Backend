package vn.edu.aros.aroscore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.aros.aroscore.dto.response.UserResponse;
import vn.edu.aros.aroscore.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }
}