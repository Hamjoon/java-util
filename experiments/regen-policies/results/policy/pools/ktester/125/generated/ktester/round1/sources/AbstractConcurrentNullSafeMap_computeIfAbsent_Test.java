package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_computeIfAbsent_Test {

    private TestMap map;

    /**
     * Concrete implementation used for the tests.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    // --------------------------------------------------------------------- //
    private static Stream<Arguments> absentKeyScenarios() {
        return Stream.of(Arguments.of("absent1", (Function<String, String>) String::toUpperCase, "ABSENT1"), Arguments.of("absent2", (Function<String, String>) k -> null, null));
    }

    @Test
    void computeIfAbsent_KeyAbsent_MapsValueAndInvokesMappingFunction(@Mock Function<String, String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply("key1")).thenReturn("value1");
        // WHEN
        String result = map.computeIfAbsent("key1", mappingFunction);
        // THEN
        assertEquals("value1", result, "Returned value should be the one produced by the mapping function");
        assertEquals("value1", map.get("key1"), "The value must be stored in the map");
        verify(mappingFunction, times(1)).apply("key1");
    }

    @Test
    void computeIfAbsent_KeyPresent_DoesNotInvokeMappingFunction(@Mock Function<String, String> mappingFunction) {
        // GIVEN
        map.put("key2", "existing");
        // mappingFunction should not be called, but we still stub it to avoid NPE if called
        when(mappingFunction.apply(any())).thenReturn("newValue");
        // WHEN
        String result = map.computeIfAbsent("key2", mappingFunction);
        // THEN
        assertEquals("existing", result, "Existing value must be returned unchanged");
        assertEquals("existing", map.get("key2"), "Map content must stay unchanged");
        verify(mappingFunction, never()).apply(any());
    }

    @Test
    void computeIfAbsent_NullKey_HandlesCorrectly(@Mock Function<String, String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply(null)).thenReturn("nullKeyValue");
        // WHEN
        String result = map.computeIfAbsent(null, mappingFunction);
        // THEN
        assertEquals("nullKeyValue", result, "Result for null key should be the mapping function's return");
        assertEquals("nullKeyValue", map.get(null), "Null key must be stored with the computed value");
        verify(mappingFunction, times(1)).apply(null);
    }

    @Test
    void computeIfAbsent_MappingFunctionReturnsNull_StoresNullValue(@Mock Function<String, String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply("key3")).thenReturn(null);
        // WHEN
        String result = map.computeIfAbsent("key3", mappingFunction);
        // THEN
        assertNull(result, "Result should be null when mapping function returns null");
        assertTrue(map.containsKey("key3"), "Key must be present even if the associated value is null");
        assertNull(map.get("key3"), "Stored value for the key must be null");
        verify(mappingFunction, times(1)).apply("key3");
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_NullKey_MappingReturnsNull_DoesNotCreateEntry(@Mock Function<String, String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply(null)).thenReturn(null);
        // WHEN
        String result = map.computeIfAbsent(null, mappingFunction);
        // THEN
        assertNull(result, "Result must be null");
        assertFalse(map.containsKey(null), "Map must not contain a mapping for the null key");
        verify(mappingFunction, times(1)).apply(null);
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_AbsentKey_StoresReturnedValue(@Mock Function<String, String> mappingFunction) {
        // GIVEN
        when(mappingFunction.apply("absent")).thenReturn("ABSENT");
        // WHEN
        String result = map.computeIfAbsent("absent", mappingFunction);
        // THEN
        assertEquals("ABSENT", result);
        assertEquals("ABSENT", map.get("absent"));
        verify(mappingFunction, times(1)).apply("absent");
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_MappingFunctionThrowsException_Propagates(@Mock Function<String, String> mappingFunction) {
        // GIVEN
        RuntimeException boom = new RuntimeException("boom");
        when(mappingFunction.apply("errorKey")).thenThrow(boom);
        int initialSize = map.size();
        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> map.computeIfAbsent("errorKey", mappingFunction), "Exception must propagate");
        assertSame(boom, thrown);
        assertEquals(initialSize, map.size(), "Map size must stay unchanged after exception");
        verify(mappingFunction, times(1)).apply("errorKey");
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_NullMappingFunction_ThrowsNullPointerException() {
        // GIVEN
        // ensure map is not empty, but irrelevant
        map.put("anyKey", "value");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.computeIfAbsent("anyKey", null), "A null mappingFunction must cause NPE");
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_NullReturnDoesNotMaskValue_SubsequentCallExecutesAgain() {
        // First call returns null – no entry created
        Function<String, String> f1 = key -> null;
        assertNull(map.computeIfAbsent("nullValueKey", f1));
        assertFalse(map.containsKey("nullValueKey"), "Key must not be present after null return");
        // Second call returns a real value – entry must be stored
        Function<String, String> f2 = key -> "real";
        assertEquals("real", map.computeIfAbsent("nullValueKey", f2));
        assertTrue(map.containsKey("nullValueKey"));
        assertEquals("real", map.get("nullValueKey"));
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_KeyPresentNullValue_MappingReturnsNonNull() {
        // GIVEN
        map.put("presentNull", null);
        Function<String, String> mapping = key -> "computedValue";
        // WHEN
        String result = map.computeIfAbsent("presentNull", mapping);
        // THEN
        assertEquals("computedValue", result);
        assertEquals("computedValue", map.get("presentNull"));
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_KeyPresentNullValue_MappingReturnsNull_LeavesEntryUnchanged() {
        // GIVEN
        map.put("presentNull", null);
        Function<String, String> mapping = key -> null;
        // WHEN
        String result = map.computeIfAbsent("presentNull", mapping);
        // THEN
        assertNull(result);
        assertTrue(map.containsKey("presentNull"), "Original null entry must stay");
        assertNull(map.get("presentNull"));
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_MappingFunctionReceivesOriginalKey_Null() {
        // GIVEN
        final Object[] captured = new Object[1];
        Function<String, String> mapping = key -> {
            captured[0] = key;
            return "val";
        };
        // WHEN
        String result = map.computeIfAbsent(null, mapping);
        // THEN
        assertEquals("val", result);
        assertNull(captured[0], "Mapping function must receive null as key");
    }

    @Test
    void computeIfAbsent_MappingFunctionReceivesOriginalKey_NonNull() {
        // GIVEN
        final Object[] captured = new Object[1];
        Function<String, String> mapping = key -> {
            captured[0] = key;
            return "val";
        };
        // WHEN
        String result = map.computeIfAbsent("someKey", mapping);
        // THEN
        assertEquals("val", result);
        assertEquals("someKey", captured[0], "Mapping function must receive the exact key instance");
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_ConcurrentSingleExecution() throws Exception {
        // GIVEN
        final String key = "concurrentKey";
        final AtomicInteger counter = new AtomicInteger(0);
        Function<String, String> mapping = k -> {
            counter.incrementAndGet();
            // simulate some work
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            return "once";
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        Callable<String> task = () -> {
            // ensure both threads start at the same moment
            startLatch.await();
            return map.computeIfAbsent(key, mapping);
        };
        Future<String> f1 = executor.submit(task);
        Future<String> f2 = executor.submit(task);
        // release both threads
        startLatch.countDown();
        String r1 = f1.get(5, TimeUnit.SECONDS);
        String r2 = f2.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();
        // THEN
        assertEquals("once", r1);
        assertEquals("once", r2);
        assertEquals(1, counter.get(), "Mapping function must be executed exactly once");
        assertEquals(1, map.size(), "Map must contain a single entry after concurrent calls");
        assertEquals("once", map.get(key));
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_SizeUnchangedWhenKeyAlreadyExists() {
        // GIVEN
        map.put("existingKey", "v1");
        map.put("anotherKey", "v2");
        int initialSize = map.size();
        Function<String, String> mapping = k -> {
            throw new AssertionError("Should not be called");
        };
        // WHEN
        String result = map.computeIfAbsent("existingKey", mapping);
        // THEN
        assertEquals("v1", result);
        assertEquals(initialSize, map.size(), "Size must stay unchanged when key already exists");
    }

    @Test
    void computeIfAbsent_SizeIncreasesWhenNewKeyWithNonNullValue() {
        // GIVEN
        map.put("existingKey", "v1");
        map.put("anotherKey", "v2");
        int initialSize = map.size();
        Function<String, String> mapping = k -> "newValue";
        // WHEN
        String result = map.computeIfAbsent("newKey", mapping);
        // THEN
        assertEquals("newValue", result);
        assertEquals(initialSize + 1, map.size(), "Size must increase by one for a new entry");
        assertEquals("newValue", map.get("newKey"));
    }

    @Test
    void computeIfAbsent_SizeUnchangedWhenNewKeyMappingReturnsNull() {
        // GIVEN
        map.put("existingKey", "v1");
        map.put("anotherKey", "v2");
        int initialSize = map.size();
        Function<String, String> mapping = k -> null;
        // WHEN
        String result = map.computeIfAbsent("anotherNewKey", mapping);
        // THEN
        assertNull(result);
        assertEquals(initialSize, map.size(), "Size must stay unchanged when mapping returns null");
        assertFalse(map.containsKey("anotherNewKey"), "No entry should be added for null result");
    }

    // --------------------------------------------------------------------- //
    @Test
    void computeIfAbsent_KeyWithFaultyHashCode_PropagatesException() {
        // GIVEN
        Object badHashKey = new Object() {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad hash");
            }
        };
        Function<Object, String> mapping = k -> "value";
        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> map.computeIfAbsent(badHashKey, (Function) mapping), "Exception from hashCode must propagate");
        assertEquals("bad hash", thrown.getMessage());
        assertFalse(map.containsKey(badHashKey), "Map must remain unchanged");
    }

    @Test
    void computeIfAbsent_KeyWithFaultyEquals_PropagatesException() {
        // GIVEN
        Object badEqualsKey = new Object() {

            @Override
            public boolean equals(Object o) {
                throw new RuntimeException("bad equals");
            }

            @Override
            public int hashCode() {
                return 42;
            }
        };
        // Insert a different key to force a lookup that triggers equals()
        map.put("someOtherKey", "value");
        Function<Object, String> mapping = k -> "value";
        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> map.computeIfAbsent(badEqualsKey, (Function) mapping), "Exception from equals must propagate");
        assertEquals("bad equals", thrown.getMessage());
        assertFalse(map.containsKey(badEqualsKey), "Map must remain unchanged");
    }

    @ParameterizedTest
    @MethodSource("absentKeyScenarios")
    void computeIfAbsent_AbsentKey_Parameterized(String key, Function<String, String> mapping, String expected) {
        // WHEN
        String result = map.computeIfAbsent(key, mapping);
        // THEN
        assertEquals(expected, result);
        if (expected != null) {
            assertTrue(map.containsKey(key));
            assertEquals(expected, map.get(key));
        } else {
            assertFalse(map.containsKey(key), "Null result must not create an entry");
        }
    }
}
