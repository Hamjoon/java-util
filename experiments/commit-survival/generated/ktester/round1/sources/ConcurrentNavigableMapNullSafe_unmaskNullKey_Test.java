package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_unmaskNullKey_Test {

    private TestableMap map;

    private StringMap stringMap;

    /**
     * A concrete subclass that makes the protected methods visible for testing.
     */
    private static class TestableMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        TestableMap() {
            super();
        }

        /**
         * Expose the protected {@code unmaskNullKey} method.
         */
        String unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        /**
         * Expose the protected {@code maskNullKey} method.
         */
        Object mask(String key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Map with <String, Integer> keys – used by the original tests.
     */
    private static class StringMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        StringMap() {
            super();
        }

        String unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        Object mask(String key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Map with <Integer, Integer> keys.
     */
    private static class IntegerMap extends ConcurrentNavigableMapNullSafe<Integer, Integer> {

        IntegerMap() {
            super();
        }

        Integer unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        Object mask(Integer key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Map with <Number, Integer> keys.
     */
    private static class NumberMap extends ConcurrentNavigableMapNullSafe<Number, Integer> {

        NumberMap() {
            super();
        }

        Number unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        Object mask(Number key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Map with <Object, Integer> keys.
     */
    private static class ObjectMap extends ConcurrentNavigableMapNullSafe<Object, Integer> {

        ObjectMap() {
            super();
        }

        Object unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        Object mask(Object key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Map with <List<?>, Integer> keys.
     */
    private static class ListMap extends ConcurrentNavigableMapNullSafe<List<?>, Integer> {

        ListMap() {
            super();
        }

        List<?> unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        Object mask(List<?> key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Map with <Comparable<?>, Integer> keys.
     */
    private static class ComparableMap extends ConcurrentNavigableMapNullSafe<Comparable<?>, Integer> {

        ComparableMap() {
            super();
        }

        Comparable<?> unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        Object mask(Comparable<?> key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Map with <int[], Integer> keys.
     */
    private static class IntArrayMap extends ConcurrentNavigableMapNullSafe<int[], Integer> {

        IntArrayMap() {
            super();
        }

        int[] unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        Object mask(int[] key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * Map with <Map<String,Integer>, Integer> keys.
     */
    private static class GenericMap extends ConcurrentNavigableMapNullSafe<Map<String, Integer>, Integer> {

        GenericMap() {
            super();
        }

        Map<String, Integer> unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }

        Object mask(Map<String, Integer> key) {
            return super.maskNullKey(key);
        }
    }

    /* --------------------------------------------------------------------- */
    private static class CustomKey {

        private final String name;

        CustomKey(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "CustomKey{" + name + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        stringMap = new StringMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        map = null;
    }

    /**
     * Data provider for cases where the masked key should be returned unchanged.
     */
    private static Stream<Arguments> unchangedKeyProvider() {
        return Stream.of(// Integer key type (K = Integer)
        Arguments.of(new IntegerMap(), 123, 123), // Number key type (K = Number) – Double value
        Arguments.of(new NumberMap(), 3.14, 3.14), // Object key generic (K = Object) – any string
        Arguments.of(new ObjectMap(), "anyString", "anyString"));
    }

    /**
     * Data provider for cases that must throw ClassCastException.
     */
    private static Stream<Arguments> classCastExceptionProvider() {
        return Stream.of(// K = Integer, maskedKey = "stringKey"
        Arguments.of(new IntegerMap(), "stringKey"), // K = String, maskedKey = new CustomKey("sentinel")
        Arguments.of(new StringMap(), new CustomKey("sentinel")), // K = Integer, maskedKey = 123L (Long)
        Arguments.of(new IntegerMap(), 123L), // K = List, maskedKey = new HashSet<>()
        Arguments.of(new ListMap(), new HashSet<>()), // K = Comparable, maskedKey = new Object()
        Arguments.of(new ComparableMap(), new Object()), // K = int[], maskedKey = "someString"
        Arguments.of(new IntArrayMap(), "someString"), // K = Map<String,Integer>, maskedKey = new ArrayList<>()
        Arguments.of(new GenericMap(), new ArrayList<>()));
    }

    @Test
    void unmaskNullKey_returnsNullWhenGivenSentinel() {
        // Arrange: obtain the sentinel that the implementation uses for a null key.
        // mask(null) returns the private sentinel string
        Object sentinel = map.mask(null);
        // Act: unmask the sentinel.
        String result = map.unmask(sentinel);
        // Assert: the result must be null.
        assertNull(result, "unmaskNullKey should return null when the masked key is the sentinel");
    }

    @Test
    void unmaskNullKey_returnsOriginalKeyWhenNotSentinel() {
        // Arrange: a regular (non‑null) key.
        String originalKey = "regularKey";
        // Act: unmask the key directly (no masking needed because it is not the sentinel).
        String result = map.unmask(originalKey);
        // Assert: the same key instance/value is returned.
        assertEquals(originalKey, result, "unmaskNullKey should return the original key when it is not the sentinel");
    }

    @Test
    void unmaskNullKey_behavesCorrectlyThroughPublicNavigationMethods() {
        // This test demonstrates that the protected method works as expected when
        // invoked indirectly via a public API (ceilingKey).
        // Insert a null key and a regular key.
        map.put(null, 100);
        map.put("alpha", 1);
        map.put("beta", 2);
        // The map is a NavigableMap; null keys are considered last in the ascending view.
        // Therefore, ceilingKey(null) should return null (the unmasked result).
        assertNull(map.ceilingKey(null), "ceilingKey(null) should return null after unmasking the sentinel");
        // For a regular key, the ceilingKey should return the key itself.
        assertEquals("alpha", map.ceilingKey("alpha"), "ceilingKey for an existing non‑null key should return that key");
    }

    /**
     * Group: handling of masked keys – null input (not sentinel).
     */
    @Test
    void unmaskNullKey_returnsNullWhenInputIsNull() {
        // Directly pass null; the sentinel check fails and the cast returns null.
        String result = stringMap.unmask(null);
        assertNull(result, "unmaskNullKey should return null when the masked key itself is null");
    }

    /**
     * Group: handling of masked keys – incompatible type (ClassCastException).
     */
    @Test
    void unmaskNullKey_throwsClassCastExceptionWhenIncompatibleType() {
        // K is String, but we supply an Integer.
        assertThrows(ClassCastException.class, () -> stringMap.unmask(12345), "unmaskNullKey must throw ClassCastException for an Integer when K is String");
    }

    /**
     * Group: duplicate sentinel instance – a new String with same contents.
     */
    @Test
    void unmaskNullKey_recognisesDuplicateSentinelInstance() {
        Object sentinel = stringMap.mask(null);
        // Create a distinct String object with identical characters.
        String duplicateSentinel = new String(sentinel.toString());
        String result = stringMap.unmask(duplicateSentinel);
        assertNull(result, "unmaskNullKey should treat any String equal to the sentinel as null");
    }

    /**
     * Group: different sentinel‑like string – should be returned unchanged.
     */
    @Test
    void unmaskNullKey_returnsValueWhenStringLooksLikeSentinelButDiffers() {
        String fakeSentinel = "null_00000000-0000-0000-0000-000000000000";
        String result = stringMap.unmask(fakeSentinel);
        assertEquals(fakeSentinel, result, "A string that merely resembles the sentinel must be returned unchanged");
    }

    @ParameterizedTest
    @MethodSource("unchangedKeyProvider")
    void unmaskNullKey_returnsMaskedKeyWhenCompatible(ConcurrentNavigableMapNullSafe<?, Integer> map, Object maskedKey, Object expected) {
        // Cast to raw type to call the exposed unmask method via reflection.
        // Each concrete subclass defines its own `unmask` method; we invoke it
        // through a lambda to keep the code simple.
        Object actual;
        if (map instanceof IntegerMap) {
            actual = ((IntegerMap) map).unmask(maskedKey);
        } else if (map instanceof NumberMap) {
            actual = ((NumberMap) map).unmask(maskedKey);
        } else if (map instanceof ObjectMap) {
            actual = ((ObjectMap) map).unmask(maskedKey);
        } else {
            throw new IllegalStateException("Unexpected map type");
        }
        assertEquals(expected, actual, "unmaskNullKey should return the original masked key when it is compatible with K");
    }

    @ParameterizedTest
    @MethodSource("classCastExceptionProvider")
    void unmaskNullKey_throwsClassCastExceptionWhenIncompatible(ConcurrentNavigableMapNullSafe<?, Integer> map, Object maskedKey) {
        // The exact unmask method signature depends on the concrete subclass.
        assertThrows(ClassCastException.class, () -> {
            if (map instanceof IntegerMap) {
                ((IntegerMap) map).unmask(maskedKey);
            } else if (map instanceof StringMap) {
                ((StringMap) map).unmask(maskedKey);
            } else if (map instanceof ListMap) {
                ((ListMap) map).unmask(maskedKey);
            } else if (map instanceof ComparableMap) {
                ((ComparableMap) map).unmask(maskedKey);
            } else if (map instanceof IntArrayMap) {
                ((IntArrayMap) map).unmask(maskedKey);
            } else if (map instanceof GenericMap) {
                ((GenericMap) map).unmask(maskedKey);
            } else {
                throw new IllegalStateException("Unsupported map type");
            }
        }, "unmaskNullKey must throw ClassCastException for incompatible masked key");
    }
}
