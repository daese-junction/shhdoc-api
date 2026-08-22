package com.shhdoc.policy.repository;

import com.shhdoc.policy.entity.DocumentCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {

    List<DocumentCategory> findByCompanyIdOrderByIdAsc(Long companyId);

    boolean existsByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdAndCodeAndIdNot(Long companyId, String code, Long id);
}
