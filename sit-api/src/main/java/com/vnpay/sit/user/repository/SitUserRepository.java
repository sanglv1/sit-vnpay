package com.vnpay.sit.user.repository;

import com.vnpay.sit.user.entity.SitUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SitUserRepository extends JpaRepository<SitUser, Long> {

    Optional<SitUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            SELECT u FROM SitUser u
            WHERE :q IS NULL OR :q = ''
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY u.fullName ASC
            """)
    List<SitUser> search(@Param("q") String q);
}
