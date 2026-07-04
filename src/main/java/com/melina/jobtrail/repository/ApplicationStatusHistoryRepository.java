package com.melina.jobtrail.repository;

import com.melina.jobtrail.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {
    List<ApplicationStatusHistory> findAllByApplicationIdOrderByChangedAtAscIdAsc(long applicationId);
}
