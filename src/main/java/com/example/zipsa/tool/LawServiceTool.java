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

        addParam(uriBuilder, "query", query);
        addParam(uriBuilder, "display", display);
        addParam(uriBuilder, "page", page);
        addParam(uriBuilder, "sort", sort);

        return callApi("searchLawList", uriBuilder.build().encode().toUri());
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

        addParam(uriBuilder, "ID", id);
        addParam(uriBuilder, "MST", mst);
        addParam(uriBuilder, "efYd", efYd);
        addParam(uriBuilder, "JO", jo);

        return callApi("searchLawContent", uriBuilder.build().encode().toUri());
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

        addParam(uriBuilder, "ID", id);
        addParam(uriBuilder, "MST", mst);
        addParam(uriBuilder, "efYd", efYd);
        addParam(uriBuilder, "HANG", hang);
        addParam(uriBuilder, "HO", ho);
        addParam(uriBuilder, "MOK", mok);

        return callApi("searchLawContentDetail", uriBuilder.build().encode().toUri());
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

        addParam(uriBuilder, "ID", id);
        addParam(uriBuilder, "MST", mst);
        addParam(uriBuilder, "HANG", hang);
        addParam(uriBuilder, "HO", ho);
        addParam(uriBuilder, "MOK", mok);

        return callApi("searchLawContentDetailByDate", uriBuilder.build().encode().toUri());
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

        addParam(uriBuilder, "query", query);
        addParam(uriBuilder, "display", display);
        addParam(uriBuilder, "page", page);
        addParam(uriBuilder, "sort", sort);
        addParam(uriBuilder, "regDt", regDt);
        addParam(uriBuilder, "gana", gana);
        addParam(uriBuilder, "dicKndCd", dicKndCd);

        return callApi("searchLawTermList", uriBuilder.build().encode().toUri());
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

        addParam(uriBuilder, "query", query);

        return callApi("searchLawTermContent", uriBuilder.build().encode().toUri());
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

        addParam(uriBuilder, "query", query);
        addParam(uriBuilder, "search", search);
        addParam(uriBuilder, "display", display);
        addParam(uriBuilder, "page", page);
        addParam(uriBuilder, "sort", sort);
        addParam(uriBuilder, "itmno", itmno);
        addParam(uriBuilder, "explYd", explYd);

        return callApi("searchMolitLawInterpretationList", uriBuilder.build().encode().toUri());
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

        addParam(uriBuilder, "ID", id);
        addParam(uriBuilder, "LM", lm);

        return callApi("searchMolitLawInterpretationContent", uriBuilder.build().encode().toUri());
    }

    // ===== 자치법규 =====

    @Tool(description = "자치법규 목록을 조회합니다. 지방자치단체의 조례, 규칙 등을 검색합니다. " +
            "응답의 각 항목에는 '자치법규일련번호', '자치법규ID', '자치법규명' 등이 포함됩니다. " +
            "본문을 조회하려면 searchOrdinanceContent에 '자치법규ID'를 id로 또는 '자치법규일련번호'를 mst로 전달하세요.")
    public String searchOrdinanceList(
            @ToolParam(description = "자치법규명 검색 질의. 예: 주차장, 건축, 도시계획") String query,
            @ToolParam(description = "검색범위. 1: 자치법규명(기본) / 2: 본문검색", required = false) String search,
            @ToolParam(description = "검색된 결과 개수 (default=20, max=100)", required = false) String display,
            @ToolParam(description = "검색 결과 페이지 (default=1)", required = false) String page,
            @ToolParam(description = "정렬옵션. lasc: 자치법규오름차순(기본) / ldes: 자치법규내림차순 / dasc: 공포일자오름차순 / ddes: 공포일자내림차순 / efasc: 시행일자오름차순 / efdes: 시행일자내림차순", required = false) String sort,
            @ToolParam(description = "1: 현행(기본), 2: 연혁", required = false) String nw,
            @ToolParam(description = "지자체 도·특별시·광역시 코드. 예: 서울특별시=6110000", required = false) String org,
            @ToolParam(description = "지자체 시·군·구 코드. org와 함께 사용. 예: 서울특별시 구로구 → org=6110000&sborg=3160000", required = false) String sborg,
            @ToolParam(description = "법령종류. 30001: 조례 / 30002: 규칙 / 30003: 훈령 / 30004: 예규 / 30010: 고시 / 30011: 의회규칙", required = false) String knd
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + searchPath)
                .queryParam("OC", oc)
                .queryParam("target", "ordin")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "query", query);
        addParam(uriBuilder, "search", search);
        addParam(uriBuilder, "display", display);
        addParam(uriBuilder, "page", page);
        addParam(uriBuilder, "sort", sort);
        addParam(uriBuilder, "nw", nw);
        addParam(uriBuilder, "org", org);
        addParam(uriBuilder, "sborg", sborg);
        addParam(uriBuilder, "knd", knd);

        return callApi("searchOrdinanceList", uriBuilder.build().encode().toUri());
    }

    @Tool(description = "자치법규 본문을 조회합니다. searchOrdinanceList 결과와 연관하여 사용합니다. " +
            "searchOrdinanceList 결과의 '자치법규ID'를 id에, 또는 '자치법규일련번호'를 mst에 전달하면 " +
            "조문내용, 부칙, 별표 등 본문 상세 내용을 반환합니다.")
    public String searchOrdinanceContent(
            @ToolParam(description = "searchOrdinanceList 결과의 '자치법규ID' 값. id 또는 mst 중 하나 필수", required = false) String id,
            @ToolParam(description = "searchOrdinanceList 결과의 '자치법규일련번호' 값. id 또는 mst 중 하나 필수", required = false) String mst
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + servicePath)
                .queryParam("OC", oc)
                .queryParam("target", "ordin")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "ID", id);
        addParam(uriBuilder, "MST", mst);

        return callApi("searchOrdinanceContent", uriBuilder.build().encode().toUri());
    }

    // ===== 판례 =====

    @Tool(description = "판례 목록을 조회합니다. 법원 판례를 사건명, 본문 등으로 검색합니다. " +
            "응답의 각 항목에는 '판례일련번호', '사건명', '사건번호', '법원명' 등이 포함됩니다. " +
            "판례 본문(판시사항, 판결요지, 판례내용)을 조회하려면 searchPrecedentContent에 '판례일련번호'를 id로 전달하세요.")
    public String searchPrecedentList(
            @ToolParam(description = "사건명 검색 질의. 예: 담보권, 임대차, 소유권") String query,
            @ToolParam(description = "검색범위. 1: 판례명(기본) / 2: 본문검색", required = false) String search,
            @ToolParam(description = "검색된 결과 개수 (default=20, max=100)", required = false) String display,
            @ToolParam(description = "검색 결과 페이지 (default=1)", required = false) String page,
            @ToolParam(description = "정렬옵션. lasc: 사건명오름차순 / ldes: 사건명내림차순 / dasc: 선고일자오름차순 / ddes: 선고일자내림차순(기본) / nasc: 법원명오름차순 / ndes: 법원명내림차순", required = false) String sort,
            @ToolParam(description = "법원종류. 400201: 대법원 / 400202: 하위법원", required = false) String org,
            @ToolParam(description = "법원명. 예: 대법원, 서울고등법원, 인천지방법원", required = false) String curt,
            @ToolParam(description = "참조법령명. 예: 형법, 민법", required = false) String jo,
            @ToolParam(description = "판례 사건번호", required = false) String nb,
            @ToolParam(description = "선고일자 범위 검색. 예: 20090101~20090130", required = false) String prncYd
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + searchPath)
                .queryParam("OC", oc)
                .queryParam("target", "prec")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "query", query);
        addParam(uriBuilder, "search", search);
        addParam(uriBuilder, "display", display);
        addParam(uriBuilder, "page", page);
        addParam(uriBuilder, "sort", sort);
        addParam(uriBuilder, "org", org);
        addParam(uriBuilder, "curt", curt);
        addParam(uriBuilder, "JO", jo);
        addParam(uriBuilder, "nb", nb);
        addParam(uriBuilder, "prncYd", prncYd);

        return callApi("searchPrecedentList", uriBuilder.build().encode().toUri());
    }

    @Tool(description = "판례 본문을 조회합니다. searchPrecedentList 결과와 연관하여 사용합니다. " +
            "searchPrecedentList 결과의 '판례일련번호'를 id에 전달하면 " +
            "판시사항, 판결요지, 참조조문, 참조판례, 판례내용 등 상세 정보를 반환합니다.")
    public String searchPrecedentContent(
            @ToolParam(description = "searchPrecedentList 결과의 '판례일련번호' 값 (필수)") String id,
            @ToolParam(description = "판례명", required = false) String lm
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + servicePath)
                .queryParam("OC", oc)
                .queryParam("target", "prec")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "ID", id);
        addParam(uriBuilder, "LM", lm);

        return callApi("searchPrecedentContent", uriBuilder.build().encode().toUri());
    }

    // ===== 중앙토지수용위원회 결정문 =====

    @Tool(description = "중앙토지수용위원회 결정문 목록을 조회합니다. 토지수용 관련 결정문을 검색합니다. " +
            "응답의 각 항목에는 '결정문일련번호', '제목' 등이 포함됩니다. " +
            "결정문 본문(관련법리, 관련규정, 판단, 근거)을 조회하려면 searchOcltContent에 '결정문일련번호'를 id로 전달하세요.")
    public String searchOcltList(
            @ToolParam(description = "제목 검색 질의. 예: 토지, 수용, 보상") String query,
            @ToolParam(description = "검색범위. 1: 제목(기본) / 2: 본문검색", required = false) String search,
            @ToolParam(description = "검색된 결과 개수 (default=20, max=100)", required = false) String display,
            @ToolParam(description = "검색 결과 페이지 (default=1)", required = false) String page,
            @ToolParam(description = "정렬옵션. lasc: 제목오름차순(기본) / ldes: 제목내림차순", required = false) String sort
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + searchPath)
                .queryParam("OC", oc)
                .queryParam("target", "oclt")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "query", query);
        addParam(uriBuilder, "search", search);
        addParam(uriBuilder, "display", display);
        addParam(uriBuilder, "page", page);
        addParam(uriBuilder, "sort", sort);

        return callApi("searchOcltList", uriBuilder.build().encode().toUri());
    }

    @Tool(description = "중앙토지수용위원회 결정문 본문을 조회합니다. searchOcltList 결과와 연관하여 사용합니다. " +
            "searchOcltList 결과의 '결정문일련번호'를 id에 전달하면 " +
            "관련법리, 관련규정, 판단, 근거, 주해 등 상세 내용을 반환합니다.")
    public String searchOcltContent(
            @ToolParam(description = "searchOcltList 결과의 '결정문일련번호' 값 (필수)") String id
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + servicePath)
                .queryParam("OC", oc)
                .queryParam("target", "oclt")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "ID", id);

        return callApi("searchOcltContent", uriBuilder.build().encode().toUri());
    }

    // ===== 조세심판원 특별행정심판재결례 =====

    @Tool(description = "조세심판원 특별행정심판재결례 목록을 조회합니다. 조세 관련 행정심판 재결례를 검색합니다. " +
            "응답의 각 항목에는 '특별행정심판재결례일련번호', '사건명', '청구번호' 등이 포함됩니다. " +
            "재결례 본문(재결요지, 주문, 이유)을 조회하려면 searchTtSpecialDeccContent에 '특별행정심판재결례일련번호'를 id로 전달하세요.")
    public String searchTtSpecialDeccList(
            @ToolParam(description = "재결례명 검색 질의. 예: 양도소득세, 취득세, 재산세") String query,
            @ToolParam(description = "검색범위. 1: 특별행정심판재결례명(기본) / 2: 본문검색", required = false) String search,
            @ToolParam(description = "검색된 결과 개수 (default=20, max=100)", required = false) String display,
            @ToolParam(description = "검색 결과 페이지 (default=1)", required = false) String page,
            @ToolParam(description = "정렬옵션. lasc: 재결례명오름차순(기본) / ldes: 재결례명내림차순 / dasc: 의결일자오름차순 / ddes: 의결일자내림차순 / nasc: 청구번호오름차순 / ndes: 청구번호내림차순", required = false) String sort,
            @ToolParam(description = "재결례유형 (재결구분코드)", required = false) String cls,
            @ToolParam(description = "처분일자 범위 검색. 예: 20090101~20090130", required = false) String dpaYd,
            @ToolParam(description = "의결일자 범위 검색. 예: 20090101~20090130", required = false) String rslYd
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + searchPath)
                .queryParam("OC", oc)
                .queryParam("target", "ttSpecialDecc")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "query", query);
        addParam(uriBuilder, "search", search);
        addParam(uriBuilder, "display", display);
        addParam(uriBuilder, "page", page);
        addParam(uriBuilder, "sort", sort);
        addParam(uriBuilder, "cls", cls);
        addParam(uriBuilder, "dpaYd", dpaYd);
        addParam(uriBuilder, "rslYd", rslYd);

        return callApi("searchTtSpecialDeccList", uriBuilder.build().encode().toUri());
    }

    @Tool(description = "조세심판원 특별행정심판재결례 본문을 조회합니다. searchTtSpecialDeccList 결과와 연관하여 사용합니다. " +
            "searchTtSpecialDeccList 결과의 '특별행정심판재결례일련번호'를 id에 전달하면 " +
            "재결요지, 주문, 청구취지, 이유, 관련법령 등 상세 내용을 반환합니다.")
    public String searchTtSpecialDeccContent(
            @ToolParam(description = "searchTtSpecialDeccList 결과의 '특별행정심판재결례일련번호' 값 (필수)") String id,
            @ToolParam(description = "특별행정심판재결례명", required = false) String lm
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + servicePath)
                .queryParam("OC", oc)
                .queryParam("target", "ttSpecialDecc")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "ID", id);
        addParam(uriBuilder, "LM", lm);

        return callApi("searchTtSpecialDeccContent", uriBuilder.build().encode().toUri());
    }

    // ===== 재정경제부 법령해석 =====

    @Tool(description = "재정경제부 법령해석 목록을 조회합니다. 재정·경제 관련 법령의 해석 사례를 검색합니다. " +
            "응답의 각 항목에는 '법령해석일련번호', '안건명', '안건번호' 등이 포함됩니다.")
    public String searchMoefLawInterpretationList(
            @ToolParam(description = "법령해석명 또는 본문 검색 질의. 예: 조합, 승계, 지분") String query,
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
                .queryParam("target", "moefCgmExpc")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "query", query);
        addParam(uriBuilder, "search", search);
        addParam(uriBuilder, "display", display);
        addParam(uriBuilder, "page", page);
        addParam(uriBuilder, "sort", sort);
        addParam(uriBuilder, "itmno", itmno);
        addParam(uriBuilder, "explYd", explYd);

        return callApi("searchMoefLawInterpretationList", uriBuilder.build().encode().toUri());
    }

    // ===== 국세청 법령해석 =====

    @Tool(description = "국세청 법령해석 목록을 조회합니다. 세금·국세 관련 법령의 해석 사례를 검색합니다. " +
            "응답의 각 항목에는 '법령해석일련번호', '안건명', '안건번호' 등이 포함됩니다.")
    public String searchNtsLawInterpretationList(
            @ToolParam(description = "법령해석명 또는 본문 검색 질의. 예: 세금, 증여, 재산") String query,
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
                .queryParam("target", "ntsCgmExpc")
                .queryParam("type", "JSON");

        addParam(uriBuilder, "query", query);
        addParam(uriBuilder, "search", search);
        addParam(uriBuilder, "display", display);
        addParam(uriBuilder, "page", page);
        addParam(uriBuilder, "sort", sort);
        addParam(uriBuilder, "itmno", itmno);
        addParam(uriBuilder, "explYd", explYd);

        return callApi("searchNtsLawInterpretationList", uriBuilder.build().encode().toUri());
    }
}
