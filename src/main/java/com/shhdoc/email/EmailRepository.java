package com.shhdoc.email;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<Email, Long> {

    /** 남의 메일은 존재 자체를 알려주지 않기 위해 항상 발신자와 함께 조회한다. */
    Optional<Email> findByIdAndSenderId(Long id, Long senderId);

    List<Email> findBySenderIdOrderByIdDesc(Long senderId);

    List<Email> findBySenderIdAndStatusOrderByIdDesc(Long senderId, EmailStatus status);

    /** 관리자는 같은 회사 메일만 본다. sender 를 거쳐 회사를 건다. */
    List<Email> findBySenderCompanyIdAndStatusOrderByIdAsc(Long companyId, EmailStatus status);

    Optional<Email> findByIdAndSenderCompanyId(Long id, Long companyId);
}
