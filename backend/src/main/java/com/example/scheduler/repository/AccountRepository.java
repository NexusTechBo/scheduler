package com.example.scheduler.repository;

import com.example.scheduler.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByEmailOrCi(String email, String ci);
    Optional<Account> findByCi(String ci);
    boolean existsByEmail(String email);
}
