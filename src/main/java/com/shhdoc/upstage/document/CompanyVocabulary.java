package com.shhdoc.upstage.document;

import com.shhdoc.upstage.pipeline.classify.DocumentCategory;
import com.shhdoc.upstage.pipeline.extract.SensitiveInfoCategory;

import java.util.List;

/**
 * 회사별 분류/추출 후보 어휘. companyId 기준 DB조회(문서유형/민감정보유형)로 만들어지는데,
 * 메일 하나에 첨부가 여럿이어도 같은 값이라 {@link DocumentAnalyzer#loadVocabulary}로
 * 메일당 한 번만 만들어 모든 첨부의 {@link DocumentAnalyzer#analyze} 호출에 재사용한다.
 */
public record CompanyVocabulary(
        List<DocumentCategory> categories,
        List<SensitiveInfoCategory> sensitiveTypes
) {
}
