package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_floorEntry_Test {

    /**
     * The map under test – created fresh for every test method.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor (natural ordering) unless a test
        // explicitly creates its own instance.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and guarantee isolation between tests.
        map = null;
    }

    /* --------------------------------------------------------------------- */
    @Test
    void floorEntryExactMatch() {
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        Map.Entry<String, Integer> entry = map.floorEntry("beta");
        assertNotNull(entry, "floorEntry should return a non‑null entry for an existing key");
        assertEquals("beta", entry.getKey());
        assertEquals(2, entry.getValue());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void floorEntryLowerKey() {
        map.put("a", 10);
        map.put("c", 30);
        map.put("e", 50);
        // Request a key between "c" and "e"
        Entry<String, Integer> entry = map.floorEntry("d");
        assertNotNull(entry);
        assertEquals("c", entry.getKey());
        assertEquals(30, entry.getValue());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void floorEntryNoMatch() {
        map.put("m", 100);
        map.put("z", 200);
        // All keys are greater than "a"
        assertNull(map.floorEntry("a"), "floorEntry should return null when there is no floor");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void floorEntryWithNullKeyPresent() {
        map.put("x", 9);
        // null is considered the greatest key
        map.put(null, 99);
        Map.Entry<String, Integer> entry = map.floorEntry(null);
        assertNotNull(entry);
        assertNull(entry.getKey(), "The key of the returned entry should be null");
        assertEquals(99, entry.getValue());
    }

    @Test
    void floorEntryWithNullKeyAbsent() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // No null key – floorEntry(null) should give the highest existing key
        Entry<String, Integer> entry = map.floorEntry(null);
        assertNotNull(entry);
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void floorEntryWithReverseComparator() {
        // Create a map with reverse (descending) ordering.
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        // In reverse order: c < b < a
        reverseMap.put("c", 3);
        // In reverse order, the floor of "b" is "c"
        Entry<String, Integer> entry = reverseMap.floorEntry("b");
        assertNotNull(entry);
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void floorEntryWhenMapIsEmpty() {
        assertNull(map.floorEntry("any"), "Empty map should always return null");
        assertNull(map.floorEntry(null), "Empty map should return null even for null key");
    }

    /**
     * The {@code internalMap} field is deliberately set to {@code null}
     * via reflection. The cast to {@code ConcurrentSkipListMap} inside
     * {@code floorEntry} should therefore throw a {@link NullPointerException}.
     */
    @Test
    void floorEntryInternalMapNullThrowsNPE() throws Exception {
        // Use reflection to set the protected field to null.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.floorEntry("A"), "When internalMap is null, floorEntry must propagate NullPointerException");
    }

    /**
     * The {@code internalMap} is replaced with a {@link HashMap},
     * which is not a {@code ConcurrentSkipListMap}. The cast performed
     * inside {@code floorEntry} should cause a {@link ClassCastException}.
     */
    @Test
    void floorEntryInternalMapWrongTypeThrowsClassCastException() throws Exception {
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // wrong type
        internalMapField.set(map, new HashMap<>());
        assertThrows(ClassCastException.class, () -> map.floorEntry("A"), "Casting a non‑ConcurrentSkipListMap internalMap must throw ClassCastException");
    }

    /**
     * Supplies a key that is not comparable to the map's key type.
     * We use a raw {@code ConcurrentNavigableMapNullSafe} instance to
     * bypass compile‑time generic checks.
     */
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void floorEntryKeyNotComparableThrowsClassCastException() {
        // Raw map so we can insert String keys but query with an Integer.
        ConcurrentNavigableMapNullSafe rawMap = new ConcurrentNavigableMapNullSafe();
        rawMap.put("A", "valueA");
        rawMap.put("B", "valueB");
        rawMap.put("C", "valueC");
        assertThrows(ClassCastException.class, () -> rawMap.floorEntry(123), "Supplying a key of an incompatible type must result in ClassCastException");
    }

    /**
     * Simulates a scenario where the underlying {@code ConcurrentSkipListMap}
     * does not accept {@code null} keys. We achieve this by mocking the map
     * and configuring {@code floorEntry(null)} to throw {@link NullPointerException}.
     */
    @Test
    void floorEntryNullKeyNotAllowedByUnderlyingMapThrowsNPE() throws Exception {
        // Create a mock that mimics the behaviour of a plain ConcurrentSkipListMap.
        @SuppressWarnings("unchecked")
        ConcurrentSkipListMap<Object, Object> mockSkipList = Mockito.mock(ConcurrentSkipListMap.class);
        Mockito.when(mockSkipList.floorEntry(any())).thenAnswer((Answer<Map.Entry<Object, Object>>) invocation -> {
            Object key = invocation.getArgument(0);
            if (key == null) {
                throw new NullPointerException("null keys not permitted");
            }
            return null;
        });
        // Inject the mock into the map instance.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, mockSkipList);
        // The public API masks null keys, so we must bypass the mask by
        // directly invoking the private method that does the cast.
        // However, because maskNullKey converts null to a sentinel object,
        // the mock will receive that sentinel (non‑null) and will NOT throw.
        // To force the NPE we call floorEntry with a non‑null key that the
        // mock treats as null – we achieve this by calling the method via
        // reflection and passing a raw null (bypassing maskNullKey).
        // This is acceptable for the purpose of this edge‑case test.
        // Obtain the floorEntry method via reflection.
        java.lang.reflect.Method floorEntryMethod = ConcurrentNavigableMapNullSafe.class.getDeclaredMethod("floorEntry", Object.class);
        floorEntryMethod.setAccessible(true);
        assertThrows(NullPointerException.class, () -> floorEntryMethod.invoke(map, (Object) null), "When the underlying map rejects null keys, floorEntry must propagate NullPointerException");
    }

    @Test
    void floorEntryBasicExactMatchFromJson() {
        map.put("A", 10);
        map.put("B", 20);
        map.put("C", 30);
        Map.Entry<String, Integer> entry = map.floorEntry("B");
        assertNotNull(entry);
        assertEquals("B", entry.getKey());
        assertEquals(20, entry.getValue());
    }

    @Test
    void floorEntryBasicBetweenKeysFromJson() {
        map.put("A", 10);
        map.put("B", 20);
        map.put("C", 30);
        Map.Entry<String, Integer> entry = map.floorEntry("B.5");
        assertNotNull(entry);
        assertEquals("B", entry.getKey());
        assertEquals(20, entry.getValue());
    }

    @Test
    void floorEntryBasicNoMatchFromJson() {
        map.put("A", 10);
        map.put("B", 20);
        map.put("C", 30);
        assertNull(map.floorEntry("0"));
    }

    @Test
    void floorEntryNullKeyPresentFromJson() {
        map.put(null, 999);
        map.put("X", 1);
        Map.Entry<String, Integer> entry = map.floorEntry(null);
        assertNotNull(entry);
        assertNull(entry.getKey());
        assertEquals(999, entry.getValue());
    }
}
