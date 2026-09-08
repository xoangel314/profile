package demo.readonlyagent;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ProjectScopeGuardTest {
    @Test
    void usesTrustedProjectScope() {
        assertEquals(101L, ProjectScopeGuard.requireProject(101L, null));
        assertEquals(101L, ProjectScopeGuard.requireProject(101L, 101L));
    }

    @Test
    void rejectsCrossProjectRequest() {
        assertThrows(SecurityException.class,
                () -> ProjectScopeGuard.requireProject(101L, 202L));
    }

    @Test
    void limitsEventQueryRange() {
        assertDoesNotThrow(() -> ProjectScopeGuard.requireValidRange(
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31")));
        assertThrows(IllegalArgumentException.class, () -> ProjectScopeGuard.requireValidRange(
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-15")));
    }
}
