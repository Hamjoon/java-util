package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_put_Test {

    private TestMap<String, String> map;

    /**
     * Simple concrete map using a regular {@link ConcurrentHashMap}.
     */
    private static final class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Concrete map that delegates to a supplied {@link ConcurrentMap}.
     */
    private static final class TestMapWithDelegate<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMapWithDelegate(ConcurrentMap<Object, Object> delegate) {
            super(delegate);
        }
    }

    /**
     * Subclass that deliberately returns {@code null} from {@code maskNullKey}.
     */
    private static final class NullKeyMaskingMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        NullKeyMaskingMap() {
            super(new ConcurrentHashMap<>());
        }

        @Override
        protected Object maskNullKey(K key) {
            // Simulate a buggy implementation that returns null for a null key.
            return key == null ? null : super.maskNullKey(key);
        }
    }

    /**
     * Subclass that deliberately returns {@code null} from {@code maskNullValue}.
     */
    private static final class NullValueMaskingMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        NullValueMaskingMap() {
            super(new ConcurrentHashMap<>());
        }

        @Override
        protected Object maskNullValue(V value) {
            // Simulate a buggy implementation that returns null for a null value.
            return value == null ? null : super.maskNullValue(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test to guarantee isolation.
        map = new TestMap<>();
        map = new TestMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid GC and avoid accidental cross‑test contamination.
        map.clear();
        map.clear();
    }

    private static Stream<Arguments> nullKeyNonNullValueProvider() {
        return Stream.of(// first insertion – no previous mapping
        Arguments.of(null, "first", null), // second insertion – should return the previous value "first"
        Arguments.of(null, "second", "first"));
    }

    private static Stream<Arguments> nonNullKeyNullValueProvider() {
        return Stream.of(// first insertion – no previous mapping
        Arguments.of("beta", null, null), // replace null with a non‑null value – previous unmasked value is null
        Arguments.of("beta", "notNull", null));
    }

    private static Stream<Arguments> nullKeyNullValueProvider() {
        return Stream.of(// first insertion – no previous mapping
        Arguments.of(null, null, null), // replace null value with a non‑null value – previous unmasked value is null
        Arguments.of(null, "filled", null));
    }

    private static Stream<Arguments> integerKeyNullValueProvider() {
        return Stream.of(// first insertion – no previous mapping
        Arguments.of(42, null, null), // replace null with non‑null value – previous is null
        Arguments.of(42, 99, null));
    }

    @Test
    void putNewEntryReturnsNullAndStoresValue() {
        String previous = map.put("key1", "value1");
        assertNull(previous, "First insertion should return null");
        assertEquals("value1", map.get("key1"), "Stored value should be retrievable");
        assertEquals(1, map.size(), "Map size should reflect the new entry");
    }

    @Test
    void putExistingKeyReturnsPreviousValueAndReplacesIt() {
        map.put("key2", "oldValue");
        String previous = map.put("key2", "newValue");
        assertEquals("oldValue", previous, "Previous value should be returned");
        assertEquals("newValue", map.get("key2"), "New value should replace the old one");
        assertEquals(1, map.size(), "Size should remain unchanged after replacement");
    }

    @Test
    void putNullKeyHandledCorrectly() {
        String previous = map.put(null, "nullKeyValue");
        assertNull(previous, "First insertion of a null key should return null");
        assertEquals("nullKeyValue", map.get(null), "Value associated with null key should be retrievable");
        assertEquals(1, map.size(), "Map size should count the null‑key entry");
    }

    @Test
    void putNullValueHandledCorrectly() {
        String previous = map.put("nullValueKey", null);
        assertNull(previous, "First insertion of a null value should return null");
        assertNull(map.get("nullValueKey"), "Stored null value should be returned by get");
        assertEquals(1, map.size(), "Map size should count the entry with a null value");
    }

    @Test
    void putNullKeyAndNullValueHandledCorrectly() {
        String previous = map.put(null, null);
        assertNull(previous, "First insertion of null key/value should return null");
        assertNull(map.get(null), "Retrieving a null key should return null value");
        assertEquals(1, map.size(), "Map size should count the null‑key/null‑value entry");
    }

    @Test
    void putMultipleEntriesIncludingNulls() {
        map.put("a", "A");
        map.put(null, "nullKey");
        map.put("b", null);
        // overwrite previous null‑key entry
        map.put(null, null);
        assertEquals("A", map.get("a"));
        assertNull(map.get("b"));
        assertNull(map.get(null));
        assertEquals(3, map.size(), "Map size should reflect three distinct keys (\"a\", \"b\", null)");
    }

    /**
     * Group: Put with null key and non‑null value (two cases).
     */
    @ParameterizedTest(name = "{index}: key={0}, value={1} → expectedPrev={2}")
    @MethodSource("nullKeyNonNullValueProvider")
    void putNullKeyNonNullValue(Object key, Object value, Object expectedPrev) {
        @SuppressWarnings("unchecked")
        TestMap<String, String> m = (TestMap<String, String>) map;
        Object prev = m.put((String) key, (String) value);
        assertEquals(expectedPrev, prev);
        assertEquals(value, m.get(key));
    }

    /**
     * Group: Put with non‑null key and null value (two cases).
     */
    @ParameterizedTest(name = "{index}: key={0}, value={1} → expectedPrev={2}")
    @MethodSource("nonNullKeyNullValueProvider")
    void putNonNullKeyNullValue(Object key, Object value, Object expectedPrev) {
        @SuppressWarnings("unchecked")
        TestMap<String, String> m = (TestMap<String, String>) map;
        Object prev = m.put((String) key, (String) value);
        assertEquals(expectedPrev, prev);
        assertEquals(value, m.get(key));
    }

    /**
     * Group: Put with both key and value null (two cases).
     */
    @ParameterizedTest(name = "{index}: key={0}, value={1} → expectedPrev={2}")
    @MethodSource("nullKeyNullValueProvider")
    void putNullKeyNullValue(Object key, Object value, Object expectedPrev) {
        @SuppressWarnings("unchecked")
        TestMap<String, String> m = (TestMap<String, String>) map;
        Object prev = m.put((String) key, (String) value);
        assertEquals(expectedPrev, prev);
        assertEquals(value, m.get(key));
    }

    /**
     * Group: Put with same key and same value (no change).
     */
    @Test
    void putSameKeySameValueReturnsPrevious() {
        map.put("gamma", "789");
        String previous = map.put("gamma", "789");
        assertEquals("789", previous, "Putting the identical key/value should return the previous value");
        assertEquals("789", map.get("gamma"));
        assertEquals(1, map.size());
    }

    /**
     * Group: Put replacing non‑null value with null.
     */
    @Test
    void putReplaceNonNullWithNullReturnsPrevious() {
        map.put("delta", "321");
        String previous = map.put("delta", null);
        assertEquals("321", previous, "Replacing a non‑null value with null should return the old value");
        assertNull(map.get("delta"));
        assertEquals(1, map.size());
    }

    /**
     * Group: Put replacing null value with null again.
     */
    @Test
    void putReplaceNullWithNullReturnsPreviousNull() {
        map.put("epsilon", null);
        String previous = map.put("epsilon", null);
        assertNull(previous, "Replacing a null value with null again should return null");
        assertNull(map.get("epsilon"));
        assertEquals(1, map.size());
    }

    /**
     * Group: Put with integer key and null value (two cases).
     */
    @ParameterizedTest(name = "{index}: key={0}, value={1} → expectedPrev={2}")
    @MethodSource("integerKeyNullValueProvider")
    void putIntegerKeyNullValue(Object key, Object value, Object expectedPrev) {
        TestMap<Integer, Integer> intMap = new TestMap<>();
        Integer prev = intMap.put((Integer) key, (Integer) value);
        assertEquals(expectedPrev, prev);
        assertEquals(value, intMap.get(key));
    }

    /**
     * Group: Put after clear on map with existing entries.
     */
    @Test
    void putAfterClearBehavesLikeEmptyMap() {
        // Populate map with several entries.
        map.put("x", "1");
        map.put("y", "2");
        map.put(null, "nullKey");
        assertEquals(3, map.size());
        // Clear the map.
        map.clear();
        assertEquals(0, map.size());
        // New insertion should behave as on an empty map.
        String previous = map.put("zeta", "555");
        assertNull(previous);
        assertEquals("555", map.get("zeta"));
        assertEquals(1, map.size());
    }

    /**
     * Group: Put on unmodifiable internal map throws {@link UnsupportedOperationException}.
     */
    @Test
    void putOnUnmodifiableInternalMapThrowsUnsupportedOperationException() {
        // Create a mock ConcurrentMap that throws UnsupportedOperationException on put().
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> unmodifiable = mock(ConcurrentMap.class);
        when(unmodifiable.put(any(), any())).thenThrow(new UnsupportedOperationException());
        TestMapWithDelegate<String, String> unmodMap = new TestMapWithDelegate<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> unmodMap.put("anyKey", "123"), "Putting into an unmodifiable internal map must propagate UnsupportedOperationException");
    }

    /**
     * Group: Put with null key when maskNullKey returns null throws {@link NullPointerException}.
     */
    @Test
    void putNullKeyWhenMaskReturnsNullThrowsNPE() {
        NullKeyMaskingMap<String, String> buggyMap = new NullKeyMaskingMap<>();
        assertThrows(NullPointerException.class, () -> buggyMap.put(null, "value"), "If maskNullKey returns null for a null key, the underlying map should throw NPE");
    }

    /**
     * Group: Put with null value when maskNullValue returns null throws {@link NullPointerException}.
     */
    @Test
    void putNullValueWhenMaskReturnsNullThrowsNPE() {
        NullValueMaskingMap<String, String> buggyMap = new NullValueMaskingMap<>();
        assertThrows(NullPointerException.class, () -> buggyMap.put("key", null), "If maskNullValue returns null for a null value, the underlying map should throw NPE");
    }

    /**
     * Group: Put where internalMap throws {@link IllegalArgumentException} for prohibited key.
     */
    @Test
    void putProhibitedKeyPropagatesIllegalArgumentException() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> validatingMap = mock(ConcurrentMap.class);
        // Simulate validation: throw IAE when key equals "badKey".
        when(validatingMap.put(eq("badKey"), any())).thenThrow(new IllegalArgumentException("bad key"));
        // For any other key, behave normally.
        when(validatingMap.put(any(), any())).thenAnswer(invocation -> {
            Object k = invocation.getArgument(0);
            Object v = invocation.getArgument(1);
            // simple in‑memory backing for the happy path
            return null;
        });
        TestMapWithDelegate<String, String> mapWithValidator = new TestMapWithDelegate<>(validatingMap);
        assertThrows(IllegalArgumentException.class, () -> mapWithValidator.put("badKey", "999"), "The IllegalArgumentException from the internal map must be propagated");
    }
}
