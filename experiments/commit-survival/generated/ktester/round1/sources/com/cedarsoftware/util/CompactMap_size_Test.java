package com.cedarsoftware.util;

import java.lang.reflect.Field;
import static org.mockito.Mockito.*;
// import java.util.concurrent.ConcurrentModificationException;
// import org.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
// @ExtendWith(MockitoExtension.class)
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_size_Test {

    private CompactMap<String, Integer> map;

    /**
     * A concrete subclass that forces a small {@code compactSize()} value so that we
     * can easily trigger the transition from the internal {@code Object[]} to a
     * delegated {@code Map}.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        /**
         * transition after 3 entries
         */
        @Override
        protected int compactSize() {
            return 3;
        }

        /**
         * use a regular HashMap for the delegated map implementation
         */
//         @Override
//         protected Map<K, V> getNewMap() {
//             return new HashMap<>();
//         }
    }

    //     @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test.
        map = new TestCompactMap<>();
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the map to help GC and avoid cross‑test contamination.
        map.clear();
    }

    /**
     * Uses reflection to replace the private {@code val} field of a {@link CompactMap}
     * instance. This is required for tests that need to force a particular internal
     * representation that is not reachable through the public API.
     */
    private static void setInternalVal(CompactMap<?, ?> target, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(target, newVal);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set internal val field via reflection", e);
        }
    }

    @Test
    void size_EmptyMap_ReturnsZero() {
        int size = map.size();
        assertEquals(0, size, "size() should be 0 for an empty CompactMap");
    }

    @Test
    void size_SingleEntry_ReturnsOne() {
        map.put("one", 1);
        assertEquals(1, map.size(), "size() should be 1 after adding a single entry");
    }

    @Test
    void size_TwoEntriesWithinCompactSize_ReturnsTwo() {
        map.put("a", 10);
        map.put("b", 20);
        assertEquals(2, map.size(), "size() should be 2 when two entries are stored in the internal array");
    }

    @Test
    void size_ExceedsCompactSize_TransitionsToMap_ReturnsCorrectSize() {
        map.put("k1", 1);
        map.put("k2", 2);
        map.put("k3", 3);
        // triggers transition
        map.put("k4", 4);
        assertEquals(4, map.size(), "size() should reflect the total number of entries after transition to a Map");
    }

    @Test
    void size_AfterClear_ReturnsZero() {
        map.put("x", 100);
        map.put("y", 200);
        map.clear();
        assertEquals(0, map.size(), "size() should be 0 after clear()");
    }

    @Test
    void size_ForceObjectArray_ThreeEntries() throws Exception {
        Object[] arr = new Object[] { "k1", 1, "k2", 2, "k3", 3 };
        setInternalVal(map, arr);
        assertEquals(3, map.size(), "size() must compute length>>1 (6>>1 = 3) for an Object[] of length 6");
    }

    @Test
    void size_ForceObjectArray_EmptyArray() throws Exception {
        Object[] empty = new Object[0];
        setInternalVal(map, empty);
        assertEquals(0, map.size(), "size() must return 0 for an empty Object[] (0>>1 = 0)");
    }

//     @Test
//     void size_ForceDelegatedMap_TwoEntries() throws Exception {
//         Map<String, Integer> delegate = new HashMap<>();
//         delegate.put("a", 1);
//         delegate.put("b", 2);
//         setInternalVal(map, delegate);
//         assertEquals(2, map.size(), "size() must delegate to the internal Map's size()");
//     }

    @Test
    void size_AfterRemovingSoleEntry_ReturnsZero() {
        map.put("only", 42);
        assertEquals(1, map.size(), "pre‑condition: map should contain one entry");
        map.remove("only");
        assertEquals(0, map.size(), "size() must be 0 after removing the sole entry");
    }

    @Test
    void size_AfterRemovingOneOfTwoEntries_ReturnsOne() {
        map.put("first", 1);
        map.put("second", 2);
        assertEquals(2, map.size(), "pre‑condition: map should contain two entries");
        map.remove("first");
        assertEquals(1, map.size(), "size() must be 1 after removing one of two entries");
    }

    @Test
    void size_DuplicateKeyUpdate_StaysOne() {
        map.put("dup", 1);
        map.put("dup", 2);
        assertEquals(1, map.size(), "size() must remain 1 when the same key is updated");
    }

    @Test
    void size_NullKey_Allowed() {
        map.put(null, 123);
        assertEquals(1, map.size(), "CompactMap must accept null keys and report size 1");
    }

    @Test
    void size_NullValue_Allowed() {
        map.put("keyWithNull", null);
        assertEquals(1, map.size(), "CompactMap must accept null values and report size 1");
    }

    @Test
    void size_ManyEntries_ViaPut_TriggersMapTransition() {
        for (int i = 0; i < 10; i++) {
            map.put("k" + i, i);
        }
        assertEquals(10, map.size(), "After inserting 10 entries size() must return 10");
    }

