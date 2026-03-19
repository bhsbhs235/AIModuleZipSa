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

    @Tool(description = "법령 본문의 특정 조항호목을 상세 조회합니다. searchLawList 결과와 연관하여 사용합니다. " +
            "searchLawList 결과의 '법령ID'를 id에, 또는 '법령일련번호'를 mst에 + '시행일자'를 efYd에 전달하고, " +
            "반드시 jo(조번호)를 지정해야 합니다. 항(hang), 호(ho), 목(mok)으로 더 세부적인 조회가 가능합니다. " +
            "예: 건축법 제3조제1항제2호다목 → jo=000300, hang=000100, ho=000200, mok=다")
    public String searchLawContentDetail(
            @ToolParam(description = "searchLawList 결과의 '법령ID' 값. id 또는 mst 중 하나 필수", required = false) String id,
            @ToolParam(description = "searchLawList 결과의 '법령일련번호' 값. mst 사용시 efYd 필수. id 또는 mst 중 하나 필수", required = false) String mst,
            @ToolParam(description = "searchLawList 결과의 '시행일자' 값 (YYYYMMDD). mst로 조회시 반드시 함께 전달", required = false) String efYd,
            @ToolParam(description = "조 번호 6자리숫자 (필수). 예: 제2조=000200, 제10조의2=001002") String jo,
            @ToolParam(description = "항 번호 6자리숫자. 예: 제2항=000200", required = false) String hang,
            @ToolParam(description = "호 번호 6자리숫자. 예: 제2호=000200, 제10호의2=001002", required = false) String ho,
            @ToolParam(description = "목 한자리 문자. 예: 가,나,다,라,…카,타,파,하", required = false) String mok
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + servicePath)
                .queryParam("OC", oc)
                .queryParam("target", "eflawjosub")
                .queryParam("type", "JSON")
                .queryParam("JO", jo);

        if (id != null && !id.isBlank()) {
            uriBuilder.queryParam("ID", id);
        }
        if (mst != null && !mst.isBlank()) {
            uriBuilder.queryParam("MST", mst);
        }
        if (efYd != null && !efYd.isBlank()) {
            uriBuilder.queryParam("efYd", efYd);
        }
        if (hang != null && !hang.isBlank()) {
            uriBuilder.queryParam("HANG", hang);
        }
        if (ho != null && !ho.isBlank()) {
            uriBuilder.queryParam("HO", ho);
        }
        if (mok != null && !mok.isBlank()) {
            uriBuilder.queryParam("MOK", mok);
        }

        URI uri = uriBuilder.build().encode().toUri();
        log.info("[searchLawContentDetail] 요청 파라미터 - id: {}, mst: {}, efYd: {}, jo: {}, hang: {}, ho: {}, mok: {}", id, mst, efYd, jo, hang, ho, mok);
        log.info("[searchLawContentDetail] 요청 URL: {}", uri);

        String response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        log.info("[searchLawContentDetail] 응답: {}", response);
        return response;
    }

    @Tool(description = "현행법령(공포일 기준) 본문의 특정 조항호목을 상세 조회합니다. " +
            "주의: 이 도구는 사용자가 명시적으로 '공포일 기준' 조회를 요청한 경우에만 사용하세요. " +
            "일반적인 법령 조회는 시행일 기준인 searchLawContentDetail을 사용하세요. " +
            "searchLawList 결과의 '법령ID'를 id에, 또는 '법령일련번호'를 mst에 전달하고, " +
            "반드시 jo(조번호)를 지정해야 합니다. 항(hang), 호(ho), 목(mok)으로 더 세부적인 조회가 가능합니다.")
    public String searchLawContentDetailByDate(
            @ToolParam(description = "searchLawList 결과의 '법령ID' 값. id 또는 mst 중 하나 필수. ID로 검색하면 현행 법령 본문 조회", required = false) String id,
            @ToolParam(description = "searchLawList 결과의 '법령일련번호' 값. id 또는 mst 중 하나 필수", required = false) String mst,
            @ToolParam(description = "조 번호 6자리숫자 (필수). 예: 제2조=000200, 제10조의2=001002") String jo,
            @ToolParam(description = "항 번호 6자리숫자. 예: 제2항=000200", required = false) String hang,
            @ToolParam(description = "호 번호 6자리숫자. 예: 제2호=000200, 제10호의2=001002", required = false) String ho,
            @ToolParam(description = "목 한자리 문자. 예: 가,나,다,라,…카,타,파,하", required = false) String mok
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + servicePath)
                .queryParam("OC", oc)
                .queryParam("target", "lawjosub")
                .queryParam("type", "JSON")
                .queryParam("JO", jo);

        if (id != null && !id.isBlank()) {
            uriBuilder.queryParam("ID", id);
        }
        if (mst != null && !mst.isBlank()) {
            uriBuilder.queryParam("MST", mst);
        }
        if (hang != null && !hang.isBlank()) {
            uriBuilder.queryParam("HANG", hang);
        }
        if (ho != null && !ho.isBlank()) {
            uriBuilder.queryParam("HO", ho);
        }
        if (mok != null && !mok.isBlank()) {
            uriBuilder.queryParam("MOK", mok);
        }

        URI uri = uriBuilder.build().encode().toUri();
        log.info("[searchLawContentDetailByDate] 요청 파라미터 - id: {}, mst: {}, jo: {}, hang: {}, ho: {}, mok: {}", id, mst, jo, hang, ho, mok);
        log.info("[searchLawContentDetailByDate] 요청 URL: {}", uri);

        String response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        log.info("[searchLawContentDetailByDate] 응답: {}", response);
        return response;
    }

    @Tool(description = "법령용어 목록을 조회합니다. 법령용어명으로 검색하여 용어 목록을 가져옵니다. " +
            "응답의 각 용어에는 '법령용어ID', '법령용어명', '사전구분코드' 등이 포함됩니다. " +
            "용어의 상세 정의를 조회하려면 searchLawTermContent에 해당 법령용어명을 query로 전달하세요.")
    public String searchLawTermList(
            @ToolParam(description = "법령용어명 검색 질의. 예: 자동차, 선박, 임대차") String query,
            @ToolParam(description = "검색된 결과 개수 (default=20, max=100)", required = false) String display,
            @ToolParam(description = "검색 결과 페이지 (default=1)", required = false) String page,
            @ToolParam(description = "정렬옵션. lasc: 법령용어명오름차순(기본) / ldes: 법령용어명내림차순 / rasc: 등록일자오름차순 / rdes: 등록일자내림차순", required = false) String sort,
            @ToolParam(description = "등록일자 범위 검색. 예: 20090101~20090130", required = false) String regDt,
            @ToolParam(description = "사전식 검색. 예: ga(ㄱ), na(ㄴ), da(ㄷ), ra(ㄹ), ma(ㅁ), ba(ㅂ), sa(ㅅ), ah(ㅇ), ja(ㅈ), cha(ㅊ), ka(ㅋ), ta(ㅌ), pa(ㅍ), ha(ㅎ)", required = false) String gana,
            @ToolParam(description = "법령 종류 코드. 010101: 법령 / 010102: 행정규칙", required = false) String dicKndCd
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + searchPath)
                .queryParam("OC", oc)
                .queryParam("target", "lstrm")
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
        if (regDt != null && !regDt.isBlank()) {
            uriBuilder.queryParam("regDt", regDt);
        }
        if (gana != null && !gana.isBlank()) {
            uriBuilder.queryParam("gana", gana);
        }
        if (dicKndCd != null && !dicKndCd.isBlank()) {
            uriBuilder.queryParam("dicKndCd", dicKndCd);
        }

        URI uri = uriBuilder.build().encode().toUri();
        log.debug("[searchLawTermList] 요청 파라미터 - query: {}, display: {}, page: {}, sort: {}, regDt: {}, gana: {}, dicKndCd: {}", query, display, page, sort, regDt, gana, dicKndCd);
        log.debug("[searchLawTermList] 요청 URL: {}", uri);

        String response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        log.info("[searchLawTermList] 응답: {}", response);
        return response;
    }

    @Tool(description = "법령용어의 상세 정의(본문)를 조회합니다. searchLawTermList 결과와 연관하여 사용합니다. " +
            "searchLawTermList 결과의 '법령용어명'을 query에 전달하면 해당 용어의 정의, 출처 등 상세 정보를 반환합니다.")
    public String searchLawTermContent(
            @ToolParam(description = "상세조회하고자 하는 법령용어명. 예: 선박, 자동차, 임대차") String query
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + servicePath)
                .queryParam("OC", oc)
                .queryParam("target", "lstrm")
                .queryParam("type", "JSON");

        if (query != null && !query.isBlank()) {
            uriBuilder.queryParam("query", query);
        }

        URI uri = uriBuilder.build().encode().toUri();
        log.info("[searchLawTermContent] 요청 파라미터 - query: {}", query);
        log.info("[searchLawTermContent] 요청 URL: {}", uri);

        String response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        log.info("[searchLawTermContent] 응답: {}", response);
        return response;
    }

    @Tool(description = "국토교통부 법령해석 목록을 조회합니다. 부동산·건축·도로 등 국토교통부 소관 법령의 해석 사례를 검색합니다. " +
            "응답의 각 항목에는 '법령해석일련번호', '안건명', '안건번호' 등이 포함됩니다. " +
            "해석 본문(질의요지, 회답, 이유)을 조회하려면 searchMolitLawInterpretationContent에 '법령해석일련번호'를 id로 전달하세요.")
    public String searchMolitLawInterpretationList(
            @ToolParam(description = "법령해석명 또는 본문 검색 질의. 예: 도로, 아파트, 상업") String query,
            @ToolParam(description = "검색범위. 1: 법령해석명(기본) / 2: 본문검색", required = false) String search,
            @ToolParam(description = "검색된 결과 개수 (default=20, max=100)", required = false) String display,
            @ToolParam(description = "검색 결과 페이지 (default=1)", required = false) String page,
            @ToolParam(description = "정렬옵션. lasc: 법령해석명오름차순(기본) / ldes: 법령해석명내림차순 / dasc: 해석일자오름차순 / ddes: 해석일자내림차순 / nasc: 안건번호오름차순 / ndes: 안건번호내림차순", required = false) String sort,
            @ToolParam(description = "안건번호. 안건번호로 검색 시 query는 무시됩니다.", required = false) String itmno,
            @ToolParam(description = "해석일자 범위 검색. 예: 20090101~20090130", required = false) String explYd
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + searchPath)
                .queryParam("OC", oc)
                .queryParam("target", "molitCgmExpc")
                .queryParam("type", "JSON");

        if (query != null && !query.isBlank()) {
            uriBuilder.queryParam("query", query);
        }
        if (search != null && !search.isBlank()) {
            uriBuilder.queryParam("search", search);
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
        if (itmno != null && !itmno.isBlank()) {
            uriBuilder.queryParam("itmno", itmno);
        }
        if (explYd != null && !explYd.isBlank()) {
            uriBuilder.queryParam("explYd", explYd);
        }

        URI uri = uriBuilder.build().encode().toUri();
        log.debug("[searchMolitLawInterpretationList] 요청 파라미터 - query: {}, search: {}, display: {}, page: {}, sort: {}, itmno: {}, explYd: {}", query, search, display, page, sort, itmno, explYd);
        log.debug("[searchMolitLawInterpretationList] 요청 URL: {}", uri);

        String response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        log.info("[searchMolitLawInterpretationList] 응답: {}", response);
        return response;
    }

    @Tool(description = "국토교통부 법령해석 본문을 조회합니다. searchMolitLawInterpretationList 결과와 연관하여 사용합니다. " +
            "searchMolitLawInterpretationList 결과의 '법령해석일련번호'를 id에 전달하면 " +
            "질의요지, 회답, 이유, 관련법령 등 해석 상세 내용을 반환합니다.")
    public String searchMolitLawInterpretationContent(
            @ToolParam(description = "searchMolitLawInterpretationList 결과의 '법령해석일련번호' 값 (필수)") String id,
            @ToolParam(description = "법령해석명", required = false) String lm
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + servicePath)
                .queryParam("OC", oc)
                .queryParam("target", "molitCgmExpc")
                .queryParam("type", "JSON");

        if (id != null && !id.isBlank()) {
            uriBuilder.queryParam("ID", id);
        }
        if (lm != null && !lm.isBlank()) {
            uriBuilder.queryParam("LM", lm);
        }

        URI uri = uriBuilder.build().encode().toUri();
        log.info("[searchMolitLawInterpretationContent] 요청 파라미터 - id: {}, lm: {}", id, lm);
        log.info("[searchMolitLawInterpretationContent] 요청 URL: {}", uri);

        String response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        log.info("[searchMolitLawInterpretationContent] 응답: {}", response);
        return response;
    }
}
