package com.scrap_system.backend_api.repository;

import com.scrap_system.backend_api.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByUserIdAndClientType(Long userId, String clientType);

    Optional<UserSession> findBySessionId(String sessionId);
}

