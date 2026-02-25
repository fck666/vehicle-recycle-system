package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByWxOpenid(String wxOpenid);

    Optional<UserAccount> findByWxUnionid(String wxUnionid);

    Optional<UserAccount> findByPhone(String phone);

    @Query("""
            select u from UserAccount u
            where (:q is null
                or lower(u.username) like lower(concat('%', :q, '%'))
                or u.phone like concat('%', :q, '%')
                or u.wxOpenid like concat('%', :q, '%')
                or u.wxUnionid like concat('%', :q, '%')
            )
            """)
    Page<UserAccount> search(@Param("q") String q, Pageable pageable);
}
