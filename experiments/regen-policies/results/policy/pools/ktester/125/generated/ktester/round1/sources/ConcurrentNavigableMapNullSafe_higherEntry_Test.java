package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_higherEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    private static class BasicCase {

        // input key (null literal represented by null)
        final String key;

        // expected entry key (null if entry is null)
        final String expectedKey;

        // expected entry value (null if entry is null)
        final String expectedValue;

        // exception class, if any
        final Class<? extends Throwable> expectedException;

        final String description;

        BasicCase(String key, String expectedKey, String expectedValue, Class<? extends Throwable> expectedException, String description) {
            this.key = key;
            this.expectedKey = expectedKey;
            this.expectedValue = expectedValue;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    private static class MaskNullReturningNullMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        @Override
        protected Object maskNullKey(String key) {
            return "trigger".equals(key) ? null : super.maskNullKey(key);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default map used by the simple tests in the original framework.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is considered the greatest key.
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    private static Stream<BasicCase> basicCases() {
        return Stream.of(new BasicCase(null, "A", "valueA", null, "When the supplied key is null, the method should mask the null key and return the first entry."), new BasicCase("A", "B", "valueB", null, "Key \"A\" exists – higherEntry should return the immediate successor \"B\"."), new BasicCase("B", "C", "valueC", null, "Key \"B\" exists – higherEntry should return \"C\"."), new BasicCase("C", null, null, null, "Key \"C\" is the greatest – higherEntry should return null."), new BasicCase("0", "A", "valueA", null, "Key \"0\" is smaller than any existing key – should return first entry."), new BasicCase("Z", null, null, null, "Key \"Z\" is larger than any entry – should return null."), new BasicCase("UUID", null, null, ClassCastException.class, "Using a non‑comparable key (UUID) must propagate ClassCastException."));
    }

    @Test
    void higherEntry_ofExistingKey_returnsNextEntry() {
        Map.Entry<String, Integer> entry = map.higherEntry("a");
        assertNotNull(entry, "higherEntry(\"a\") should not be null");
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.getValue());
        entry = map.higherEntry("b");
        assertNotNull(entry, "higherEntry(\"b\") should not be null");
        assertEquals("c", entry.getKey());
        assertEquals(3, entry.getValue());
    }

    @Test
    void higherEntry_ofLastNonNullKey_returnsNullKeyEntry() {
        Map.Entry<String, Integer> entry = map.higherEntry("c");
        assertNotNull(entry, "higherEntry(\"c\") should return the null‑key entry");
        assertNull(entry.getKey(), "The key of the returned entry should be null");
        assertEquals(99, entry.getValue());
    }

    @Test
    void higherEntry_ofNullKey_returnsNull() {
        assertNull(map.higherEntry(null), "higherEntry(null) must return null because null is the greatest key");
    }

    @Test
    void higherEntry_ofNonExistingKey_returnsCorrectHigherEntry() {
        // "aa" is between "a" and "b"
        Map.Entry<String, Integer> entry = map.higherEntry("aa");
        assertNotNull(entry);
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.getValue());
        // "z" is greater than any non‑null key, so the next higher entry is the null key.
        entry = map.higherEntry("z");
        assertNotNull(entry);
        assertNull(entry.getKey());
        assertEquals(99, entry.getValue());
    }

    @Test
    void higherEntry_onEmptyMap_returnsNull() {
        map.clear();
        assertNull(map.higherEntry("any"), "higherEntry on an empty map must return null");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicCases")
    void higherEntry_basicFunctionality(BasicCase tc) {
        // Build a fresh map with the required entries for the basic tests.
        ConcurrentNavigableMapNullSafe<String, String> basicMap = new ConcurrentNavigableMapNullSafe<>();
        basicMap.put("A", "valueA");
        basicMap.put("B", "valueB");
        basicMap.put("C", "valueC");
        // The special case for the UUID test – we use an actual UUID instance.
        Object inputKey = tc.key;
        if ("UUID".equals(tc.key)) {
            inputKey = new UUID(0L, 0L);
        }
        if (tc.expectedException != null) {
            assertThrows(tc.expectedException, () -> basicMap.higherEntry((String) inputKey), tc.description);
        } else {
            Map.Entry<String, String> entry = basicMap.higherEntry((String) inputKey);
            if (tc.expectedKey == null) {
                assertNull(entry, tc.description);
            } else {
                assertNotNull(entry, tc.description);
                assertEquals(tc.expectedKey, entry.getKey(), "Returned key mismatch");
                assertEquals(tc.expectedValue, entry.getValue(), "Returned value mismatch");
            }
        }
    }

    @Test
    void higherEntry_emptyMap_nullKey_returnsNull() {
        map.clear();
        assertNull(map.higherEntry(null), "Empty map + null key must return null");
    }

    @Test
    void higherEntry_emptyMap_nonNullKey_returnsNull() {
        map.clear();
        assertNull(map.higherEntry("anyKey"), "Empty map + any key must return null");
    }

    @Test
    void higherEntry_singleElementMap_keySmaller_returnsOnlyEntry() {
        ConcurrentNavigableMapNullSafe<String, String> single = new ConcurrentNavigableMapNullSafe<>();
        single.put("X", "valueX");
        Map.Entry<String, String> entry = single.higherEntry("0");
        assertNotNull(entry);
        assertEquals("X", entry.getKey());
        assertEquals("valueX", entry.getValue());
    }

    @Test
    void higherEntry_singleElementMap_keyEqual_returnsNull() {
        ConcurrentNavigableMapNullSafe<String, String> single = new ConcurrentNavigableMapNullSafe<>();
        single.put("X", "valueX");
        assertNull(single.higherEntry("X"), "Exact key should yield null");
    }

    @Test
    void higherEntry_singleElementMap_keyGreater_returnsNull() {
        ConcurrentNavigableMapNullSafe<String, String> single = new ConcurrentNavigableMapNullSafe<>();
        single.put("X", "valueX");
        assertNull(single.higherEntry("Z"), "Key greater than sole entry should yield null");
    }

    @Test
    void higherEntry_onlyMaskedNullKey_nullKey_returnsNull() {
        ConcurrentNavigableMapNullSafe<String, Integer> onlyNull = new ConcurrentNavigableMapNullSafe<>();
        // only a null key
        onlyNull.put(null, 42);
        assertNull(onlyNull.higherEntry(null), "Only null key – higherEntry(null) must be null");
    }

    @Test
    void higherEntry_onlyMaskedNullKey_nonNullKey_returnsNull() {
        ConcurrentNavigableMapNullSafe<String, Integer> onlyNull = new ConcurrentNavigableMapNullSafe<>();
        onlyNull.put(null, 42);
        assertNull(onlyNull.higherEntry("A"), "Only null key – any non‑null key must yield null");
    }

    @Test
    void higherEntry_caseInsensitiveComparator() {
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        ConcurrentNavigableMapNullSafe<String, String> ciMap = new ConcurrentNavigableMapNullSafe<>(caseInsensitive);
        ciMap.put("a", "valueA");
        ciMap.put("B", "valueB");
        ciMap.put("c", "valueC");
        // a (or A) -> higher entry should be B
        Map.Entry<String, String> e1 = ciMap.higherEntry("a");
        assertNotNull(e1);
        assertEquals("B", e1.getKey());
        assertEquals("valueB", e1.getValue());
        // B -> higher entry should be c
        Map.Entry<String, String> e2 = ciMap.higherEntry("B");
        assertNotNull(e2);
        assertEquals("c", e2.getKey());
        assertEquals("valueC", e2.getValue());
        // c is highest according to case‑insensitive order
        assertNull(ciMap.higherEntry("c"));
    }

    @Test
    void higherEntry_reverseOrderComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, String> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        revMap.put("A", "valueA");
        revMap.put("B", "valueB");
        revMap.put("C", "valueC");
        // In reverse order, higherEntry("B") -> "A"
        Map.Entry<String, String> e1 = revMap.higherEntry("B");
        assertNotNull(e1);
        assertEquals("A", e1.getKey());
        assertEquals("valueA", e1.getValue());
        // higherEntry("C") -> "B"
        Map.Entry<String, String> e2 = revMap.higherEntry("C");
        assertNotNull(e2);
        assertEquals("B", e2.getKey());
        assertEquals("valueB", e2.getValue());
        // "A" is greatest in reverse order
        assertNull(revMap.higherEntry("A"));
    }

    @Test
    void higherEntry_entrySetValueUpdatesMap() {
        ConcurrentNavigableMapNullSafe<String, String> m = new ConcurrentNavigableMapNullSafe<>();
        m.put("A", "valueA");
        m.put("B", "valueB");
        Map.Entry<String, String> entry = m.higherEntry("A");
        assertNotNull(entry);
        assertEquals("B", entry.getKey());
        // mutate via Entry#setValue
        entry.setValue("newValueB");
        assertEquals("newValueB", m.get("B"), "Underlying map must reflect the new value");
    }

    @Test
    void higherEntry_comparatorThrowsException() {
        Comparator<String> exploding = (o1, o2) -> {
            throw new IllegalStateException("Comparator exploded");
        };
        ConcurrentNavigableMapNullSafe<String, String> explodingMap = new ConcurrentNavigableMapNullSafe<>(exploding);
        explodingMap.put("X", "x");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> explodingMap.higherEntry("anyKey"));
        assertEquals("Comparator exploded", ex.getMessage());
    }

    @Test
    void higherEntry_maskNullKeyReturnsNull_causesNPE() {
        MaskNullReturningNullMap broken = new MaskNullReturningNullMap();
        broken.put("A", 1);
        broken.put("B", 2);
        assertThrows(NullPointerException.class, () -> broken.higherEntry("trigger"), "maskNullKey returning null should cause NPE from ConcurrentSkipListMap");
    }

    @Test
    void higherEntry_internalMapNotConcurrentSkipListMap_throwsClassCastException() throws Exception {
        // Create a normal instance then replace its internalMap via reflection.
        ConcurrentNavigableMapNullSafe<String, Integer> corrupted = new ConcurrentNavigableMapNullSafe<>();
        corrupted.put("A", 1);
        corrupted.put("B", 2);
        // Access the private field declared in AbstractConcurrentNullSafeMap.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // replace with plain HashMap
        internalMapField.set(corrupted, new HashMap<>());
        assertThrows(ClassCastException.class, () -> corrupted.higherEntry("A"), "Casting internalMap to ConcurrentSkipListMap should fail");
    }
}
