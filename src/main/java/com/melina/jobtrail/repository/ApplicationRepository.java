package com.melina.jobtrail.repository;

import com.melina.jobtrail.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByIdAndUserId(long id, long userId);
    List<Application> findAllByUserId(long id);
    Page<Application> findAllByUserId(long userId, Pageable pageable);
}
