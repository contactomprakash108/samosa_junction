package com.samosajunction.user.repository;

import com.samosajunction.user.entity.Role;
import com.samosajunction.user.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
