package com.lframework.xingyun.comp.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.lframework.starter.web.core.components.security.DefaultUserDetails;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.starter.web.core.utils.HttpUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.inner.entity.SysOpenDomain;
import com.lframework.starter.web.inner.entity.SysRole;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.starter.web.inner.service.system.SysOpenDomainService;
import com.lframework.starter.web.inner.service.system.SysRoleService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 接收机构端发起的星云 SSO
 */
@Slf4j
@RestController
@RequestMapping("/auth/sso/jianyou")
public class SsoJianyouController {

    private static final String USER_INFO_SESSION_KEY = "user_info_key";

    private static final String DEFAULT_SSO_ERROR_MESSAGE = "星云单点登录失败，请联系管理员";
    private static final List<String> ALLOWED_SSO_ERROR_MESSAGES = new ArrayList<>();

    static {
        ALLOWED_SSO_ERROR_MESSAGES.add("机构端票据消费地址未配置");
        ALLOWED_SSO_ERROR_MESSAGES.add("跳转页面不在允许范围内");
        ALLOWED_SSO_ERROR_MESSAGES.add("受信任客户端未启用");
        ALLOWED_SSO_ERROR_MESSAGES.add("机构端未返回有效票据数据");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云租户未配置");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云租户校验失败");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云账号不存在");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云账号已禁用");
    }

    @Autowired
    private SysOpenDomainService sysOpenDomainService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysRoleService sysRoleService;

    @Value("${xingyun.sso.jianyou.consume-url:}")
    private String consumeUrl;

    @Value("${xingyun.sso.jianyou.result-path:/sso/jianyou/result}")
    private String resultPath;

    @Value("${xingyun.sso.jianyou.front-base-url:}")
    private String frontBaseUrl;

    @Value("${xingyun.sso.jianyou.front-hash-route:true}")
    private Boolean frontHashRoute;

    @Value("${xingyun.sso.jianyou.allowed-redirect-prefixes:/dashboard,/profile,/settings,/basedata,/sc,/settle}")
    private String allowedRedirectPrefixes;

    @GetMapping("/callback")
    public void callback(@RequestParam("clientId") String clientId,
                         @RequestParam("ticket") String ticket,
                         @RequestParam("timestamp") Long timestamp,
                         @RequestParam("sign") String sign,
                         @RequestParam("redirect") String redirect,
                         HttpServletResponse response) throws IOException {
        boolean tenantSwitched = false;
        try {
            if (StringUtils.isBlank(consumeUrl)) {
                throw new IllegalStateException("机构端票据消费地址未配置");
            }
            String normalizedRedirect = normalizeRedirect(redirect);
            if (normalizedRedirect == null) {
                throw new IllegalArgumentException("跳转页面不在允许范围内");
            }

            SysOpenDomain openDomain = getOpenDomain(clientId);
            if (openDomain == null || !Boolean.TRUE.equals(openDomain.getAvailable())) {
                throw new IllegalArgumentException("受信任客户端未启用");
            }

            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("clientId", clientId);
            requestParams.put("ticket", ticket);
            requestParams.put("timestamp", timestamp);
            requestParams.put("sign", sign);
            requestParams.put("redirect", normalizedRedirect);

            String respBody = HttpUtil.doPost(consumeUrl, requestParams);
            Map respMap = JsonUtil.parseObject(respBody, Map.class);
            Integer code = toInteger(respMap.get("code"));
            if (code == null || code != 0) {
                throw new IllegalStateException(String.valueOf(respMap.get("msg")));
            }

            Map data = JsonUtil.convert(respMap.get("data"), Map.class);
            if (data == null) {
                throw new IllegalStateException("机构端未返回有效票据数据");
            }

            Integer targetTenantId = toInteger(data.get("targetTenantId"));
            if (targetTenantId == null) {
                throw new IllegalStateException("星云租户未配置");
            }
            if (openDomain.getTenantId() != null && !openDomain.getTenantId().equals(targetTenantId)) {
                throw new IllegalStateException("星云租户校验失败");
            }

            TenantContextHolder.setTenantId(targetTenantId);
            tenantSwitched = true;

            String targetUserId = toStringValue(data.get("targetXingyunUserId"));
            if (StringUtils.isBlank(targetUserId)) {
                throw new IllegalStateException("星云账号不存在");
            }

            SysUser targetUser = sysUserService.findById(targetUserId);
            if (targetUser == null) {
                throw new IllegalStateException("星云账号不存在");
            }
            if (!Boolean.TRUE.equals(targetUser.getAvailable()) || Boolean.TRUE.equals(targetUser.getLockStatus())) {
                throw new IllegalStateException("星云账号已禁用");
            }

            StpUtil.login(targetUser.getId());
            String token = StpUtil.getTokenValue();
            SaSession session = StpUtil.getSession();
            session.set(USER_INFO_SESSION_KEY, buildUserDetails(targetUser, targetTenantId, token));
            session.set("ssoSource", "jianyou-health-org-web");
            session.set("sourceOrgUserId", data.get("orgUserId"));
            session.set("sourceOrgUserName", data.get("orgUserName"));
            session.set("sourceOrgId", data.get("orgId"));
            session.set("sourceOrgName", data.get("orgName"));
            session.set("sourceDomain", data.get("orgDomain"));
            session.set("ssoTraceId", data.get("traceId"));
            session.set("sourceTenantId", targetTenantId);
            session.set("sourceOpenDomainId", openDomain.getId());

            List<SysRole> roles = sysRoleService.getByUserId(targetUser.getId());
            List<String> roleCodes = roles == null ? new ArrayList<>() : roles.stream()
                    .filter(item -> item != null && StringUtils.isNotBlank(item.getCode()))
                    .map(SysRole::getCode)
                    .collect(Collectors.toList());

            String redirectUrl = buildResultUrl(token, normalizedRedirect, targetUser, roleCodes);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("jianyou sso callback failed, clientId={}, ticket={}", clientId, ticket, e);
            response.sendRedirect(buildLoginFailUrl(resolveErrorMessage(e)));
        } finally {
            if (tenantSwitched) {
                TenantContextHolder.clearTenantId();
            }
        }
    }

    private SysOpenDomain getOpenDomain(String clientId) {
        if (!StringUtils.isNumeric(clientId)) {
            return null;
        }
        return sysOpenDomainService.findById(Integer.valueOf(clientId));
    }

    private String buildResultUrl(String token, String redirect, SysUser targetUser, List<String> roleCodes) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("token", token);
        queryParams.put("redirect", redirect);
        queryParams.put("userId", targetUser.getId());
        queryParams.put("name", StringUtils.defaultIfBlank(targetUser.getName(), targetUser.getUsername()));
        queryParams.put("roles", String.join(",", roleCodes));
        return buildFrontUrl(resultPath, queryParams);
    }

    private String buildLoginFailUrl(String message) {
        Map<String, Object> queryParams = new HashMap<>();
        if (StringUtils.isNotBlank(message)) {
            queryParams.put("ssoError", sanitizeErrorMessage(StringUtils.abbreviate(message, 60)));
        }
        return buildFrontUrl("/login", queryParams);
    }

    private String buildFrontUrl(String path, Map<String, Object> queryParams) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        UriComponentsBuilder routeBuilder = UriComponentsBuilder.fromPath(normalizedPath);
        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.forEach(routeBuilder::queryParam);
        }
        String encodedRoute = buildEncodedUri(routeBuilder);
        if (StringUtils.isBlank(frontBaseUrl)) {
            return Boolean.TRUE.equals(frontHashRoute) ? "/#" + encodedRoute : encodedRoute;
        }

        String baseUrl = StringUtils.removeEnd(frontBaseUrl, "/");
        if (Boolean.TRUE.equals(frontHashRoute)) {
            return baseUrl + "/#" + encodedRoute;
        }
        return baseUrl + encodedRoute;
    }

    private String normalizeRedirect(String redirect) {
        String target = StringUtils.trimToEmpty(redirect);
        if (StringUtils.isBlank(target)) {
            return "/dashboard";
        }
        if (!target.startsWith("/") || target.startsWith("//") || target.contains("://")) {
            return null;
        }
        for (String prefix : getAllowedRedirectPrefixList()) {
            if (target.equals(prefix) || target.startsWith(prefix + "/")) {
                return target;
            }
        }
        return null;
    }

    private List<String> getAllowedRedirectPrefixList() {
        List<String> results = new ArrayList<>();
        if (StringUtils.isBlank(allowedRedirectPrefixes)) {
            results.add("/dashboard");
            return results;
        }
        String[] split = allowedRedirectPrefixes.split(",");
        for (String item : split) {
            String value = StringUtils.trimToEmpty(item);
            if (StringUtils.isNotBlank(value)) {
                results.add(value);
            }
        }
        if (results.isEmpty()) {
            results.add("/dashboard");
        }
        return results;
    }

    private String resolveErrorMessage(Exception e) {
        String message = StringUtils.trimToEmpty(e.getMessage());
        if (StringUtils.isBlank(message)) {
            return DEFAULT_SSO_ERROR_MESSAGE;
        }
        if (ALLOWED_SSO_ERROR_MESSAGES.contains(message)) {
            return message;
        }
        if (StringUtils.containsAny(message, "\r", "\n", "\t")
                || StringUtils.containsIgnoreCase(message, "sql")
                || StringUtils.containsIgnoreCase(message, "exception")
                || StringUtils.containsIgnoreCase(message, "select ")
                || StringUtils.containsIgnoreCase(message, "insert ")
                || StringUtils.containsIgnoreCase(message, "update ")
                || StringUtils.containsIgnoreCase(message, "delete ")) {
            return DEFAULT_SSO_ERROR_MESSAGE;
        }
        return DEFAULT_SSO_ERROR_MESSAGE;
    }

    private String sanitizeErrorMessage(String message) {
        String normalized = StringUtils.defaultString(message);
        normalized = normalized.replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .replace('"', ' ')
                .replace('\'', ' ')
                .replace('<', ' ')
                .replace('>', ' ');
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return StringUtils.defaultIfBlank(normalized, DEFAULT_SSO_ERROR_MESSAGE);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return Integer.valueOf(text);
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private DefaultUserDetails buildUserDetails(SysUser targetUser, Integer targetTenantId, String loginId) {
        DefaultUserDetails userDetails = new DefaultUserDetails();
        userDetails.setId(targetUser.getId());
        userDetails.setUsername(targetUser.getUsername());
        userDetails.setName(StringUtils.defaultIfBlank(targetUser.getName(), targetUser.getUsername()));
        userDetails.setPassword(targetUser.getPassword());
        userDetails.setEmail(targetUser.getEmail());
        userDetails.setTelephone(targetUser.getTelephone());
        userDetails.setAvailable(targetUser.getAvailable());
        userDetails.setLockStatus(targetUser.getLockStatus());
        userDetails.setTenantId(targetTenantId);
        userDetails.setIsAdmin(true);
        userDetails.setIsPlatform(false);
        userDetails.setLoginId(loginId);
        return userDetails;
    }

    private String buildEncodedUri(UriComponentsBuilder builder) {
        return builder.build().encode().toUriString();
    }
}
