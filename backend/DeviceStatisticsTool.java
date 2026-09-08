package demo.readonlyagent;

import java.util.Map;

/** 面试演示代码：数量问题使用专用统计 API，失败不能伪装为零数据。 */
public final class DeviceStatisticsTool {
    private final ReadOnlyIotClient client;

    public DeviceStatisticsTool(ReadOnlyIotClient client) {
        this.client = client;
    }

    public Map<String, Object> execute(Request request, ConversationContext context) {
        long projectId = ProjectScopeGuard.requireProject(context.projectId(), request.projectId());
        ApiResult<Map<String, Object>> response = client.deviceStatistics(projectId, request.parkId());
        if (response == null || !response.ok()) {
            throw new IllegalStateException("Device statistics service is temporarily unavailable");
        }
        return ResultSanitizer.statistics(response.data());
    }

    public record Request(Long projectId, Long parkId) {}
    public record ConversationContext(Long projectId, Long tenantId, Long userId) {}
    public record ApiResult<T>(boolean ok, T data) {}

    public interface ReadOnlyIotClient {
        ApiResult<Map<String, Object>> deviceStatistics(long projectId, Long parkId);
    }
}
