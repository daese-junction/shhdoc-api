package com.shhdoc.upstage.pipeline.parse;

/**
 * Document Parse 응답의 elements 배열 원소 하나.
 *
 * @param category 레이아웃 카테고리 (table/figure/chart/heading1/paragraph/equation/list/index/footnote 등)
 * @param content  해당 element의 html/markdown/text
 * @param page     문서 내 페이지 번호 (1부터 시작)
 */
public record ParsedElement(
        String category,
        ParsedContent content,
        int page
) {
}
