package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_minus_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CompactMap instance before each test
        map = new CompactMap<>();
        // Populate with a few entries just to prove the map works normally before the minus call
        map.put("one", 1);
        map.put("two", 2);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required
        map = null;
    }

    /**
     * Supplies a stream of arguments covering all cases described in the JSON test data.
     *
     * @return a stream of {@link Arguments} where each argument is the value to be
     *         supplied to {@link #minus_VariousInputs_ShouldThrowUnsupportedOperationException(Object)}.
     */
    private static Stream<Arguments> minusUnsupportedArguments() {
        // The CompactMap instance itself is needed for one test case.
        // It will be created lazily inside the argument supplier to avoid static initialization problems.
        return Stream.of(// null
        Arguments.of((Object) null), // Simple non‑null objects
        Arguments.of("someKey"), Arguments.of(new HashMap<>()), // Primitive wrapper types
        Arguments.of(123), Arguments.of(45.67), Arguments.of(true), // Generic Object
        Arguments.of(new Object()), // Arrays
        Arguments.of(new int[] { 1, 2, 3 }), Arguments.of(new double[] { 1.1, 2.2 }), Arguments.of(new char[] { 'a', 'b' }), // Collections
        Arguments.of(new ArrayList<>()), Arguments.of(new HashSet<>()), // Other Map implementations
        Arguments.of(new LinkedHashMap<>()), Arguments.of(new TreeMap<>()), // JDK specific types
        Arguments.of(new BigInteger("12345678901234567890")), Arguments.of(new BigDecimal("12345.6789")), Arguments.of(LocalDate.of(2023, 1, 1)), Arguments.of(DayOfWeek.MONDAY), Arguments.of(new AtomicInteger(5)), Arguments.of(Optional.empty()), // Anonymous Runnable
        Arguments.of((Runnable) () -> {
            // no‑op
        }), // CompactMap instance itself – created at runtime
        Arguments.of(createSelfCompactMap()));
    }

    /**
     * Helper that creates a fresh {@link CompactMap} instance populated with two entries.
     * This mirrors the state of the test class's {@code map} field but is required for the
     * argument source (which must be static).  The returned instance is only used as the
     * argument to {@code minus}; it is not otherwise inspected.
     *
     * @return a {@code CompactMap<String,Integer>} containing two entries.
     */
    private static CompactMap<String, Integer> createSelfCompactMap() {
        CompactMap<String, Integer> self = new CompactMap<>();
        self.put("one", 1);
        self.put("two", 2);
        return self;
    }

    @Test
    void minus_ShouldThrowUnsupportedOperationException() {
        // GIVEN: a CompactMap instance with some entries (already set up in @BeforeEach)
        // WHEN / THEN: invoking minus(Object) must throw the expected exception
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> map.minus("one"), "Calling minus should throw UnsupportedOperationException");
        assertEquals("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.", ex.getMessage(), "Exception message should match the specification");
    }

    /**
     * Parameterized test that runs the same assertion for a large collection of
     * diverse argument types.  All inputs must result in the same
     * {@link UnsupportedOperationException} with the exact message defined by the
     * {@code CompactMap} implementation.
     *
     * @param removeMe the argument passed to {@code CompactMap#minus}
     */
    @ParameterizedTest(name = "{index} => removeMe={0}")
    @MethodSource("minusUnsupportedArguments")
    void minus_VariousInputs_ShouldThrowUnsupportedOperationException(Object removeMe) {
        // GIVEN: map is already populated by @BeforeEach
        // WHEN / THEN: minus must throw UnsupportedOperationException with the exact message
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> map.minus(removeMe), () -> "minus(" + String.valueOf(removeMe) + ") should throw UnsupportedOperationException");
        assertEquals("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.", ex.getMessage(), "Exception message must be consistent for all argument types");
    }
}
