package com.shhdoc.policy.repository;

import com.shhdoc.policy.entity.PolicyRule;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    /**
     * category/documentType/sensitiveType는 전부 LAZY 연관이라 룰을 upstage 판정모델로
     * 변환할 때(각 필드 getter 호출) 룰 개수만큼 추가 SELECT가 나가는 N+1을 막기 위해
     * 한 번에 fetch join한다.
     */
    @EntityGraph(attributePaths = {"category", "documentType", "sensitiveType"})
    List<PolicyRule> findByCompanyIdOrderByIdAsc(Long companyId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByDocumentTypeId(Long documentTypeId);

    boolean existsBySensitiveTypeId(Long sensitiveTypeId);
}
