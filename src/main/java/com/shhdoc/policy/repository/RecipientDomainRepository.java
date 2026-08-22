package com.shhdoc.policy.repository;

import com.shhdoc.policy.entity.RecipientDomain;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientDomainRepository extends JpaRepository<RecipientDomain, Long> {

    List<RecipientDomain> findByCompanyIdOrderByIdAsc(Long companyId);

    boolean existsByCompanyIdAndDomain(Long companyId, String domain);

    boolean existsByCompanyIdAndDomainAndIdNot(Long companyId, String domain, Long id);
}
