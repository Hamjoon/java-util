package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_equals_Test {

    private Supplier<Boolean> sealedSupplier;

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> mapA;

    private SealableNavigableMap<String, Integer> mapB;

    /**
     * A {@link NavigableMap} that pretends to be equal to any other object.
     */
    private static class AlwaysTrueMap<K, V> extends TreeMap<K, V> {

        AlwaysTrueMap(Map<? extends K, ? extends V> m) {
            super(m);
        }

        @Override
        public boolean equals(Object o) {
            return true;
        }
    }

    /**
     * A {@link NavigableMap} whose {@code equals} method always throws a {@link RuntimeException}.
     */
    private static class ThrowingMap<K, V> extends TreeMap<K, V> {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("Deliberate exception from equals");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The map is always “unsealed” for the equality tests
        sealedSupplier = () -> false;
        // Create a mutable backing map and populate it with a few entries
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        // null key/value are allowed by the original tests
        backingMap.put(null, null);
        // Two SealableNavigableMap instances that start with the same content
        mapA = new SealableNavigableMap<>(backingMap, sealedSupplier);
        // mapB gets the same entries via putAll rather than sharing the same backing instance
        mapB = new SealableNavigableMap<>(new ConcurrentNavigableMapNullSafe<>(), sealedSupplier);
        mapB.putAll(backingMap);
    }

    /**
     * Supplies arguments for the basic delegation and null handling group.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>a description (for readability)</li>
     *   <li>a {@code Supplier<Object>} that produces the object to compare with {@code mapA}</li>
     *   <li>the expected outcome: {@code Boolean.TRUE}, {@code Boolean.FALSE} or an exception class</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> basicDelegationProvider() {
        return Stream.of(// null argument
        org.junit.jupiter.params.provider.Arguments.of("null argument", (Supplier<Object>) () -> null, Boolean.FALSE), // same instance
        org.junit.jupiter.params.provider.Arguments.of("same SealableNavigableMap instance", (Supplier<Object>) () -> mapA, Boolean.TRUE), // different SealableNavigableMap with same entries
        org.junit.jupiter.params.provider.Arguments.of("different SealableNavigableMap with same entries", (Supplier<Object>) () -> {
            SealableNavigableMap<String, Integer> other = new SealableNavigableMap<>(new ConcurrentNavigableMapNullSafe<>(), () -> false);
            other.putAll(backingMap);
            return other;
        }, Boolean.TRUE), // different SealableNavigableMap with different entries
        org.junit.jupiter.params.provider.Arguments.of("different SealableNavigableMap with different entries", (Supplier<Object>) () -> {
            SealableNavigableMap<String, Integer> other = new SealableNavigableMap<>(new ConcurrentNavigableMapNullSafe<>(), () -> false);
            other.putAll(backingMap);
            other.put("four", 4);
            return other;
        }, Boolean.FALSE), // plain NavigableMap (TreeMap) with same entries
        org.junit.jupiter.params.provider.Arguments.of("plain NavigableMap (TreeMap) with same entries", (Supplier<Object>) () -> {
            NavigableMap<String, Integer> plain = new TreeMap<>();
            plain.putAll(backingMap);
            return plain;
        }, Boolean.TRUE), // plain NavigableMap (TreeMap) with different entries
        org.junit.jupiter.params.provider.Arguments.of("plain NavigableMap (TreeMap) with different entries", (Supplier<Object>) () -> {
            NavigableMap<String, Integer> plain = new TreeMap<>();
            plain.putAll(backingMap);
            plain.put("four", 4);
            return plain;
        }, Boolean.FALSE));
    }

    /**
     * Additional delegation scenarios and edge cases.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> additionalDelegationProvider() {
        return Stream.of(// SealableNavigableMap wrapping a null internal map (simulated via reflection)
        org.junit.jupiter.params.provider.Arguments.of("SealableNavigableMap with null internal navMap", (Supplier<Object>) () -> {
            SealableNavigableMap<String, Integer> broken = new SealableNavigableMap<>(new ConcurrentNavigableMapNullSafe<>(), () -> false);
            // Force the internal navMap to null via reflection
            try {
                Field f = SealableNavigableMap.class.getDeclaredField("navMap");
                f.setAccessible(true);
                f.set(broken, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return broken;
        }, Boolean.FALSE), // Object of unrelated type (String)
        org.junit.jupiter.params.provider.Arguments.of("unrelated type String", (Supplier<Object>) () -> "test", Boolean.FALSE), // SealableNavigableMap wrapping a TreeMap with same entries
        org.junit.jupiter.params.provider.Arguments.of("SealableNavigableMap wrapping a TreeMap with same entries", (Supplier<Object>) () -> {
            SealableNavigableMap<String, Integer> other = new SealableNavigableMap<>(new TreeMap<>(), () -> false);
            other.putAll(backingMap);
            return other;
        }, Boolean.TRUE), // SealableNavigableMap wrapping a TreeMap with same keys but different values
        org.junit.jupiter.params.provider.Arguments.of("SealableNavigableMap wrapping a TreeMap with same keys but different values", (Supplier<Object>) () -> {
            SealableNavigableMap<String, Integer> other = new SealableNavigableMap<>(new TreeMap<>(), () -> false);
            // Same keys, different values
            other.put("one", 10);
            other.put("two", 20);
            other.put("three", 30);
            // TreeMap does not allow null key, but we ignore null here
            other.put(null, null);
            return other;
        }, Boolean.FALSE), // NavigableMap subclass that always returns true from equals
        org.junit.jupiter.params.provider.Arguments.of("NavigableMap subclass that always returns true", (Supplier<Object>) () -> new AlwaysTrueMap<>(backingMap), Boolean.TRUE), // NavigableMap subclass that throws RuntimeException from equals
        org.junit.jupiter.params.provider.Arguments.of("NavigableMap subclass that throws RuntimeException from equals", (Supplier<Object>) () -> new ThrowingMap<>(), RuntimeException.class), // Two empty SealableNavigableMap instances
        org.junit.jupiter.params.provider.Arguments.of("two empty SealableNavigableMap instances", (Supplier<Object>) () -> {
            SealableNavigableMap<String, Integer> empty1 = new SealableNavigableMap<>(new ConcurrentNavigableMapNullSafe<>(), () -> false);
            SealableNavigableMap<String, Integer> empty2 = new SealableNavigableMap<>(new ConcurrentNavigableMapNullSafe<>(), () -> false);
            // The test will compare mapA (populated) with empty2, so we need a wrapper that returns empty2
            // We'll compare empty1.equals(empty2) directly in the test method.
            return new Object[] { empty1, empty2 };
        }, Boolean.TRUE), // SealableNavigableMap wrapping a TreeMap with a null key (allowed by backingMap)
        org.junit.jupiter.params.provider.Arguments.of("SealableNavigableMap wrapping a TreeMap with null key (if allowed)", (Supplier<Object>) () -> {
            // TreeMap does NOT allow null keys, so we use ConcurrentNavigableMapNullSafe which does.
            SealableNavigableMap<String, Integer> other = new SealableNavigableMap<>(new ConcurrentNavigableMapNullSafe<>(), () -> false);
            other.putAll(backingMap);
            return other;
        }, Boolean.TRUE));
    }

    @Test
    void equalsIsReflexive() {
        assertTrue(mapA.equals(mapA), "An object must be equal to itself");
    }

    @Test
    void equalsIsSymmetricForIdenticalContent() {
        assertTrue(mapA.equals(mapB), "mapA should equal mapB when they contain the same entries");
        assertTrue(mapB.equals(mapA), "Symmetry: mapB should equal mapA");
    }

    @Test
    void equalsIsTransitiveForThreeIdenticalMaps() {
        SealableNavigableMap<String, Integer> mapC = new SealableNavigableMap<>(new ConcurrentNavigableMapNullSafe<>(), sealedSupplier);
        mapC.putAll(backingMap);
        assertTrue(mapA.equals(mapB));
        assertTrue(mapB.equals(mapC));
        assertTrue(mapA.equals(mapC), "Transitivity failed");
    }

    @Test
    void equalsReturnsFalseWhenOtherHasDifferentContent() {
        // Change mapB by adding an extra entry
        mapB.put("four", 4);
        assertFalse(mapA.equals(mapB), "Maps with different entries must not be equal");
    }

    @Test
    void equalsReturnsFalseWhenOtherIsNull() {
        assertFalse(mapA.equals(null), "Map should not be equal to null");
    }

    @Test
    void equalsReturnsFalseWhenOtherIsDifferentType() {
        assertFalse(mapA.equals("not a map"), "Map should not be equal to an object of a different type");
    }

    @Test
    void equalsReflectsChangesInUnderlyingBackingMap() {
        // Modify the original backing map directly (the underlying navMap of mapA)
        backingMap.put("extra", 99);
        // mapA should now see the new entry, so equality with mapB (which lacks it) is false
        assertFalse(mapA.equals(mapB), "Equality must reflect the current state of the underlying map");
        // Bring mapB up to date and equality should be restored
        mapB.put("extra", 99);
        assertTrue(mapA.equals(mapB), "After synchronising contents, maps must be equal again");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicDelegationProvider")
    void equalsBasicDelegationAndNullHandling(String description, Supplier<Object> otherSupplier, Object expected) {
        Object other = otherSupplier.get();
        if (expected instanceof Boolean) {
            boolean expectedBool = (Boolean) expected;
            assertEquals(expectedBool, mapA.equals(other), () -> "Unexpected equality result for case: " + description);
        } else if (expected instanceof Class) {
            // Not used in this provider but kept for symmetry with other providers
            fail("Unexpected exception expectation in basicDelegationProvider");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalDelegationProvider")
    void equalsAdditionalDelegationScenarios(String description, Supplier<Object> otherSupplier, Object expected) {
        Object other = otherSupplier.get();
        // Special handling for the "two empty SealableNavigableMap instances" case where we receive an array
        if (other instanceof Object[] arr && arr.length == 2 && arr[0] instanceof SealableNavigableMap && arr[1] instanceof SealableNavigableMap) {
            SealableNavigableMap<?, ?> empty1 = (SealableNavigableMap<?, ?>) arr[0];
            SealableNavigableMap<?, ?> empty2 = (SealableNavigableMap<?, ?>) arr[1];
            assertTrue(empty1.equals(empty2), "Empty SealableNavigableMap instances should be equal");
            return;
        }
        if (expected instanceof Boolean) {
            boolean expectedBool = (Boolean) expected;
            assertEquals(expectedBool, mapA.equals(other), () -> "Unexpected equality result for case: " + description);
        } else if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expecting an exception to be thrown from the delegated equals call
            assertThrows((Class<? extends Throwable>) expected, () -> mapA.equals(other), () -> "Expected exception for case: " + description);
        } else {
            fail("Invalid expectation type for case: " + description);
        }
    }
}
