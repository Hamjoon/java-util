package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableMap_equals_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
        sealedState = false;
        sealedSupplier = () -> sealedState;
        map = new SealableMap<>(sealedSupplier);
        map.put("a", 1);
        map.put("b", 2);
    }

    /**
     * Parameter provider for the “Equals with null” group **
     */
    private static Stream<EqualsNullCase> nullCases() {
        return Stream.of(new EqualsNullCase(null, false, "Comparing a SealableMap to null should return false."));
    }

    /**
     * Parameter provider for the “Equals with same underlying map” group **
     */
    private static Stream<EqualsSameMapCase> sameMapCases() {
        return Stream.of(new EqualsSameMapCase(new HashMap<String, Integer>() {

            {
                put("a", 1);
                put("b", 2);
            }
        }, true, "SealableMap wrapping a HashMap with entries {a=1, b=2} should be equal to another HashMap with the same entries."));
    }

    @Test
    void equalsIsReflexive() {
        assertTrue(map.equals(map), "An object must be equal to itself");
    }

    @Test
    void equalsWithIdenticalContentUnsealed() {
        SealableMap<String, Integer> other = new SealableMap<>(sealedSupplier);
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);
        other.put(null, null);
        assertEquals(map, other, "Maps with identical content should be equal when unsealed");
        assertEquals(other, map, "Equality must be symmetric");
    }

    @Test
    void equalsWithIdenticalContentSealed() {
        // seal both maps (same supplier instance)
        sealedState = true;
        SealableMap<String, Integer> other = new SealableMap<>(sealedSupplier);
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);
        other.put(null, null);
        assertEquals(map, other, "Maps with identical content should be equal when sealed");
    }

    @Test
    void equalsWithDifferentContent() {
        SealableMap<String, Integer> other = new SealableMap<>(sealedSupplier);
        other.put("one", 1);
        other.put("two", 2);
        // different value
        other.put("three", 99);
        other.put(null, null);
        assertNotEquals(map, other, "Maps with different content must not be equal");
    }

    @Test
    void equalsAgainstPlainHashMap() {
        Map<String, Integer> plain = new HashMap<>();
        plain.put("one", 1);
        plain.put("two", 2);
        plain.put("three", 3);
        plain.put(null, null);
        assertEquals(map, plain, "SealableMap should be equal to a plain Map with the same entries");
        assertEquals(plain, map, "Plain Map should be equal to SealableMap with the same entries");
    }

    @Test
    void equalsWhenSealedDoesNotThrow() {
        // seal the map under test
        sealedState = true;
        SealableMap<String, Integer> other = new SealableMap<>(sealedSupplier);
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);
        other.put(null, null);
        // The equals method should delegate to the underlying map and therefore not be affected by the sealed state.
        assertDoesNotThrow(() -> map.equals(other), "equals should not throw when the map is sealed");
        assertTrue(map.equals(other), "Sealed maps with identical content should still be equal");
    }

    @Test
    void equalsWithNull() {
        // Act
        boolean result = map.equals(tc.obj);
        // Assert
        assertEquals(tc.expected, result, tc.description);
    }

    @Test
    void equalsWithDifferentType() {
        Object other = new Object();
        assertNotEquals(map, other, "Map should not be equal to an object of a different type");
    }

    @Test
    void equalsIsTransitive() {
        SealableMap<String, Integer> a = new SealableMap<>(sealedSupplier);
        SealableMap<String, Integer> b = new SealableMap<>(sealedSupplier);
        SealableMap<String, Integer> c = new SealableMap<>(sealedSupplier);
        a.put("one", 1);
        a.put("two", 2);
        a.put(null, null);
        b.putAll(a);
        c.putAll(b);
        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(a, c, "Equality must be transitive");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("sameMapCases")
    void equalsWithSameUnderlyingMap(EqualsSameMapCase tc) {
        // Act
        boolean result = map.equals(tc.obj);
        // Assert
        assertEquals(tc.expected, result, tc.description);
        // Additionally verify symmetry when the plain map is on the left side.
        assertEquals(tc.expected, tc.obj.equals(map), "Symmetry check failed: " + tc.description);
    }
}
