package com.powerfitness.repository;

import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRoleOrderByIdAsc(Role role);
    long countByRole(Role role);
    long countByRoleAndEnabled(Role role, Boolean enabled);
}
