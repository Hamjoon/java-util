package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactCILinkedMap_useCopyIterator_Test {

    /**
     * Simple concrete subclass that exposes the protected method for testing.
     */
    private static class TestableCompactCILinkedMap<K, V> extends CompactCILinkedMap<K, V> {

        /**
         * Public wrapper delegating to the protected {@code useCopyIterator}.
         */
        boolean callUseCopyIterator() {
            return super.useCopyIterator();
        }
    }

    /**
     * Provides test case descriptions. Each case expects {@code false} from {@code useCopyIterator()}.
     */
    private static Stream<Arguments> useCopyIteratorCases() {
        return Stream.of(Arguments.of("Base implementation should return false"), Arguments.of("Public wrapper should expose method and return false"));
    }

    @Test
    void useCopyIterator_ShouldReturnFalse() {
        // given
        TestableCompactCILinkedMap<String, Integer> map = new TestableCompactCILinkedMap<>();
        // when
        boolean result = map.callUseCopyIterator();
        // then
        assertFalse(result, "useCopyIterator() is expected to return false");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("useCopyIteratorCases")
    void useCopyIterator_ShouldAlwaysReturnFalse(String description) {
        // given: a fresh instance of the testable map
        TestableCompactCILinkedMap<String, Integer> map = new TestableCompactCILinkedMap<>();
        // when: invoking the protected method via the public wrapper
        boolean result = map.callUseCopyIterator();
        // then: the result must be false regardless of the scenario described
        assertFalse(result, "useCopyIterator() is expected to return false – " + description);
    }
}
