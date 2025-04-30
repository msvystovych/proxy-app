package org.company.util;

import lombok.experimental.UtilityClass;
import org.springframework.web.server.ServerWebExchange;

@UtilityClass
public class UrlUtils {
    public static String extractPath(ServerWebExchange exchange) {
        String fullPath = exchange.getRequest().getURI().getPath(); // e.g., /proxy/spring3/
        String contextPath = exchange.getRequest().getPath().contextPath().value(); // usually ""

        String mappingPath = "/proxy";

        if (fullPath.startsWith(contextPath + mappingPath)) {
            String extracted = fullPath.substring((contextPath + mappingPath).length());
            return extracted.isEmpty() ? "/" : extracted;
        } else {
            return "/";
        }
    }
}
