package com.melina.jobtrail.repository;

import com.melina.jobtrail.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByIdAndUserId(long id, long userId);
    List<Company> findAllByUserId(long userId);
}
