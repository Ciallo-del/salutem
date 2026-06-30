package com.lframework.xingyun.comp.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.security.AbstractUserDetails;
import com.lframework.starter.web.core.components.security.UserDetailsService;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.event.ClearTenantEvent;
import com.lframework.starter.web.core.event.ReloadTenantEvent;
import com.lframework.starter.web.core.event.SetTenantEvent;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.EncryptUtil;
import com.lframework.starter.web.core.utils.HttpUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.inner.entity.SysOpenDomain;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.starter.web.inner.entity.Tenant;
import com.lframework.starter.web.inner.service.TenantService;
import com.lframework.starter.web.inner.service.system.SysOpenDomainService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.xingyun.comp.bo.SsoJianyouCurrentUserBo;
import com.lframework.xingyun.comp.bo.SsoJianyouMenuPreviewGroupBo;
import com.lframework.xingyun.comp.service.SsoJianyouMenuPreviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 接收机构端发起的星云 SSO銆?
 */
@Api(tags = "建友星云单点登录")
@Slf4j
@RestController
@RequestMapping("/auth/sso/jianyou")
public class SsoJianyouController extends DefaultBaseController {

    private static final String USER_INFO_SESSION_KEY = "user_info_key";

    private static final String DEFAULT_SSO_ERROR_MESSAGE = "星云单点登录失败，请联系系统管理员！";

    private static final String CORE_DEPT_PERMISSION = "system:dept:query";

    private static final String CORE_USER_PERMISSION = "system:user:query";

    private static final List<String> ALLOWED_SSO_ERROR_MESSAGES = new ArrayList<>();

