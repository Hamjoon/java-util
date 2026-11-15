package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListSet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_last_Test {

    private NavigableSet<String> set;

    private NavigableSet<String> comparatorSet;

    @BeforeEach
    void setupBeforeEach() {
        // Set used for natural ordering tests
        set = new ConcurrentNavigableSetNullSafe<>();
        // Set used for comparator‑based tests (order by string length)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        comparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination
        if (set != null) {
            set.clear();
        }
        if (comparatorSet != null) {
            comparatorSet.clear();
        }
    }

    @Test
    void lastReturnsHighestElementWithNaturalOrder() {
        // Add elements including a null; null is treated as the greatest element
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        // The highest element according to natural ordering (with null handling) is null
        assertNull(set.last(), "When null is present, last() should return null");
    }

    @Test
    void lastReturnsHighestElementWithoutNull() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        // Natural ordering: "cherry" > "banana" > "apple"
        assertEquals("cherry", set.last(), "last() should return the greatest element");
    }

    @Test
    void lastWithComparatorOrdersByLength() {
        // Comparator orders by length, null is considered length 0 (smallest)
        // length 4
        assertTrue(comparatorSet.add("kiwi"));
        // length 5
        assertTrue(comparatorSet.add("apple"));
        // length 6
        assertTrue(comparatorSet.add("banana"));
        // length 0 (smallest)
        assertTrue(comparatorSet.add(null));
        // The greatest element by length is "banana"
        assertEquals("banana", comparatorSet.last(), "With length comparator, last() should return the longest string");
    }

    @Test
    void lastOnEmptySetThrowsNoSuchElementException() {
        // The set is freshly created and empty
        assertTrue(set.isEmpty());
        // According to NavigableSet contract, last() must throw NoSuchElementException
        assertThrows(NoSuchElementException.class, () -> set.last(), "Calling last() on an empty set should throw NoSuchElementException");
    }

    @Test
    @DisplayName("last() returns the sole UUID element")
    void lastReturnsSingleUuidElement() {
        // Use a dedicated instance typed to UUID
        ConcurrentNavigableSetNullSafe<UUID> uuidSet = new ConcurrentNavigableSetNullSafe<>();
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertTrue(uuidSet.add(uuid));
        // The only element is also the greatest one
        assertEquals(uuid, uuidSet.last(), "When the set contains a single non‑null element, last() should return that element");
    }

    @Test
    @DisplayName("last() returns the greatest Integer among several values")
    void lastReturnsGreatestInteger() {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(intSet.add(1));
        assertTrue(intSet.add(42));
        assertTrue(intSet.add(99));
        assertEquals(Integer.valueOf(99), intSet.last(), "last() should return the highest integer according to natural ordering");
    }

    @Test
    @DisplayName("last() propagates NullPointerException when internalSet is null")
    void lastThrowsNullPointerExceptionWhenInternalSetIsNull(@Mock NavigableSet<Object> dummy) throws Exception {
        // Create an instance and then forcibly set its internalSet field to null via reflection
        ConcurrentNavigableSetNullSafe<String> broken = new ConcurrentNavigableSetNullSafe<>();
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        // break the internal reference
        internalSetField.set(broken, null);
        // The call should dereference a null field and therefore throw NPE
        assertThrows(NullPointerException.class, broken::last, "If internalSet is null, last() must throw NullPointerException");
    }

    @Test
    @DisplayName("last() throws ClassCastException when underlying element type mismatches generic type")
    void lastThrowsClassCastExceptionDueToTypeMismatch(@Mock NavigableSet<Object> mockSet) throws Exception {
        // Mock internalSet.last() to return a String, while the wrapper is typed as Integer
        when(mockSet.last()).thenReturn("I am a String");
        // Inject the mock into the wrapper
        ConcurrentNavigableSetNullSafe<Integer> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(wrapper, mockSet);
        // The unmaskNull method will attempt to cast the returned String to Integer → ClassCastException
        assertThrows(ClassCastException.class, wrapper::last, "When the underlying set contains an incompatible type, last() should throw ClassCastException");
    }

    @Test
    @DisplayName("last() returns null when internalSet.last() yields the internal null‑mask object")
    void lastReturnsNullWhenMaskObjectIsReturned() throws Exception {
        // Retrieve the private static null‑mask sentinel used by the implementation
        Field nullMaskField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("NULL");
        nullMaskField.setAccessible(true);
        // static field, hence null target
        Object nullMask = nullMaskField.get(null);
        // Mock internalSet to return the sentinel as the greatest element
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = mock(NavigableSet.class);
        when(mockSet.last()).thenReturn(nullMask);
        // Inject the mock into a wrapper instance
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(wrapper, mockSet);
        // The wrapper should translate the mask back to a genuine null
        assertNull(wrapper.last(), "When internalSet.last() returns the internal null‑mask, last() must unmask it to null");
    }
}
