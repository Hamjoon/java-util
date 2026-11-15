package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_remove_Test {

    /**
     * The spy‑wrapped backing map used by the test instance.
     */
    private ConcurrentMap<Object, Object> backingMapSpy;

    /**
     * The map under test.
     */
    private AbstractConcurrentNullSafeMap<String, String> map;

    /* --------------------------------------------------------------------- *
     *  Helper concrete implementation used for testing the abstract class. *
     * --------------------------------------------------------------------- */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    /**
     * A concrete subclass that deliberately passes {@code null} as the backing map.
     */
    private static class NullBackingMap extends AbstractConcurrentNullSafeMap<String, String> {

        NullBackingMap() {
            super(null);
        }
    }

    /**
     * Simple POJO used for the “complex value handling” test case.
     */
    private static final class Dummy {

        final int id;

        Dummy(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Dummy) && ((Dummy) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "Dummy{id=" + id + '}';
        }
    }

    /* --------------------------------------------------------------------- *
     *  Helper holder for parameterised test arguments.                     *
     * --------------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final String key;

        final String expected;

        final String description;

        final boolean prepopulate;

        ArgumentsHolder(String key, String expected, String description) {
            this.key = key;
            this.expected = expected;
            this.description = description;
            // For the "missingKey" case we do NOT prepopulate.
            this.prepopulate = !"missingKey".equals(key);
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap but spy on it so we can verify interactions.
        backingMapSpy = spy(new ConcurrentHashMap<>());
        // The concrete subclass works with any key/value types; we use String for simplicity.
        map = new TestMap<>(backingMapSpy);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests for the basic functionality group.               *
     * --------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> basicRemovalProvider() {
        return Stream.of(new ArgumentsHolder("existingKey", "existingValue", "Remove an entry that exists with a non‑null key and non‑null value."), new ArgumentsHolder(null, "nullValue", "Remove an entry whose key is null (handled via maskNullKey)."), new ArgumentsHolder("keyWithNullValue", null, "Remove an entry that exists with a non‑null key but the associated value is null."), new ArgumentsHolder("missingKey", null, "Attempt to remove a key that is not present in the map."));
    }

    @Test
    void remove_existingNonNullKey_returnsValueAndRemovesEntry() {
        // given
        map.put("alpha", "first");
        map.put("beta", "second");
        // when
        String removed = map.remove("alpha");
        // then
        assertEquals("first", removed, "remove should return the previously associated value");
        assertFalse(map.containsKey("alpha"), "key should no longer be present after removal");
        assertTrue(map.containsKey("beta"), "other keys must stay untouched");
        // Verify that the underlying map received the masked key.
        verify(backingMapSpy).remove("alpha");
    }

    @Test
    void remove_nonExistingKey_returnsNull() {
        // given – map is empty
        // when
        String removed = map.remove("nonexistent");
        // then
        assertNull(removed, "remove of a missing key must return null");
        verify(backingMapSpy).remove("nonexistent");
    }

    @Test
    void remove_nullKey_whenPresent_returnsValueAndRemovesEntry() {
        // given
        map.put(null, "nullKeyValue");
        // when
        String removed = map.remove(null);
        // then
        assertEquals("nullKeyValue", removed, "remove should correctly handle a null key");
        assertFalse(map.containsKey(null), "null key must be removed");
        // The internal map receives the sentinel object that masks a null key.
        // we cannot know the sentinel value, just ensure remove was called
        verify(backingMapSpy).remove(any());
    }

    @Test
    void remove_keyWithNullValue_returnsNullAndRemovesEntry() {
        // given
        map.put("keyWithNull", null);
        // when
        String removed = map.remove("keyWithNull");
        // then
        assertNull(removed, "remove should return null for a key whose value is null");
        assertFalse(map.containsKey("keyWithNull"), "entry must be removed even when value is null");
        verify(backingMapSpy).remove("keyWithNull");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicRemovalProvider")
    void remove_basicFunctionality(ArgumentsHolder args) {
        // -----------------------------------------------------------------
        // Arrange – pre‑populate the map according to the test case.
        // -----------------------------------------------------------------
        if (args.key != null || args.prepopulate) {
            // Only put when we want the entry to exist.
            if (args.expected != null || args.key != null) {
                map.put(args.key, args.expected);
            }
        }
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        String result = map.remove(args.key);
        // -----------------------------------------------------------------
        // Assert – verify returned value and that the entry is gone.
        // -----------------------------------------------------------------
        assertEquals(args.expected, result, () -> "remove should return " + args.expected + " for key " + args.key);
        assertFalse(map.containsKey(args.key), () -> "After removal the map must not contain key " + args.key);
        // Verify that the underlying map received a remove call with the masked key.
        // For null keys we cannot know the sentinel, so we just verify a call happened.
        if (args.key == null) {
            verify(backingMapSpy).remove(any());
        } else {
            verify(backingMapSpy).remove(args.key);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Equality handling – different instance but equal key.                *
     * --------------------------------------------------------------------- */
    @Test
    void remove_equalKeyDifferentInstance() {
        // given
        String originalKey = "existingKey";
        // equal but not same instance
        String equalKey = new String("existingKey");
        map.put(originalKey, "existingValue");
        // when
        String removed = map.remove(equalKey);
        // then
        assertEquals("existingValue", removed);
        assertFalse(map.containsKey(originalKey));
        verify(backingMapSpy).remove(originalKey);
    }

    /* --------------------------------------------------------------------- *
     *  Size and state verification after removal.                           *
     * --------------------------------------------------------------------- */
    @Test
    void remove_existingKey_decreasesSizeAndMakesGetReturnNull() {
        // given
        map.put("existingKey", "existingValue");
        map.put("otherKey", "otherValue");
        int sizeBefore = map.size();
        // when
        String removed = map.remove("existingKey");
        // then
        assertEquals("existingValue", removed);
        assertEquals(sizeBefore - 1, map.size(), "Map size must decrease by one after successful removal");
        assertNull(map.get("existingKey"), "Subsequent get for the removed key must return null");
    }

    @Test
    void remove_nullKey_decreasesSizeAndMakesGetReturnNull() {
        // given
        map.put(null, "nullValue");
        map.put("other", "value");
        int sizeBefore = map.size();
        // when
        String removed = map.remove(null);
        // then
        assertEquals("nullValue", removed);
        assertEquals(sizeBefore - 1, map.size(), "Map size must decrease by one after removing a null key");
        assertNull(map.get(null), "Subsequent get(null) must return null after removal");
        // sentinel argument
        verify(backingMapSpy).remove(any());
    }

    /* --------------------------------------------------------------------- *
     *  Null key with null value.                                            *
     * --------------------------------------------------------------------- */
    @Test
    void remove_nullKeyWithNullValue_returnsNull() {
        // given
        map.put(null, null);
        // when
        String removed = map.remove(null);
        // then
        assertNull(removed);
        assertFalse(map.containsKey(null));
        verify(backingMapSpy).remove(any());
    }

    /* --------------------------------------------------------------------- *
     *  Complex value handling – custom object as value.                     *
     * --------------------------------------------------------------------- */
    @Test
    void remove_keyWithComplexObjectValue_returnsExactInstance() {
        // Use a map that holds Dummy objects instead of Strings.
        AbstractConcurrentNullSafeMap<String, Dummy> dummyMap = new TestMap<>(new ConcurrentHashMap<>());
        Dummy dummy = new Dummy(42);
        dummyMap.put("objectKey", dummy);
        // when
        Dummy removed = dummyMap.remove("objectKey");
        // then
        assertSame(dummy, removed, "The exact Dummy instance must be returned");
        assertFalse(dummyMap.containsKey("objectKey"));
    }

    /* --------------------------------------------------------------------- *
     *  ClassCastException when key type is incompatible.                   *
     * --------------------------------------------------------------------- */
    @Test
    void remove_incompatibleKeyType_throwsClassCastException() {
        // The map is typed with String keys; passing an Integer should fail.
        assertThrows(ClassCastException.class, () -> map.remove(12345), "Removing with a key of the wrong type must throw ClassCastException");
    }

    /* --------------------------------------------------------------------- *
     *  Value type mismatch – internal map contains wrong value type.      *
     * --------------------------------------------------------------------- */
    @Test
    void remove_valueTypeMismatch_throwsClassCastException() throws Exception {
        // Directly tamper with the backing map to store an Integer value.
        // We need the masked key for "validKey".
        String key = "validKey";
        // Use reflection to invoke the protected maskNullKey method.
        java.lang.reflect.Method maskMethod = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullKey", Object.class);
        maskMethod.setAccessible(true);
        Object maskedKey = maskMethod.invoke(map, key);
        // Put an Integer directly into the internal map.
        backingMapSpy.put(maskedKey, 999);
        // Now invoke remove – unmaskNullValue will try to cast Integer to String.
        assertThrows(ClassCastException.class, () -> map.remove(key), "When the underlying map holds a value of the wrong type, remove must throw ClassCastException");
    }

    /* --------------------------------------------------------------------- *
     *  Null internal map – should result in NullPointerException.         *
     * --------------------------------------------------------------------- */
    @Test
    void remove_withNullInternalMap_throwsNullPointerException() {
        AbstractConcurrentNullSafeMap<String, String> nullMap = new NullBackingMap();
        assertThrows(NullPointerException.class, () -> nullMap.remove("anyKey"), "Calling remove when internalMap is null must throw NullPointerException");
    }
}
