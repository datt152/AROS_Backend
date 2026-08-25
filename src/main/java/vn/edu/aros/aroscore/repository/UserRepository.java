package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.aros.aroscore.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByStudentCode(String studentCode);
    boolean existsByEmail(String email);
    List<User> findAllByEmailIn(List<String> emails);

    @Query("SELECT u FROM User u JOIN FETCH u.account WHERE u.email IN :emails")
    List<User> findAllByEmailInWithAccount(@Param("emails") List<String> emails);

    @Query("SELECT u FROM User u JOIN FETCH u.account WHERE u.id = :id")
    Optional<User> findByIdWithAccount(@Param("id") Long id);
}