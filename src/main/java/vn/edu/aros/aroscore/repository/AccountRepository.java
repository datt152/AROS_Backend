package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.aros.aroscore.entity.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
}
