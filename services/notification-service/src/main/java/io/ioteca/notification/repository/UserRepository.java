package io.ioteca.notification.repository;

import io.ioteca.notification.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {}
