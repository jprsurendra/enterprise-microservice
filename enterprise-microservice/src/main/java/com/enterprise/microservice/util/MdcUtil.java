package com.enterprise.microservice.util;

import org.slf4j.MDC;
import java.util.UUID;

public class MdcUtil {

    public static void generateAndSetTraceId() {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
    }

    public static void setClientIp(String clientIp) {
        MDC.put("clientIp", clientIp);
    }

    public static void clear() {
        MDC.clear();
    }

    public static String getCurrentTraceId() {
        return MDC.get("traceId");
    }
}