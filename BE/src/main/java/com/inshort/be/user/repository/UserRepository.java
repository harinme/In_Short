package com.inshort.be.user.repository;

import com.inshort.be.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByPhone(String phone);

  boolean existsByPhone(String phone);
}
