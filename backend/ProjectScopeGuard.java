package demo.readonlyagent;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** 面试演示代码：以可信会话范围约束模型参数。 */
public final class ProjectScopeGuard {
    private ProjectScopeGuard() {}

    public static long requireProject(Long trustedProjectId, Long requestedProjectId) {
        if (trustedProjectId == null || trustedProjectId <= 0) {
            throw new IllegalStateException("No project is bound to the authenticated conversation");
        }
        if (requestedProjectId != null && !Objects.equals(trustedProjectId, requestedProjectId)) {
            throw new SecurityException("Cross-project query rejected");
        }
        return trustedProjectId;
    }

    public static void requireValidRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Both start and end dates are required");
        }
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) > 31) {
            throw new IllegalArgumentException("Date range must be ordered and no longer than 31 days");
        }
    }
}
