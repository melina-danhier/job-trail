package com.melina.jobtrail.repository;

import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.util.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByIdAndUserId(long id, long userId);
    boolean existsByUserIdAndCompanyIdAndPositionTitle(long userId, long companyId, String positionTitle);
    boolean existsByUserIdAndCompanyIdAndPositionTitleAndIdNot(
            long userId, long companyId, String positionTitle, long id
    );
    List<Application> findAllByUserId(long id);
    Page<Application> findAllByUserId(long userId, Pageable pageable);

    @Query("""
            select application from Application application
            where application.user.id = :userId
              and (:status is null or application.status = :status)
              and (:companyId is null or application.company.id = :companyId)
              and (:dateFrom is null or application.applicationDate >= :dateFrom)
              and (:dateTo is null or application.applicationDate <= :dateTo)
            """)
    Page<Application> findAllFiltered(
            @Param("userId") long userId,
            @Param("status") ApplicationStatus status,
            @Param("companyId") Long companyId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );
}
