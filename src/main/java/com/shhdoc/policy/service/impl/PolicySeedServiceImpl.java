package com.shhdoc.policy.service.impl;

import com.shhdoc.company.Company;
import com.shhdoc.policy.entity.Classification;
import com.shhdoc.policy.entity.DocumentCategory;
import com.shhdoc.policy.entity.DocumentType;
import com.shhdoc.policy.entity.PolicyAction;
import com.shhdoc.policy.entity.PolicyRule;
import com.shhdoc.policy.entity.RecipientDomain;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.entity.SendDirection;
import com.shhdoc.policy.entity.SensitiveInfoType;
import com.shhdoc.policy.repository.DocumentCategoryRepository;
import com.shhdoc.policy.repository.DocumentTypeRepository;
import com.shhdoc.policy.repository.PolicyRuleRepository;
import com.shhdoc.policy.repository.RecipientDomainRepository;
import com.shhdoc.policy.repository.SensitiveInfoTypeRepository;
import com.shhdoc.policy.service.PolicySeedService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicySeedServiceImpl implements PolicySeedService {

    private final DocumentCategoryRepository categoryRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final SensitiveInfoTypeRepository sensitiveTypeRepository;
    private final RecipientDomainRepository domainRepository;
    private final PolicyRuleRepository ruleRepository;

    private record SeedType(String categoryCode, String code, String name, String description) {
    }

    private record SeedCategory(String code, String name) {
    }

    private static final List<SeedCategory> CATEGORIES = List.of(
            new SeedCategory("HR", "인사/노무"),
            new SeedCategory("FINANCE", "재무/회계"),
            new SeedCategory("LEGAL", "법무/계약"),
            new SeedCategory("SALES", "영업"),
            new SeedCategory("PROCUREMENT", "구매"),
            new SeedCategory("STRATEGY", "경영/전략"),
            new SeedCategory("RND", "기술/R&D"),
            new SeedCategory("SECURITY_IT", "보안/IT"),
            new SeedCategory("GENERAL", "일반업무"));

    private static final List<SeedType> DOCUMENT_TYPES = List.of(
            new SeedType("HR", "EMPLOYMENT_CONTRACT", "근로계약서", "근로 조건·연봉이 담긴 고용 계약 문서"),
            new SeedType("HR", "PAYROLL", "급여명세서", "급여·공제 내역이 담긴 급여 명세 문서"),
            new SeedType("HR", "PERFORMANCE_REVIEW", "인사평가", "직원 평가 등급·평가 의견이 담긴 문서"),
            new SeedType("HR", "RECRUITMENT", "채용 서류", "이력서, 면접 평가, 채용 품의 등 채용 관련 문서"),
            new SeedType("HR", "ORG_CHART", "조직도", "부서 구성과 인원 배치가 담긴 문서"),
            new SeedType("FINANCE", "FINANCIAL_STATEMENT", "재무제표", "재무상태표, 손익계산서 등 결산 문서"),
            new SeedType("FINANCE", "TAX_INVOICE", "세금계산서", "거래 증빙용 세금계산서·계산서"),
            new SeedType("FINANCE", "BUDGET", "예산 문서", "예산 계획·집행 내역 문서"),
            new SeedType("FINANCE", "AUDIT_REPORT", "감사보고서", "내부·외부 감사 결과 문서"),
            new SeedType("LEGAL", "CONTRACT", "계약서", "거래·협력 조건이 담긴 계약 문서"),
            new SeedType("LEGAL", "NDA", "비밀유지계약서", "비밀유지 의무를 정한 계약 문서"),
            new SeedType("LEGAL", "LEGAL_OPINION", "법률의견서", "법률 자문·검토 의견 문서"),
            new SeedType("LEGAL", "LITIGATION", "소송 문서", "소장, 준비서면 등 소송·분쟁 관련 문서"),
            new SeedType("SALES", "QUOTATION", "견적서", "제품·서비스 가격 견적 문서"),
            new SeedType("SALES", "PROPOSAL", "제안서", "고객 대상 사업·입찰 제안 문서"),
            new SeedType("SALES", "CUSTOMER_LIST", "고객명단", "고객·거래처 연락처와 거래 정보 목록"),
            new SeedType("SALES", "PRICING_POLICY", "가격정책", "판매 단가·할인 기준이 담긴 내부 문서"),
            new SeedType("PROCUREMENT", "PURCHASE_ORDER", "발주서", "공급사에 보내는 구매 주문 문서"),
            new SeedType("PROCUREMENT", "RFQ_BID", "입찰 문서", "견적 요청, 입찰 공고·평가 문서"),
            new SeedType("PROCUREMENT", "PRICE_AGREEMENT", "단가계약", "공급 단가를 정한 계약 문서"),
            new SeedType("STRATEGY", "BUSINESS_PLAN", "사업계획서", "사업 목표·전략이 담긴 계획 문서"),
            new SeedType("STRATEGY", "EXEC_MEETING", "경영회의자료", "경영진 회의 보고·안건 문서"),
            new SeedType("STRATEGY", "MNA_DOCUMENT", "M&A 문서", "인수합병·투자 검토 문서"),
            new SeedType("RND", "DESIGN_DRAWING", "설계도면", "제품·부품 설계 도면"),
            new SeedType("RND", "TECH_SPEC", "기술사양서", "기술 사양·아키텍처 설계 문서"),
            new SeedType("RND", "SOURCE_CODE", "소스코드", "프로그램 소스코드 파일 또는 코드가 담긴 문서"),
            new SeedType("RND", "RESEARCH_NOTE", "연구자료", "연구노트, 실험 데이터 등 R&D 기록"),
            new SeedType("SECURITY_IT", "SYSTEM_DIAGRAM", "시스템 구성도", "시스템·네트워크 구성이 담긴 문서"),
            new SeedType("SECURITY_IT", "CREDENTIAL", "계정/인증정보", "비밀번호, API 키, 인증서 등이 담긴 문서"),
            new SeedType("SECURITY_IT", "VULNERABILITY_REPORT", "취약점 보고서", "취약점 진단·모의해킹 결과 문서"),
            new SeedType("GENERAL", "MEETING_MINUTES", "회의록", "일반 업무 회의 기록"),
            new SeedType("GENERAL", "WORK_REPORT", "업무보고", "주간·월간 업무 보고 문서"),
            new SeedType("GENERAL", "OFFICIAL_LETTER", "공문", "대외 공문·협조문"),
            new SeedType("GENERAL", "PR_MATERIAL", "홍보자료", "보도자료, 홍보물 등 공개 목적 문서"),
            new SeedType("GENERAL", "ETC", "기타", "어느 유형에도 속하지 않는 문서"));

    private record SeedSensitive(String code, String name, String description) {
    }

    private static final List<SeedSensitive> SENSITIVE_TYPES = List.of(
            new SeedSensitive("PERSONAL", "개인정보", "주민등록번호, 연락처, 주소, 계좌번호 등 개인 식별 정보"),
            new SeedSensitive("FINANCIAL", "재무정보", "매출, 원가, 손익 등 회사 재무 수치"),
            new SeedSensitive("CUSTOMER", "고객정보", "고객사명, 담당자, 거래 조건 등 고객 관련 정보"),
            new SeedSensitive("TRADE_SECRET", "영업기밀", "가격 전략, 영업 전략 등 경쟁상 비밀 정보"),
            new SeedSensitive("TECHNICAL", "기술정보", "설계, 사양, 공정 등 기술 자산 정보"),
            new SeedSensitive("SOURCE_CODE", "소스코드", "프로그램 소스코드"),
            new SeedSensitive("CREDENTIAL", "인증정보", "비밀번호, API 키, 토큰, 인증서 등 접근 자격 정보"));

    private static final List<String> FREE_MAIL_DOMAINS = List.of(
            "gmail.com", "naver.com", "daum.net", "hanmail.net", "kakao.com",
            "nate.com", "hotmail.com", "outlook.com", "yahoo.com", "icloud.com");

    @Override
    @Transactional
    public void seedFor(Company company) {
        Map<String, DocumentCategory> categories = new HashMap<>();
        for (SeedCategory category : CATEGORIES) {
            categories.put(category.code(),
                    categoryRepository.save(new DocumentCategory(company, category.code(), category.name())));
        }

        for (SeedType type : DOCUMENT_TYPES) {
            documentTypeRepository.save(new DocumentType(
                    company, categories.get(type.categoryCode()), type.code(), type.name(), type.description()));
        }

        Map<String, SensitiveInfoType> sensitives = new HashMap<>();
        for (SeedSensitive sensitive : SENSITIVE_TYPES) {
            sensitives.put(sensitive.code(), sensitiveTypeRepository.save(new SensitiveInfoType(
                    company, sensitive.code(), sensitive.name(), sensitive.description())));
        }

        for (String domain : FREE_MAIL_DOMAINS) {
            domainRepository.save(new RecipientDomain(company, domain, RecipientScope.PERSONAL_EMAIL));
        }

        ruleRepository.save(new PolicyRule(company, "인증정보 사외 반출 차단",
                null, null, sensitives.get("CREDENTIAL"), null,
                SendDirection.OUTBOUND, null, PolicyAction.BLOCK));
        ruleRepository.save(new PolicyRule(company, "소스코드 개인메일 반출 차단",
                null, null, sensitives.get("SOURCE_CODE"), null,
                SendDirection.OUTBOUND, RecipientScope.PERSONAL_EMAIL, PolicyAction.BLOCK));
        ruleRepository.save(new PolicyRule(company, "극비 문서 사외 반출 차단",
                null, null, null, Classification.SECRET,
                SendDirection.OUTBOUND, null, PolicyAction.BLOCK));
        ruleRepository.save(new PolicyRule(company, "대외비 문서 외부 반출 검토",
                null, null, null, Classification.CONFIDENTIAL,
                SendDirection.OUTBOUND, RecipientScope.EXTERNAL, PolicyAction.REVIEW));
        ruleRepository.save(new PolicyRule(company, "개인정보 포함 문서 외부 반출 검토",
                null, null, sensitives.get("PERSONAL"), null,
                SendDirection.OUTBOUND, RecipientScope.EXTERNAL, PolicyAction.REVIEW));
    }
}
