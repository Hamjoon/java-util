package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_containsValue_Test {

    private static final int CAPACITY = 5;

    private static final int CLEANUP_DELAY_MILLIS = 50;

    private ThreadedLRUCacheStrategy<String, String> cache;

    /**
     * Simple key class with custom equality based on an identifier.
     */
    private static final class CustomKey {

        private final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomKey))
                return false;
            CustomKey that = (CustomKey) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomKey{id='" + id + "'}";
        }
    }

    /**
     * Value class whose equals() deliberately throws an exception.
     */
    private static final class BadEquals {

        @Override
        public boolean equals(Object obj) {
            throw new IllegalArgumentException("Bad equals invoked");
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test to avoid cross‑test contamination.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to release references; also triggers any scheduled cleanup.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// null is stored
        Arguments.of(null, true), // existing string
        Arguments.of("test", true), // absent string
        Arguments.of("missing", false), // existing integer
        Arguments.of(42, true), // absent integer
        Arguments.of(99, false));
    }

    private static Stream<Arguments> emptyCacheProvider() {
        return Stream.of(Arguments.of(null, false), Arguments.of("any", false));
    }

    @Test
    void containsValue_returnsTrueWhenValueExists() {
        // given
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        // when
        boolean result = cache.containsValue("value1");
        // then
        assertTrue(result, "containsValue should return true for an existing value");
    }

    @Test
    void containsValue_returnsFalseWhenValueDoesNotExist() {
        // given
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        // when
        boolean result = cache.containsValue("missing");
        // then
        assertFalse(result, "containsValue should return false when the value is not present");
    }

    @Test
    void containsValue_handlesNullValueCorrectly() {
        // given
        cache.put("nullKey", null);
        cache.put("nonNullKey", "someValue");
        // when
        boolean containsNull = cache.containsValue(null);
        boolean containsSome = cache.containsValue("someValue");
        // then
        assertTrue(containsNull, "containsValue should return true for stored null value");
        assertTrue(containsSome, "containsValue should still work for non‑null values");
    }

    @Test
    void containsValue_returnsFalseWhenCacheIsEmpty() {
        // given – an empty cache (setupBeforeEach already provides this)
        // when
        boolean result = cache.containsValue("anything");
        // then
        assertFalse(result, "containsValue should return false on an empty cache");
    }

    @Test
    void containsValue_isIndependentOfKeyEquality() {
        // given
        cache.put("keyA", "sharedValue");
        // same value, different key
        cache.put("keyB", "sharedValue");
        // when
        boolean result = cache.containsValue("sharedValue");
        // then
        assertTrue(result, "containsValue should return true when the value is stored under any key");
    }

    /* --------------------------------------------------------------
     *  Basic functionality – parameterised for readability
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => value={0}, expected={1}")
    @MethodSource("basicFunctionalityProvider")
    void containsValue_basicFunctionality(Object value, boolean expected) {
        // given – populate cache with known values
        cache.put("k1", "test");
        cache.put("k2", null);
        cache.put("k3", 42);
        // when
        boolean actual = cache.containsValue(value);
        // then
        assertEquals(expected, actual, () -> "containsValue(" + value + ") should be " + expected);
    }

    /* --------------------------------------------------------------
     *  Empty cache
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => value={0}, expected={1}")
    @MethodSource("emptyCacheProvider")
    void containsValue_emptyCache(Object value, boolean expected) {
        // cache is already empty (setupBeforeEach)
        boolean actual = cache.containsValue(value);
        assertEquals(expected, actual, () -> "Empty cache: containsValue(" + value + ") should be " + expected);
    }

    /* --------------------------------------------------------------
     *  Duplicate values – key independence
     * -------------------------------------------------------------- */
    @Test
    void containsValue_duplicateValues() {
        cache.put("a", 42);
        // same value, different key
        cache.put("b", 42);
        cache.put("c", 7);
        assertTrue(cache.containsValue(42), "Duplicate value 42 should be found");
        assertFalse(cache.containsValue(99), "Value 99 is not present");
    }

    /* --------------------------------------------------------------
     *  Custom object equality
     * -------------------------------------------------------------- */
    @Test
    void containsValue_customObjectEquality() {
        CustomKey stored = new CustomKey("id1");
        cache.put("key", stored);
        // Different instance but equal according to CustomKey.equals()
        CustomKey query = new CustomKey("id1");
        assertTrue(cache.containsValue(query), "CustomKey with same id should be considered equal");
        CustomKey missing = new CustomKey("missing");
        assertFalse(cache.containsValue(missing), "CustomKey with different id should not be found");
    }

    /* --------------------------------------------------------------
     *  Type‑mismatch equality checks (Objects.equals)
     * -------------------------------------------------------------- */
    @Test
    void containsValue_typeMismatchEquality() {
        // Integer stored
        cache.put("intKey", 123);
        // String "123" should NOT match Integer 123
        assertFalse(cache.containsValue("123"), "String \"123\" must not be considered equal to Integer 123");
        // Same Integer (autoboxed) must match
        assertTrue(cache.containsValue(123), "Integer 123 should be found");
    }

    /* --------------------------------------------------------------
     *  Internal cache set to null – expect NPE
     * -------------------------------------------------------------- */
    @Test
    void containsValue_nullInternalCache_throwsNPE() throws Exception {
        // Use reflection to nullify the private 'cache' field
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.containsValue(null), "Calling containsValue when internal cache is null must throw NPE");
    }

    /* --------------------------------------------------------------
     *  Cache containing a null Node – expect NPE during iteration
     * -------------------------------------------------------------- */
    @Test
    void containsValue_nullNodeInCache_throwsNPE() throws Exception {
        // Mock a ConcurrentMap whose values() collection contains a single null entry
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Object>> mockedMap = mock(ConcurrentMap.class);
        Collection<ThreadedLRUCacheStrategy.Node<String, Object>> valuesWithNull = Collections.singletonList(null);
        when(mockedMap.values()).thenReturn(valuesWithNull);
        // Inject the mocked map
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, mockedMap);
        assertThrows(NullPointerException.class, () -> cache.containsValue("anything"), "Iterating over a null Node should cause NPE");
    }

    /* --------------------------------------------------------------
     *  Node holding a value whose equals() throws – expect that exception
     * -------------------------------------------------------------- */
    @Test
    void containsValue_badEqualsValue_throwsIllegalArgumentException() throws Exception {
        // Create a mock Node and set its 'value' field to an instance of BadEquals
        @SuppressWarnings("unchecked")
        ThreadedLRUCacheStrategy.Node<String, Object> badNode = mock(ThreadedLRUCacheStrategy.Node.class);
        BadEquals badEquals = new BadEquals();
        // Use reflection to set the private 'value' field inside the mock Node
        Field valueField = badNode.getClass().getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(badNode, badEquals);
        // Mock map to return a collection containing the bad node
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Object>> mockedMap = mock(ConcurrentMap.class);
        Collection<ThreadedLRUCacheStrategy.Node<String, Object>> collection = Collections.singleton(badNode);
        when(mockedMap.values()).thenReturn(collection);
        // Inject mocked map
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, mockedMap);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> cache.containsValue(null), "BadEquals.equals should propagate IllegalArgumentException");
        assertEquals("Bad equals invoked", thrown.getMessage());
    }
}
