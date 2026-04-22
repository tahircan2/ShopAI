package com.shopai.repository;

import com.shopai.entity.UserAiPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAiPreferenceRepository extends JpaRepository<UserAiPreference, Long> {

    Optional<UserAiPreference> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
