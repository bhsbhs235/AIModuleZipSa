package com.example.zipsa.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@Slf4j
public class AptTradeTool {

    @Value("${data_go_kr.apt-trade.base-url}")
    private String baseUrl;

    @Value("${data_go_kr.auth_key}")
    private String serviceKey;

    private final RestClient restClient;

    public AptTradeTool() {
        this.restClient = RestClient.create();
    }

    private String callApi(String toolName, URI uri) {
        try {
            log.debug("[{}] 요청 URL: {}", toolName, uri);
            String response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            log.info("[{}] 응답: {}", toolName, response);
            return response;
        } catch (Exception e) {
            log.error("[{}] API 호출 실패: {}", toolName, e.getMessage(), e);
            return "[Error] " + toolName + " 호출 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private void addParam(UriComponentsBuilder builder, String key, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(key, value);
        }
    }

    @Tool(description = "아파트 매매 실거래가 자료를 조회합니다. 특정 지역의 아파트 매매 실거래 내역을 검색합니다. " +
            "응답에는 단지명, 법정동, 전용면적, 거래금액(만원), 계약년월일, 층, 건축년도, 거래유형 등이 포함됩니다. " +
            "지역코드(LAWD_CD)는 행정표준코드관리시스템(www.code.go.kr)의 법정동코드 10자리 중 앞 5자리입니다. " +
            "예: 서울 종로구=11110, 서울 강남구=11680, 서울 서초구=11650, 서울 송파구=11710, 서울 마포구=11440, " +
            "경기 성남시 분당구=41135, 경기 수원시 영통구=41117, 부산 해운대구=26350")
    public String searchAptTrade(
            @ToolParam(description = "지역코드 (필수). 법정동코드 10자리 중 앞 5자리. 예: 11110(서울 종로구), 11680(서울 강남구)") String lawdCd,
            @ToolParam(description = "계약년월 (필수). 6자리(YYYYMM). 예: 202407") String dealYmd,
            @ToolParam(description = "페이지 번호 (기본값: 1)", required = false) String pageNo,
            @ToolParam(description = "한 페이지 결과 수 (기본값: 10)", required = false) String numOfRows
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYmd);

        addParam(uriBuilder, "pageNo", pageNo);
        addParam(uriBuilder, "numOfRows", numOfRows);

        return callApi("searchAptTrade", uriBuilder.build(true).toUri());
    }
}
