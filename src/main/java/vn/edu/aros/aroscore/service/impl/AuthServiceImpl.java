package vn.edu.aros.aroscore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.aros.aroscore.dto.request.SignUpRequest;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.enums.UserRole;
import vn.edu.aros.aroscore.repository.AccountRepository;
import vn.edu.aros.aroscore.service.AuthService;

@Service
public class AuthServiceImpl  implements AuthService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Override
    public void registerAccount(SignUpRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại!");
        }

        // Tạo Entity
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPassword(passwordEncoder.encode(request.getPassword())); // MÃ HÓA PASSWORD
        account.setRole(request.getRole());

        accountRepository.save(account);
    }
}