//     @Test
//     void size_AfterPutAll_FourEntries() {
//         Map<String, Integer> source = new HashMap<>();
//         source.put("a", 1);
//         source.put("b", 2);
//         source.put("c", 3);
//         source.put("d", 4);
//         map.putAll(source);
//         assertEquals(4, map.size(), "size() must reflect the number of entries added via putAll()");
//     }

//     @Test
//     void size_AfterMinusOperation_TwoEntries() {
//         map.put("one", 1);
//         map.put("two", 2);
//         map.put("three", 3);
//         Map<String, Integer> result = map.minus("two");
//         assertEquals(2, result.size(), "Resulting map after minus should contain 2 entries");
//     }

//     @Test
//     void size_AfterPlusOperation_UniqueKeysFive() {
//         CompactMap<String, Integer> left = new TestCompactMap<>();
//         left.put("a", 1);
//         left.put("b", 2);
//         left.put("c", 3);
//         CompactMap<String, Integer> right = new TestCompactMap<>();
//         right.put("b", 20);
//         right.put("c", 30);
//         right.put("d", 40);
//         Map<String, Integer> merged = left.plus(right);
//         assertEquals(5, merged.size(), "Merged map should contain 5 unique keys");
//     }

//     @Test
//     void size_DelegatedMapThrows_ConcurrentModificationException() throws Exception {
//         @SuppressWarnings("unchecked")
//         Map<String, Integer> badMap = mock(Map.class);
//         when(badMap.size()).thenThrow(new ConcurrentModificationException("CM"));
//         setInternalVal(map, badMap);
//         ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, map::size);
//         assertEquals("CM", ex.getMessage());
//     }

//     @Test
//     void size_DelegatedMapThrows_IllegalStateException() throws Exception {
//         @SuppressWarnings("unchecked")
//         Map<String, Integer> badMap = mock(Map.class);
//         when(badMap.size()).thenThrow(new IllegalStateException("illegal"));
//         setInternalVal(map, badMap);
//         IllegalStateException ex = assertThrows(IllegalStateException.class, map::size);
//         assertEquals("illegal", ex.getMessage());
//     }

//     @Test
//     void size_DelegatedMapThrows_RuntimeException() throws Exception {
//         @SuppressWarnings("unchecked")
//         Map<String, Integer> badMap = mock(Map.class);
//         RuntimeException runtime = new RuntimeException("boom");
//         when(badMap.size()).thenThrow(runtime);
//         setInternalVal(map, badMap);
//         RuntimeException ex = assertThrows(RuntimeException.class, map::size);
//         assertSame(runtime, ex);
//     }

    @ParameterizedTest(name = "{index} => arrayLength={0}, expectedSize={1}")
    @ValueSource(ints = { 0, 6 })
    void size_ObjectArray_VariousLengths(int arrayLength, TestInfo testInfo) throws Exception {
        // Build an Object[] of the requested length.
        // Even indices are keys (String), odd indices are values (Integer).
        Object[] arr = new Object[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            arr[i] = (i % 2 == 0) ? "k" + i : i;
        }
        setInternalVal(map, arr);
        int expected = arrayLength >> 1;
        assertEquals(expected, map.size(), "size() should compute arrayLength>>1 = " + expected + " for Object[] length " + arrayLength);
    }

    @Test
    public void size_ObjectArray_LengthZero() throws Exception {
        Object[] arr = new Object[0];
        setInternalVal(map, arr);
        assertEquals(0, map.size(), "size() should compute arrayLength>>1 = 0 for Object[] length 0");
    }

    @Test
    public void size_ObjectArray_LengthSix() throws Exception {
        Object[] arr = new Object[6];
        for (int i = 0; i < 6; i++) {
            arr[i] = (i % 2 == 0) ? "k" + i : i;
        }
        setInternalVal(map, arr);
        assertEquals(3, map.size(), "size() should compute arrayLength>>1 = 3 for Object[] length 6");
    }
}