package com.example.scheduler.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionTest {
    @Test
    void shouldCarryConflictStatusKeyAndArgsWithoutDetailsWhenCreatedWithConstructor() {
        var ex = new BusinessException(MessageKeys.APPOINTMENT_SLOT_TAKEN, 1, "a");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.APPOINTMENT_SLOT_TAKEN);
        assertThat(ex.getMessage()).isEqualTo(MessageKeys.APPOINTMENT_SLOT_TAKEN);
        assertThat(ex.getArgs()).containsExactly(1, "a");
        assertThat(ex.getDetails()).isNull();
    }

    @Test
    void shouldUseEmptyArgsWhenArgsAreNull() {
        var ex = new BusinessException(MessageKeys.APPOINTMENT_SLOT_TAKEN, (Object[]) null);

        assertThat(ex.getArgs()).isEmpty();
    }

    @Test
    void shouldExposeDetailsInInsertionOrderWhenCreatedWithDetails() {
        var details = new LinkedHashMap<String, Object>();
        details.put("b", 2);
        details.put("a", 1);

        var ex = BusinessException.withDetails(details, MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE, "x", "y", 0);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE);
        assertThat(ex.getArgs()).containsExactly("x", "y", 0);
        assertThat(ex.getDetails()).containsExactly(entry("b", 2), entry("a", 1));
    }

    @Test
    void shouldCopyDetailsDefensivelyWhenSourceMapChangesLater() {
        var details = new HashMap<String, Object>();
        details.put("count", 1L);

        var ex = BusinessException.withDetails(details, MessageKeys.SCHEDULE_EXCEPTION_ACTIVE_APPOINTMENTS_CONFLICT);
        details.put("count", 5L);
        details.put("extra", true);

        assertThat(ex.getDetails()).containsExactly(entry("count", 1L));
    }

    @Test
    void shouldNormalizeEmptyDetailsToNull() {
        var ex = BusinessException.withDetails(Map.of(), MessageKeys.APPOINTMENT_SLOT_TAKEN);

        assertThat(ex.getDetails()).isNull();
    }

    @Test
    void shouldNormalizeNullDetailsToNull() {
        var ex = BusinessException.withDetails(null, MessageKeys.APPOINTMENT_SLOT_TAKEN);

        assertThat(ex.getDetails()).isNull();
    }

    @Test
    void shouldMapEachSubclassToItsStatus() {
        assertThat(new BadRequestException("k").getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new UnauthorizedException("k").getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(new ForbiddenException("k").getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(new ResourceNotFoundException("k").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(new ServiceUnavailableException("k").getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
