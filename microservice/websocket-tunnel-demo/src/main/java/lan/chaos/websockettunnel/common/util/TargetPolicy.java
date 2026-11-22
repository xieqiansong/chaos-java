package lan.chaos.websockettunnel.common.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 目标地址白名单——把隧道暴露到网关后，{@code /forward/{host}/{port}} 的目标完全由客户端指定，
 * 不设白名单就是现成的 SSRF 跳板（可探测/访问任意内网服务）。这里默认 fail-closed：
 * 规则为空时一律拒绝；规则形如 {@code host:port}（精确）或 {@code host}（该主机任意端口）。
 */
public final class TargetPolicy {

    private final Set<String> allowed;

    public TargetPolicy(Collection<String> allowedTargets) {
        Set<String> normalized = new LinkedHashSet<>();
        if (allowedTargets != null) {
            for (String item : allowedTargets) {
                if (item != null && !item.trim().isEmpty()) {
                    normalized.add(item.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.allowed = Collections.unmodifiableSet(normalized);
    }

    public boolean isAllowed(String host, int port) {
        if (host == null || allowed.isEmpty()) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT);
        return allowed.contains(h + ":" + port) || allowed.contains(h);
    }
}
