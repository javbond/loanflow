package com.loanflow.auth.repository;

import com.loanflow.auth.domain.entity.Role;
import com.loanflow.auth.domain.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Role entity
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleType name);

    boolean existsByName(RoleType name);
}
