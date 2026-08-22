package com.shhdoc.policy.repository;

import com.shhdoc.policy.entity.SensitiveInfoType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensitiveInfoTypeRepository extends JpaRepository<SensitiveInfoType, Long> {

    List<SensitiveInfoType> findByCompanyIdOrderByIdAsc(Long companyId);

    boolean existsByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdAndCodeAndIdNot(Long companyId, String code, Long id);
}
