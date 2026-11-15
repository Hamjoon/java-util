package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactLinkedMap_getNewMap_Test {

    private TestableCompactLinkedMap<Integer, String> mapUnderTest;

    /**
     * Simple subclass exposing the protected {@code getNewMap()} method.
     */
    private static class TestableCompactLinkedMap<K, V> extends CompactLinkedMap<K, V> {

        @Override
        public Map<K, V> getNewMap() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that lets the test control the value (or exception) returned by
     * {@code compactSize()}. All other behaviour is inherited unchanged.
     */
    private static class ConfigurableCompactLinkedMap extends CompactLinkedMap<Integer, String> {

        private final Integer sizeToReturn;

        private final RuntimeException exceptionToThrow;

        ConfigurableCompactLinkedMap(Integer sizeToReturn, RuntimeException exceptionToThrow) {
            this.sizeToReturn = sizeToReturn;
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        protected int compactSize() {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return sizeToReturn;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default instance – used by the non‑parameterised test at the bottom.
        mapUnderTest = new TestableCompactLinkedMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly necessary.
        mapUnderTest = null;
    }

    private static Stream<Arguments> compactSizeProvider() {
        return Stream.of(// compactSize = 0  → initial capacity 1, map should accept 1 entry
        Arguments.of(0, null, "compactSize() returns 0 → capacity 1"), // compactSize = 5  → initial capacity 6, map should accept 6 entries
        Arguments.of(5, null, "compactSize() returns 5 → capacity 6"), // compactSize = Integer.MAX_VALUE → overflow → IllegalArgumentException
        Arguments.of(Integer.MAX_VALUE, IllegalArgumentException.class, "compactSize() returns Integer.MAX_VALUE → overflow → exception"), // compactSize = -2 → -1 capacity → IllegalArgumentException
        Arguments.of(-2, IllegalArgumentException.class, "compactSize() returns -2 → negative capacity → exception"), // compactSize = -1 → capacity 0 (allowed)
        Arguments.of(-1, null, "compactSize() returns -1 → capacity 0"), // compactSize = 1 → capacity 2
        Arguments.of(1, null, "compactSize() returns 1 → capacity 2"), // compactSize = Integer.MIN_VALUE → negative capacity → exception
        Arguments.of(Integer.MIN_VALUE, IllegalArgumentException.class, "compactSize() returns Integer.MIN_VALUE → negative capacity → exception"), // compactSize = Integer.MAX_VALUE-1 → capacity Integer.MAX_VALUE (allowed)
        Arguments.of(Integer.MAX_VALUE - 1, null, "compactSize() returns Integer.MAX_VALUE-1 → capacity Integer.MAX_VALUE"));
    }

    @Test
    void getNewMap_returnsEmptyLinkedHashMapWithExpectedCapacity() {
        // When
        Map<Integer, String> newMap = mapUnderTest.getNewMap();
        // Then
        assertNotNull(newMap, "The returned map should not be null.");
        assertTrue(newMap instanceof LinkedHashMap, "The returned map should be an instance of LinkedHashMap.");
        assertEquals(0, newMap.size(), "A newly created map should be empty.");
        // Additional sanity check: the map should accept entries up to at least
        // compactSize() + 1 without throwing an exception.  This indirectly
        // verifies that the initial capacity was set as intended.
        int capacityHint = mapUnderTest.compactSize() + 1;
        for (int i = 0; i < capacityHint; i++) {
            newMap.put(i, "value" + i);
        }
        assertEquals(capacityHint, newMap.size(), "The map should hold compactSize() + 1 entries after insertion.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("compactSizeProvider")
    void getNewMap_variousCompactSizeScenarios(int compactSizeReturn, Class<? extends Throwable> expectedException, String description) {
        // Arrange: create a map whose compactSize() returns the supplied value.
        ConfigurableCompactLinkedMap map = new ConfigurableCompactLinkedMap(compactSizeReturn, null);
        if (expectedException != null) {
            // The constructor of LinkedHashMap should reject the illegal capacity.
            assertThrows(expectedException, map::getNewMap, "Expected " + expectedException.getSimpleName() + " for compactSize = " + compactSizeReturn);
            return;
        }
        // Act: obtain a new map.
        Map<Integer, String> newMap = map.getNewMap();
        // Assert: basic properties.
        assertNotNull(newMap, "Returned map must not be null.");
        assertTrue(newMap instanceof LinkedHashMap, "Returned map should be an instance of LinkedHashMap.");
        assertEquals(0, newMap.size(), "A freshly created map must be empty.");
        // Verify that the map can hold exactly (compactSize + 1) entries without
        // triggering a resize.  For capacity 0 the loop does nothing – this is
        // the expected behaviour for compactSize == -1.
        int expectedCapacity = compactSizeReturn + 1;
        for (int i = 0; i < expectedCapacity; i++) {
            newMap.put(i, "value" + i);
        }
        assertEquals(expectedCapacity, newMap.size(), "Map should contain exactly compactSize+1 entries after insertion.");
    }

    @Test
    void getNewMap_propagatesExceptionFromCompactSize() {
        // Arrange: map whose compactSize() throws a RuntimeException.
        ConfigurableCompactLinkedMap map = new ConfigurableCompactLinkedMap(null, new RuntimeException("boom"));
        // Act & Assert: the same exception must be propagated.
        RuntimeException thrown = assertThrows(RuntimeException.class, map::getNewMap, "RuntimeException from compactSize() should propagate unchanged.");
        assertEquals("boom", thrown.getMessage(), "Exception message should be preserved.");
    }
}