    static {
        ALLOWED_SSO_ERROR_MESSAGES.add("机构端票据消费地址未配置！");
        ALLOWED_SSO_ERROR_MESSAGES.add("跳转页面不在允许范围内！");
        ALLOWED_SSO_ERROR_MESSAGES.add("受信任客户端未启用！");
        ALLOWED_SSO_ERROR_MESSAGES.add("机构端未返回有效票据数据");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云租户未配置！");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云租户校验失败");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云账号不存在！");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云账号已禁用！");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云 SSO 登录态初始化失败：未加载到任何权限！");
        ALLOWED_SSO_ERROR_MESSAGES.add("星云 SSO 登录态初始化失败：未加载到用户信息！");
    }

    @Autowired
    private SysOpenDomainService sysOpenDomainService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private SsoJianyouMenuPreviewService ssoJianyouMenuPreviewService;

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @Value("${xingyun.sso.jianyou.consume-url:}")
    private String consumeUrl;

    @Value("${xingyun.sso.jianyou.result-path:/sso/jianyou/result}")
    private String resultPath;

    @Value("${xingyun.sso.jianyou.front-base-url:}")
    private String frontBaseUrl;

    @Value("${xingyun.sso.jianyou.front-hash-route:true}")
    private Boolean frontHashRoute;

    @Value("${xingyun.sso.jianyou.allowed-redirect-prefixes:/dashboard,/profile,/settings,/basedata,/base-data,/system,/msg-center,/product,/sc,/stock,/sale,/settle}")
    private String allowedRedirectPrefixes;

    @Value("${xingyun.sso.jianyou.api-secret:}")
    private String apiSecret;

    @Value("${xingyun.sso.jianyou.timestamp-skew-seconds:300}")
    private Integer timestampSkewSeconds;

    @ApiOperation("寤哄弸 SSO 鍥炶皟")
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
                throw new IllegalStateException("机构端票据消费地址未配置！");
            }
            String normalizedRedirect = normalizeRedirect(redirect);
            if (normalizedRedirect == null) {
                throw new IllegalArgumentException("跳转页面不在允许范围内！");
            }

            SysOpenDomain openDomain = getOpenDomain(clientId);
            if (openDomain == null || !Boolean.TRUE.equals(openDomain.getAvailable())) {
                throw new IllegalArgumentException("受信任客户端未启用！");
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
                throw new IllegalStateException("星云租户未配置！");
            }
            if (openDomain.getTenantId() != null && !openDomain.getTenantId().equals(targetTenantId)) {
                log.warn("寤哄弸 SSO 回调租户校验失败，scene=callback, clientId={}, openDomainTenantId={}, targetTenantId={}, targetUserId={}",
                        clientId, openDomain.getTenantId(), targetTenantId, toStringValue(data.get("targetXingyunUserId")));
                throw new IllegalStateException("星云租户校验失败");
            }

            ensureTenantDataSourceReady(targetTenantId);
            switchToTenant(targetTenantId);
            TenantContextHolder.setTenantId(targetTenantId);
            tenantSwitched = true;

            String targetUserId = toStringValue(data.get("targetXingyunUserId"));
            if (StringUtils.isBlank(targetUserId)) {
                throw new IllegalStateException("星云账号不存在！");
            }

            SysUser targetUser = sysUserService.findById(targetUserId);
            if (targetUser == null) {
                throw new IllegalStateException("星云账号不存在！");
            }
            if (!Boolean.TRUE.equals(targetUser.getAvailable()) || Boolean.TRUE.equals(targetUser.getLockStatus())) {
                throw new IllegalStateException("星云账号已禁用！");
            }

            StpUtil.login(targetUser.getId());
            String token = StpUtil.getTokenValue();

            AbstractUserDetails userDetails = loadUserDetails(targetUser, targetTenantId, token);

            SaSession session = StpUtil.getSession();
            session.set(USER_INFO_SESSION_KEY, userDetails);
            session.set("ssoSource", "jianyou-health-org-web");
            session.set("sourceOrgUserId", data.get("orgUserId"));
            session.set("sourceOrgUserName", data.get("orgUserName"));
            session.set("sourceOrgId", data.get("orgId"));
            session.set("sourceOrgName", data.get("orgName"));
            session.set("sourceDomain", data.get("orgDomain"));
            session.set("ssoTraceId", data.get("traceId"));
            session.set("sourceTenantId", targetTenantId);
            session.set("sourceOpenDomainId", openDomain.getId());

            List<String> permissionCodes = new ArrayList<>(userDetails.getPermissions());
            log.info("寤哄弸 SSO 登录成功，tenantId={}, userId={}, username={}, permissionCount={}, hasUserQueryPermission={}, hasDeptQueryPermission={}",
                    targetTenantId, targetUser.getId(), targetUser.getUsername(),
                    permissionCodes.size(),
                    permissionCodes.contains(CORE_USER_PERMISSION),
                    permissionCodes.contains(CORE_DEPT_PERMISSION));

            String redirectUrl = buildResultUrl(token, normalizedRedirect);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("寤哄弸 SSO 回调失败，clientId={}, ticket={}", clientId, ticket, e);
            response.sendRedirect(buildLoginFailUrl(resolveErrorMessage(e)));
        } finally {
            if (tenantSwitched) {
                TenantContextHolder.clearTenantId();
                clearTenant();
            }
        }
    }

    @ApiOperation("获取当前建友 SSO 登录用户信息")
    @GetMapping("/current-user")
    public InvokeResult<SsoJianyouCurrentUserBo> currentUser() {
        AbstractUserDetails currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new DefaultClientException("星云 SSO 登录态初始化失败：未加载到用户信息！");
        }
        Set<String> permissions = currentUser.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            throw new DefaultClientException("星云 SSO 登录态初始化失败：未加载到任何权限！");
        }

        SysUser user = sysUserService.findById(String.valueOf(currentUser.getId()));
        if (user == null) {
            throw new DefaultClientException("星云账号不存在！");
        }

        SsoJianyouCurrentUserBo bo = new SsoJianyouCurrentUserBo();
        bo.setUserId(user.getId());
        bo.setUsername(user.getUsername());
        bo.setName(StringUtils.defaultIfBlank(user.getName(), user.getUsername()));
        bo.setAvatar("");
        bo.setHomePath("/dashboard");
        bo.setPermissions(new ArrayList<>(permissions));
        return InvokeResultBuilder.success(bo);
    }

    @ApiOperation("鑾峰彇寤哄弸 inventory 可用菜单")
    @GetMapping("/menu-preview")
    public InvokeResult<List<SsoJianyouMenuPreviewGroupBo>> menuPreview(@RequestParam("clientId") String clientId,
                                                                        @RequestParam("targetTenantId") Integer targetTenantId,
                                                                        @RequestParam("targetUserId") String targetUserId,
                                                                        @RequestParam("timestamp") Long timestamp,
                                                                        @RequestParam("sign") String sign,
                                                                        HttpServletResponse response) {
        disableCache(response);
        if (StringUtils.isAnyBlank(clientId, targetUserId, sign) || targetTenantId == null || timestamp == null) {
            throw new DefaultClientException("SSO 参数不完整！");
        }
        if (StringUtils.isBlank(apiSecret)) {
            throw new DefaultClientException(DEFAULT_SSO_ERROR_MESSAGE);
        }

        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > timestampSkewSeconds * 1000L) {
            throw new DefaultClientException("请求已过期，请重新进入 inventory）");
        }

        SysOpenDomain openDomain = getOpenDomain(clientId);
        if (openDomain == null || !Boolean.TRUE.equals(openDomain.getAvailable())) {
            throw new DefaultClientException("受信任客户端未启用！");
        }
        if (openDomain.getTenantId() != null && !openDomain.getTenantId().equals(targetTenantId)) {
            log.warn("寤哄弸 inventory 菜单预览租户校验失败，scene=menu-preview, clientId={}, openDomainTenantId={}, targetTenantId={}, targetUserId={}",
                    clientId, openDomain.getTenantId(), targetTenantId, targetUserId);
            throw new DefaultClientException("星云租户校验失败");
        }

        String expectedSign = buildMenuPreviewSign(clientId, targetTenantId, targetUserId, timestamp);
        if (!StringUtils.equalsIgnoreCase(expectedSign, sign)) {
            throw new DefaultClientException("菜单预览签名校验失败");
        }

        boolean tenantSwitched = false;
        try {
            ensureTenantDataSourceReady(targetTenantId);
            switchToTenant(targetTenantId);
            TenantContextHolder.setTenantId(targetTenantId);
            tenantSwitched = true;

            SysUser targetUser = sysUserService.findById(targetUserId);
            if (targetUser == null) {
                throw new DefaultClientException("星云账号不存在！");
            }
            if (!Boolean.TRUE.equals(targetUser.getAvailable()) || Boolean.TRUE.equals(targetUser.getLockStatus())) {
                throw new DefaultClientException("星云账号已禁用！");
            }

            return InvokeResultBuilder.success(ssoJianyouMenuPreviewService.getMenuPreview(targetUserId));
        } finally {
            if (tenantSwitched) {
                TenantContextHolder.clearTenantId();
                clearTenant();
            }
        }
    }

    private void switchToTenant(Integer tenantId) {
        ApplicationUtil.publishEvent(new SetTenantEvent(this, tenantId));
    }

    private void clearTenant() {
        ApplicationUtil.publishEvent(new ClearTenantEvent(this));
    }

    private void ensureTenantDataSourceReady(Integer tenantId) {
        Tenant tenant = tenantService.findById(tenantId);
        if (tenant == null || !Boolean.TRUE.equals(tenant.getAvailable())) {
            throw new DefaultClientException(DEFAULT_SSO_ERROR_MESSAGE);
        }
        if (StringUtils.isAnyBlank(tenant.getJdbcUrl(), tenant.getJdbcUsername(), tenant.getJdbcPassword())) {
            throw new DefaultClientException(DEFAULT_SSO_ERROR_MESSAGE);
        }

        String tenantDataSourceKey = String.valueOf(tenantId);
        if (dynamicRoutingDataSource != null
                && dynamicRoutingDataSource.getDataSources().containsKey(tenantDataSourceKey)) {
            return;
        }

        ApplicationUtil.publishEvent(new ReloadTenantEvent(this, tenantId, tenant.getJdbcUrl(),
                tenant.getJdbcUsername(), EncryptUtil.decrypt(tenant.getJdbcPassword())));
    }

    private AbstractUserDetails loadUserDetails(SysUser targetUser, Integer targetTenantId, String loginId) {
        AbstractUserDetails userDetails = userDetailsService.loadUserByUsername(targetUser.getUsername());
        if (userDetails == null) {
            throw new DefaultClientException("星云 SSO 登录态初始化失败：未加载到用户信息！");
        }

        userDetails.setTenantId(targetTenantId);
        userDetails.setLoginId(loginId);
        userDetails.setIsPlatform(false);

        Set<String> permissions = userDetails.getPermissions() == null
                ? new HashSet<>()
                : new HashSet<>(userDetails.getPermissions());
        userDetails.setPermissions(permissions);

        if (permissions.isEmpty()) {
            log.error("寤哄弸 SSO 登录态权限为空，tenantId={}, userId={}, username={}",
                    targetTenantId, targetUser.getId(), targetUser.getUsername());
            throw new DefaultClientException("星云 SSO 登录态初始化失败：未加载到任何权限！");
        }

        return userDetails;
    }

    private SysOpenDomain getOpenDomain(String clientId) {
        if (!StringUtils.isNumeric(clientId)) {
            return null;
        }
        return sysOpenDomainService.findById(Integer.valueOf(clientId));
    }

    private String buildResultUrl(String token, String redirect) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("token", token);
        queryParams.put("redirect", redirect);
        return buildFrontUrl(resultPath, queryParams);
    }

    private String buildMenuPreviewSign(String clientId, Integer targetTenantId, String targetUserId, Long timestamp) {
        String source = clientId + "|" + targetTenantId + "|" + targetUserId + "|" + timestamp + "|" + apiSecret;
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8)).toUpperCase();
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
        if (!target.startsWith("/") || target.startsWith("//") || target.contains("://") || target.contains("#")) {
            return null;
        }
        String path = extractRedirectPath(target);
        if (StringUtils.isBlank(path)) {
            return null;
        }
        for (String prefix : getAllowedRedirectPrefixList()) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return target;
            }
        }
        return null;
    }

    private String extractRedirectPath(String redirect) {
        int queryIndex = redirect.indexOf('?');
        if (queryIndex < 0) {
            return redirect;
        }
        return redirect.substring(0, queryIndex);
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

    private String buildEncodedUri(UriComponentsBuilder builder) {
        return builder.build().encode().toUriString();
    }

    private void disableCache(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);
    }
}
