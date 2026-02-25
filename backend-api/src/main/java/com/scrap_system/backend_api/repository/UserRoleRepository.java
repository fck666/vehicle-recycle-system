package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    long countDistinctUserIdByRoleId(Long roleId);

    @Query("""
            select r.code from UserRole ur
            join Role r on r.id = ur.roleId
            where ur.userId = :userId
            """)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Query("""
            select ur.userId, r.code from UserRole ur
            join Role r on r.id = ur.roleId
            where ur.userId in :userIds
            """)
    List<Object[]> findRoleCodesByUserIds(@Param("userIds") List<Long> userIds);
}
