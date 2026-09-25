package com.workos.search.service.impl;

import com.workos.common.model.BasePageResult;
import com.workos.search.data.mapper.SearchMapper;
import com.workos.search.model.dto.SearchHitDTO;
import com.workos.search.model.request.SearchReq;
import com.workos.search.model.response.SearchResp;
import com.workos.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Class SearchServiceImpl
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 搜索实现：三类事实合并检索，高亮由服务端生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    /**
     * 高亮片段长度上限
     */
    private static final int SNIPPET_LENGTH = 160;

    private final SearchMapper searchMapper;

    /**
     * @Method search
     * @Param [req]
     * @Return BasePageResult<SearchResp>
     * @Desc 检索事项 / 项目 / 日报
     */
    @Override
    public BasePageResult<SearchResp> search(SearchReq req) {
        req.fillPageDefault();
        String keyword = StringUtils.hasText(req.getQ()) ? req.getQ().trim() : "";
        if (keyword.isEmpty()) {
            return new BasePageResult<>(Collections.emptyList(),
                    req.getPageNo().longValue(), req.getPageSize().longValue(), 0L);
        }
        Set<String> types = parseTypes(req.getTypes());
        boolean item = types.isEmpty() || types.contains("ITEM");
        boolean project = types.isEmpty() || types.contains("PROJECT");
        boolean daily = types.isEmpty() || types.contains("DAILY");

        String like = "%" + escapeLike(keyword) + "%";
        long total = searchMapper.count(like, item, project, daily);
        List<SearchResp> records = total == 0 ? Collections.emptyList()
                : searchMapper.search(like, item, project, daily, req.getPageSize(), req.getStartRow())
                .stream().map(hit -> toResp(hit, keyword)).collect(Collectors.toList());
        return new BasePageResult<>(records, req.getPageNo().longValue(),
                req.getPageSize().longValue(), total);
    }

    private Set<String> parseTypes(String types) {
        if (!StringUtils.hasText(types)) {
            return Collections.emptySet();
        }
        return Arrays.stream(types.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private SearchResp toResp(SearchHitDTO hit, String keyword) {
        SearchResp resp = new SearchResp();
        resp.setType(hit.getHitType());
        resp.setId(hit.getId());
        resp.setTitle(hit.getTitle());
        resp.setProjectId(hit.getProjectId());
        resp.setProjectName(hit.getProjectName());
        resp.setUpdatedAt(hit.getUpdatedAt());
        resp.setHighlight(highlight(StringUtils.hasText(hit.getDetail()) ? hit.getDetail() : hit.getTitle(),
                keyword));
        return resp;
    }

    /**
     * @Method highlight
     * @Param [text, keyword]
     * @Return String
     * @Desc 转义后截断，再用 <em> 包裹命中词
     */
    private String highlight(String text, String keyword) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String plain = text.replaceAll("\\s+", " ").trim();
        if (plain.length() > SNIPPET_LENGTH) {
            plain = plain.substring(0, SNIPPET_LENGTH) + "…";
        }
        String escaped = escapeHtml(plain);
        Matcher matcher = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE).matcher(escaped);
        return matcher.replaceAll(match -> "<em>" + match.group() + "</em>");
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * @Method escapeLike
     * @Param [keyword]
     * @Return String
     * @Desc 转义 LIKE 通配符，避免用户输入 % 变成全匹配
     */
    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
