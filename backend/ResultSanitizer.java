package demo.readonlyagent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 面试演示代码：下游返回采用允许字段白名单，而不是危险字段黑名单。 */
public final class ResultSanitizer {
    private static final List<String> DEVICE_FIELDS = List.of(
            "deviceId", "name", "typeName", "status", "onlineTime", "offlineTime", "alarmCount");
    private static final List<String> STAT_FIELDS = List.of(
            "total", "online", "offline", "alarmCount", "onlineRate", "offlineRate");

    private ResultSanitizer() {}

    public static Map<String, Object> device(Map<String, Object> source) {
        return copyAllowed(source, DEVICE_FIELDS);
    }

    public static Map<String, Object> statistics(Map<String, Object> source) {
        return copyAllowed(source, STAT_FIELDS);
    }

    private static Map<String, Object> copyAllowed(Map<String, Object> source, List<String> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source == null) return result;
        for (String field : fields) {
            if (source.containsKey(field)) result.put(field, limitText(source.get(field)));
        }
        return result;
    }

    private static Object limitText(Object value) {
        if (value instanceof String text && text.length() > 2_000) {
            return text.substring(0, 2_000) + "…";
        }
        return value;
    }
}
