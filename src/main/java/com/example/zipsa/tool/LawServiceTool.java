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
public class LawServiceTool {

    @Value("${law.api.base-url}")
    private String baseUrl;

    @Value("${law.api.search}")
    private String searchPath;

    @Value("${law.api.service}")
    private String servicePath;

    @Value("${law.api.oc}")
    private String oc;

    private final RestClient restClient;

    public LawServiceTool() {
        this.restClient = RestClient.create();
    }

    @Tool(description = "현행법령(시행일) 목록을 조회합니다. 법령명으로 검색하여 법령 목록을 가져옵니다. " +
            "응답의 각 법령에는 '법령ID', '법령일련번호', '시행일자' 등이 포함됩니다. " +
            "법령 본문을 조회하려면 이 결과에서 '법령ID'를 searchLawContent의 id로, " +
            "또는 '법령일련번호'를 mst로 + '시행일자'를 efYd로 전달하세요. " +
            "현행 법령만 조회하려면 '현행연혁코드'가 '현행'인 항목을 사용하세요.")
    public String searchLawList(
            @ToolParam(description = "법령명 검색 질의. 예: 주택법, 건축법, 공인중개사법") String query,
            @ToolParam(description = "검색된 결과 개수 (default=20, max=100)", required = false) String display,
            @ToolParam(description = "검색 결과 페이지 (default=1)", required = false) String page,
            @ToolParam(description = "정렬옵션. lasc: 법령오름차순(기본) / ldes: 법령내림차순 / dasc: 공포일자오름차순 / ddes: 공포일자내림차순 / efasc: 시행일자오름차순 / efdes: 시행일자내림차순", required = false) String sort
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + searchPath)
                .queryParam("OC", oc)
                .queryParam("target", "eflaw")
                .queryParam("type", "JSON");

        if (query != null && !query.isBlank()) {
            uriBuilder.queryParam("query", query);
        }
        if (display != null && !display.isBlank()) {
            uriBuilder.queryParam("display", display);
        }
        if (page != null && !page.isBlank()) {
            uriBuilder.queryParam("page", page);
        }
        if (sort != null && !sort.isBlank()) {
            uriBuilder.queryParam("sort", sort);
        }

        URI uri = uriBuilder.build().encode().toUri();
        log.debug("[searchLawList] 요청 파라미터 - query: {}, display: {}, page: {}, sort: {}", query, display, page, sort);
        log.debug("[searchLawList] 요청 URL: {}", uri);

        String response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        log.info("[searchLawList] 응답: {}", response);
        return response;
    }

    @Tool(description = "법령 본문을 조회합니다. searchLawList 결과와 연관하여 사용합니다. " +
            "방법1: searchLawList 결과의 '법령ID'를 id에 전달 → 해당 법령의 현행 본문 조회. " +
            "방법2: searchLawList 결과의 '법령일련번호'를 mst에, '시행일자'를 efYd에 전달 → 특정 시행일 기준 본문 조회. " +
            "특정 조문만 보려면 jo 파라미터를 사용하세요.")
    public String searchLawContent(
            @ToolParam(description = "searchLawList 결과의 '법령ID' 값. id로 조회하면 현행 본문 반환. id 또는 mst 중 하나 필수", required = false) String id,
            @ToolParam(description = "searchLawList 결과의 '법령일련번호' 값. mst 사용시 efYd 필수. id 또는 mst 중 하나 필수", required = false) String mst,
            @ToolParam(description = "searchLawList 결과의 '시행일자' 값 (YYYYMMDD). mst로 조회시 반드시 함께 전달", required = false) String efYd,
            @ToolParam(description = "조번호. 6자리숫자: 조번호(4자리)+조가지번호(2자리). 예: 000200=2조, 001002=10조의2. 생략시 모든 조 표시", required = false) String jo
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + servicePath)
                .queryParam("OC", oc)
                .queryParam("target", "eflaw")
                .queryParam("type", "JSON");

        if (id != null && !id.isBlank()) {
            uriBuilder.queryParam("ID", id);
        }
        if (mst != null && !mst.isBlank()) {
            uriBuilder.queryParam("MST", mst);
        }
        if (efYd != null && !efYd.isBlank()) {
            uriBuilder.queryParam("efYd", efYd);
        }
        if (jo != null && !jo.isBlank()) {
            uriBuilder.queryParam("JO", jo);
        }

        URI uri = uriBuilder.build().encode().toUri();
        log.info("[searchLawContent] 요청 파라미터 - id: {}, mst: {}, efYd: {}, jo: {}", id, mst, efYd, jo);
        log.info("[searchLawContent] 요청 URL: {}", uri);

        String response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        log.info("[searchLawContent] 응답: {}", response);
        return response;
    }
}
