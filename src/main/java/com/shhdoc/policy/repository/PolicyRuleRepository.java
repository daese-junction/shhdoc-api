package com.shhdoc.policy.repository;

import com.shhdoc.policy.entity.PolicyRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    List<PolicyRule> findByCompanyIdOrderByIdAsc(Long companyId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByDocumentTypeId(Long documentTypeId);

    boolean existsBySensitiveTypeId(Long sensitiveTypeId);
}
