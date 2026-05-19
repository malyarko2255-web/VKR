package ru.finuniversity.advance.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finuniversity.advance.identity.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByUsername(String username);
}
