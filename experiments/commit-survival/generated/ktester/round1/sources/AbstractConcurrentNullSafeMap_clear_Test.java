package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_clear_Test {

    private TestConcurrentNullSafeMap<String, Integer> mapUnderTest;

    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    /**
     * Concrete implementation used for the real‑map tests.
     */
    private static final class TestConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestConcurrentNullSafeMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test that uses a real ConcurrentHashMap.
        mapUnderTest = new TestConcurrentNullSafeMap<>(new ConcurrentHashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared after each test to avoid cross‑test contamination.
        mapUnderTest.clear();
    }

    @Test
    void clear_RemovesAllEntries_FromRealMap() {
        // given
        mapUnderTest.put("one", 1);
        mapUnderTest.put("two", 2);
        mapUnderTest.put("three", 3);
        assertEquals(3, mapUnderTest.size(), "pre‑condition: map should contain three entries");
        // when
        mapUnderTest.clear();
        // then
        assertTrue(mapUnderTest.isEmpty(), "map should be empty after clear()");
        assertEquals(0, mapUnderTest.size(), "size should be zero after clear()");
        assertNull(mapUnderTest.get("one"), "no entry should be present for key \"one\"");
    }

    @Test
    void clear_DelegatesToInternalMap_ClearIsInvoked() {
        // given
        // Use the mock internal map; we need a concrete subclass that receives the mock.
        TestConcurrentNullSafeMap<String, Integer> mapWithMock = new TestConcurrentNullSafeMap<>(mockInternalMap);
        // Ensure the mock starts with no interactions.
        verifyNoInteractions(mockInternalMap);
        // when
        mapWithMock.clear();
        // then
        verify(mockInternalMap, times(1)).clear();
        // No other methods should be called on the mock.
        verifyNoMoreInteractions(mockInternalMap);
    }

    @Test
    void clearOnNonEmptyMap_RemovesAllRegularEntries() {
        // given: two regular entries
        mapUnderTest.put("a", 10);
        mapUnderTest.put("b", 20);
        assertEquals(2, mapUnderTest.size());
        // when
        mapUnderTest.clear();
        // then
        assertTrue(mapUnderTest.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, mapUnderTest.size());
        assertNull(mapUnderTest.get("a"));
        assertNull(mapUnderTest.get("b"));
    }

    @Test
    void clearOnNonEmptyMap_RemovesNullKeyAndNullValue() {
        // given: a null key and a null value (the map masks them internally)
        mapUnderTest.put(null, null);
        mapUnderTest.put("x", null);
        mapUnderTest.put(null, 42);
        // null key masks to one entry, plus "x"
        assertEquals(2, mapUnderTest.size());
        // when
        mapUnderTest.clear();
        // then
        assertTrue(mapUnderTest.isEmpty());
        assertEquals(0, mapUnderTest.size());
        assertNull(mapUnderTest.get(null), "Lookup for null key should return null after clear");
    }

    @Test
    void clearOnEmptyMap_NoExceptionAndMapRemainsEmpty() {
        // given: a freshly created map (already empty)
        assertTrue(mapUnderTest.isEmpty());
        // when / then
        assertDoesNotThrow(() -> mapUnderTest.clear(), "clear() on an empty map should not throw");
        assertTrue(mapUnderTest.isEmpty(), "Map should still be empty after clear()");
        assertEquals(0, mapUnderTest.size());
    }

    @Test
    void clearAfterPutAll_EmptiesMapIncludingNullEntries() {
        // given: a source map with several entries, including a null key/value
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 1);
        source.put("y", 2);
        // null key
        source.put(null, 3);
        // null value
        source.put("z", null);
        mapUnderTest.putAll(source);
        assertEquals(4, mapUnderTest.size());
        // when
        mapUnderTest.clear();
        // then
        assertTrue(mapUnderTest.isEmpty());
        assertEquals(0, mapUnderTest.size());
        assertNull(mapUnderTest.get("x"));
        assertNull(mapUnderTest.get("y"));
        assertNull(mapUnderTest.get(null));
    }

    @Test
    void clearAfterReplaceOperations_MapIsEmpty() {
        // given: two entries
        mapUnderTest.put("k1", 100);
        mapUnderTest.put("k2", 200);
        // replace using replace(K,V)
        mapUnderTest.replace("k1", 101);
        // replace using replace(K,oldV,newV)
        mapUnderTest.replace("k2", 200, 202);
        assertEquals(2, mapUnderTest.size());
        // when
        mapUnderTest.clear();
        // then
        assertTrue(mapUnderTest.isEmpty());
        assertEquals(0, mapUnderTest.size());
        assertNull(mapUnderTest.get("k1"));
        assertNull(mapUnderTest.get("k2"));
    }

    @Test
    void clearDuringConcurrentUpdates_NoConcurrentModificationAndMapRemainsUsable() throws InterruptedException {
        // given: start a few worker threads that continuously put entries
        final int THREAD_COUNT = 4;
        final int DURATION_MS = 200;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // wait until all threads are ready
                    startLatch.await();
                    int counter = 0;
                    while (!stop.get()) {
                        String key = "t" + threadId + "-" + counter++;
                        mapUnderTest.put(key, counter);
                    }
                } catch (InterruptedException ignored) {
                }
            });
        }
        // start workers
        startLatch.countDown();
        // give them a moment to populate the map
        Thread.sleep(50);
        // when: clear while workers are still running
        mapUnderTest.clear();
        // stop workers and wait for termination
        stop.set(true);
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        // then: map should be empty and still usable
        assertTrue(mapUnderTest.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, mapUnderTest.size());
        // subsequent operations must not throw
        assertDoesNotThrow(() -> mapUnderTest.put("afterClear", 999));
        assertEquals(999, mapUnderTest.get("afterClear"));
    }

    @Test
    void clearDoesNotAffectOtherInstances_OnlyTargetMapIsEmptied() {
        // given: two independent map instances
        TestConcurrentNullSafeMap<String, Integer> firstMap = new TestConcurrentNullSafeMap<>(new ConcurrentHashMap<>());
        TestConcurrentNullSafeMap<String, Integer> secondMap = new TestConcurrentNullSafeMap<>(new ConcurrentHashMap<>());
        firstMap.put("a1", 1);
        firstMap.put("a2", 2);
        secondMap.put("b1", 10);
        secondMap.put("b2", 20);
        assertEquals(2, firstMap.size());
        assertEquals(2, secondMap.size());
        // when
        firstMap.clear();
        // then
        assertTrue(firstMap.isEmpty(), "First map should be empty after clear()");
        assertEquals(0, firstMap.size());
        assertFalse(secondMap.isEmpty(), "Second map must remain untouched");
        assertEquals(2, secondMap.size());
        assertEquals(10, secondMap.get("b1"));
        assertEquals(20, secondMap.get("b2"));
    }

    @Test
    void testClearWithNullInternalMap_ThrowsNullPointerException() throws Exception {
        // given: a map whose internal delegate is forcibly set to null via reflection
        TestConcurrentNullSafeMap<String, Integer> mapWithNullDelegate = new TestConcurrentNullSafeMap<>(new ConcurrentHashMap<>());
        // Use reflection to null‑out the final field 'internalMap'
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(mapWithNullDelegate, null);
        // when / then
        assertThrows(NullPointerException.class, mapWithNullDelegate::clear, "clear() should throw NPE when internalMap is null");
    }

    @Test
    void testClearUnsupportedOperation_PropagatesUnsupportedOperationException() {
        // given: mock that throws UnsupportedOperationException on clear()
        ConcurrentMap<Object, Object> throwingMock = mock(ConcurrentMap.class);
        doThrow(new UnsupportedOperationException("clear not supported")).when(throwingMock).clear();
        TestConcurrentNullSafeMap<String, Integer> map = new TestConcurrentNullSafeMap<>(throwingMock);
        // when / then
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, map::clear, "clear() should propagate UnsupportedOperationException");
        assertEquals("clear not supported", ex.getMessage());
    }

    @Test
    void testClearSecurityException_PropagatesSecurityException() {
        // given: mock that throws SecurityException on clear()
        ConcurrentMap<Object, Object> securityMock = mock(ConcurrentMap.class);
        doThrow(new SecurityException("access denied")).when(securityMock).clear();
        TestConcurrentNullSafeMap<String, Integer> map = new TestConcurrentNullSafeMap<>(securityMock);
        // when / then
        SecurityException ex = assertThrows(SecurityException.class, map::clear, "clear() should propagate SecurityException");
        assertEquals("access denied", ex.getMessage());
    }

    @Test
    void testClearRuntimeExceptionPropagation_PropagatesRuntimeException() {
        // given: mock that throws a generic RuntimeException
        ConcurrentMap<Object, Object> runtimeMock = mock(ConcurrentMap.class);
        doThrow(new RuntimeException("boom")).when(runtimeMock).clear();
        TestConcurrentNullSafeMap<String, Integer> map = new TestConcurrentNullSafeMap<>(runtimeMock);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, map::clear, "clear() should propagate RuntimeException");
        assertEquals("boom", ex.getMessage());
    }
}
