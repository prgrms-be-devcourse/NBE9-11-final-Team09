package com.back.team9.moyeota.domain.admin.repository;

import com.back.team9.moyeota.domain.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
}
