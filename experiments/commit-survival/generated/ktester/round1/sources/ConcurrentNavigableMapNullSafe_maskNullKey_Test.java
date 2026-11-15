package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_maskNullKey_Test {

    private ExposedMap<String, Integer> map;

    /**
     * A concrete subclass that makes the protected {@code maskNullKey} method public.
     */
    private static class ExposedMap<K, V> extends ConcurrentNavigableMapNullSafe<K, V> {

        public Object maskNullKeyPublic(K key) {
            return super.maskNullKey(key);
        }
    }

    /**
     * A tiny POJO used for the “custom object key” test case.
     */
    static class Person {

        String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person p = (Person) o;
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new ExposedMap<>();
    }

    /**
     * Parameter source for non‑null keys of various types.
     */
    private static Stream<Arguments> nonNullKeyProvider() {
        return Stream.of(Arguments.of("example", "example"), Arguments.of(123, 123), // looks like sentinel but is not
        Arguments.of("null_foo", "null_foo"));
    }

    @Test
    void maskNullKeyReturnsSentinelForNullKey() {
        Object masked = map.maskNullKeyPublic(null);
        assertNotNull(masked, "The sentinel for a null key must not be null");
        assertTrue(masked instanceof String, "The sentinel should be a String");
        String sentinel = (String) masked;
        assertTrue(sentinel.startsWith("null_"), "Sentinel should start with the prefix \"null_\" but was: " + sentinel);
    }

    @Test
    void maskNullKeyReturnsSameSentinelForMultipleNullKeys() {
        Object first = map.maskNullKeyPublic(null);
        Object second = map.maskNullKeyPublic(null);
        assertSame(first, second, "Multiple calls with a null key should return the identical sentinel instance");
    }

    @Test
    void maskNullKeyReturnsOriginalKeyWhenNonNull() {
        String key = "myKey";
        Object masked = map.maskNullKeyPublic(key);
        assertSame(key, masked, "When the key is non‑null the method must return the original key instance");
    }

    @Test
    void maskNullKeyDoesNotAlterNonNullKeyEquality() {
        String key1 = new String("duplicate");
        String key2 = new String("duplicate");
        // Ensure the two distinct objects are equal but not identical.
        assertNotSame(key1, key2);
        assertEquals(key1, key2);
        Object masked1 = map.maskNullKeyPublic(key1);
        Object masked2 = map.maskNullKeyPublic(key2);
        // The returned objects should be the original keys, preserving equality semantics.
        assertEquals(masked1, masked2);
        assertNotSame(masked1, masked2);
    }

    @ParameterizedTest(name = "maskNullKey returns unchanged value for non‑null key: {0}")
    @MethodSource("nonNullKeyProvider")
    void maskNullKeyReturnsUnchangedForVariousNonNullKeys(Object inputKey, Object expected) {
        // The map is typed with String keys, but the protected method is generic,
        // so we can pass any Object via raw type.
        @SuppressWarnings("unchecked")
        Object result = ((ExposedMap) map).maskNullKeyPublic(inputKey);
        assertSame(expected, result, "Non‑null key should be returned unchanged (reference equality)");
    }

    @Test
    void sentinelIsSameAcrossDistinctMapInstances() {
        ExposedMap<String, Integer> map1 = new ExposedMap<>();
        ExposedMap<String, Integer> map2 = new ExposedMap<>();
        Object sentinel1 = map1.maskNullKeyPublic(null);
        Object sentinel2 = map2.maskNullKeyPublic(null);
        assertSame(sentinel1, sentinel2, "Sentinel must be the same static instance across different map objects");
    }

    @Test
    void maskNullKeyLeavesCustomObjectKeyUntouched() {
        Person alice = new Person("Alice");
        Object result = map.maskNullKeyPublic(alice);
        assertSame(alice, result, "Custom object keys must be returned unchanged (reference equality)");
    }

    @Test
    void maskNullKeyLeavesStringBuilderUnchanged() {
        StringBuilder sb = new StringBuilder("test");
        Object result = map.maskNullKeyPublic(sb);
        assertSame(sb, result, "StringBuilder (a CharSequence) should be returned unchanged");
    }

    @Test
    void concurrentInvocationReturnsIdenticalSentinel() throws Exception {
        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> map.maskNullKeyPublic(null)));
        }
        // Wait for all tasks and collect results
        Set<Object> distinctResults = new HashSet<>();
        for (Future<Object> f : futures) {
            // get() propagates any exception
            distinctResults.add(f.get());
        }
        executor.shutdownNow();
        assertEquals(1, distinctResults.size(), "All threads must receive the exact same sentinel instance");
    }

    @Test
    void sentinelUnaffectedByMapMutation() {
        // Populate the map with regular entries.
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Now request the sentinel.
        Object sentinelAfterMutation = map.maskNullKeyPublic(null);
        assertNotNull(sentinelAfterMutation);
        assertTrue(sentinelAfterMutation instanceof String);
        assertTrue(((String) sentinelAfterMutation).startsWith("null_"));
    }

    @Test
    void sentinelTypeVerification() {
        Object sentinel = map.maskNullKeyPublic(null);
        assertTrue(sentinel instanceof String, "Sentinel for null key must be an instance of java.lang.String");
    }
}
