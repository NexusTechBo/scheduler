package com.example.scheduler.middleware;

import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.ActiveUserChecker;
import com.example.scheduler.security.JwtUtil;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter: B7 deactivated users lose access")
class JwtAuthFilterTest {
    private final JwtUtil jwtUtil = new JwtUtil("test-secret-test-secret-test-secret-0123456789");
    @Mock PatientRepository patientRepository;
    @Mock PersonalRepository personalRepository;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("active personal -> authenticated with ROLE_DOCTOR, only the personal table is checked")
    void filter_should_authenticate_when_personalActive() throws Exception {
        when(personalRepository.existsByIdAndActiveTrue(5L)).thenReturn(true);

        var auth = run(5L, "DOCTOR");

        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("5");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_DOCTOR");
        verifyNoInteractions(patientRepository);
    }

    @Test
    @DisplayName("deactivated personal -> request continues unauthenticated")
    void filter_should_notAuthenticate_when_personalInactive() throws Exception {
        when(personalRepository.existsByIdAndActiveTrue(5L)).thenReturn(false);

        assertThat(run(5L, "ASSISTANT")).isNull();
    }

    @Test
    @DisplayName("deactivated patient -> unauthenticated, checked against the patients table (ids are per table)")
    void filter_should_notAuthenticate_when_patientInactive() throws Exception {
        when(patientRepository.existsByIdAndActiveTrue(5L)).thenReturn(false);

        assertThat(run(5L, "PATIENT")).isNull();
        verify(personalRepository, never()).existsByIdAndActiveTrue(anyLong());
    }

    @Test
    @DisplayName("lookup fails (DB down, missing schema) -> request continues unauthenticated instead of a 500")
    void filter_should_notAuthenticate_when_lookupThrows() throws Exception {
        when(personalRepository.existsByIdAndActiveTrue(5L)).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThat(run(5L, "DOCTOR")).isNull();
    }

    @Test
    @DisplayName("invalid token -> no DB lookup, unauthenticated")
    void filter_should_skipLookup_when_tokenInvalid() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-jwt");
        var chain = new MockFilterChain();

        new JwtAuthFilter(jwtUtil, new ActiveUserChecker(patientRepository, personalRepository)).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).as("chain must continue").isNotNull();
        verifyNoInteractions(patientRepository, personalRepository);
    }

    /** Runs the filter and returns the Authentication seen by the next element of the chain. */
    private Authentication run(long userId, String role) throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtUtil.generate(userId, role, 1L, "Test"));
        var seen = new AtomicReference<Authentication>();
        var chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.@NonNull ServletRequest req, jakarta.servlet.@NonNull ServletResponse res) {
                seen.set(SecurityContextHolder.getContext().getAuthentication());
            }
        };
        new JwtAuthFilter(jwtUtil, new ActiveUserChecker(patientRepository, personalRepository)).doFilter(request, new MockHttpServletResponse(), chain);
        return seen.get();
    }
}
