package com.shhdoc.policy.repository;

import com.shhdoc.policy.entity.DocumentType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    List<DocumentType> findByCompanyIdOrderByIdAsc(Long companyId);

    boolean existsByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdAndCodeAndIdNot(Long companyId, String code, Long id);

    boolean existsByCategoryId(Long categoryId);
}
