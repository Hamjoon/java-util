package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_remove_Test {

    private NavigableSet<String> set;

    class CustomKey {

        final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomKey{" + id + '}';
        }
    }

    class CustomKey {

        final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomKey{" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a known state
        set = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
        // Verify initial size and contents
        assertEquals(4, set.size());
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains("cherry"));
        assertTrue(set.contains(null));
        set = new ConcurrentNavigableSetNullSafe<>();
        // Populate with a deterministic state used by most tests
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
        assertEquals(4, set.size());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to ensure no side‑effects between tests
        set.clear();
        set.clear();
    }

    /**
     * -------------------------------------------------------------------- *
     */
    private static Stream<Arguments> basicRemovalProvider() {
        return Stream.of(// remove existing element
        Arguments.of("apple", true, "Remove existing element 'apple' should return true"), // remove non‑existing element
        Arguments.of("absent", false, "Remove non‑existing element should return false"), // remove null when it is present
        Arguments.of(null, true, "Remove null element when null is present should return true"), // remove element of incompatible type – should throw ClassCastException
        Arguments.of(123, ClassCastException.class, "Removing element of incompatible type should throw ClassCastException"));
    }

    /**
     * -------------------------------------------------------------------- *
     */
    private static void setInternalSet(ConcurrentNavigableSetNullSafe<?> target, NavigableSet<Object> newSet) throws Exception {
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        // Remove final modifier if present (works on most JVMs for testing purposes)
        java.lang.reflect.Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        field.set(target, newSet);
    }

    @Test
    void removeExistingElement_returnsTrue_andElementIsGone() {
        // given
        assertTrue(set.contains("banana"));
        // when
        boolean removed = set.remove("banana");
        // then
        assertTrue(removed, "remove should return true for an existing element");
        assertFalse(set.contains("banana"), "set should no longer contain the removed element");
        assertEquals(3, set.size(), "size should be decremented");
    }

    @Test
    void removeNullElement_returnsTrue_andNullIsGone() {
        // given
        assertTrue(set.contains(null));
        // when
        boolean removed = set.remove(null);
        // then
        assertTrue(removed, "remove should return true when null is present");
        assertFalse(set.contains(null), "null should no longer be present after removal");
        assertEquals(3, set.size(), "size should be decremented");
    }

    @Test
    void removeNonExistingElement_returnsFalse_andSetUnchanged() {
        // given
        assertFalse(set.contains("durian"));
        int originalSize = set.size();
        // when
        boolean removed = set.remove("durian");
        // then
        assertFalse(removed, "remove should return false when element is absent");
        assertEquals(originalSize, set.size(), "size must remain unchanged");
        // verify other elements are still present
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains("cherry"));
        assertTrue(set.contains(null));
    }

    @Test
    void removeFromEmptySet_returnsFalse() {
        // given
        set.clear();
        assertTrue(set.isEmpty());
        // when
        boolean removed = set.remove("anything");
        // then
        assertFalse(removed, "remove on an empty set must return false");
    }

    @Test
    void removeMultipleElements_iterateOrderReflectsRemoval() {
        // given
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains("cherry"));
        assertTrue(set.contains(null));
        // when
        set.remove("apple");
        set.remove(null);
        // then
        assertFalse(set.contains("apple"));
        assertFalse(set.contains(null));
        assertEquals(2, set.size());
        // iterator should reflect the remaining ordering (natural order, null is masked)
        Iterator<String> it = set.iterator();
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertFalse(it.hasNext());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicRemovalProvider")
    void remove_basicScenarios(Object input, Object expected, String description) {
        // Arrange: for the case where we expect a false result for null, make null absent
        if (input == null && Boolean.FALSE.equals(expected)) {
            set.remove(null);
        }
        // Act & Assert
        if (expected instanceof Boolean) {
            boolean result = set.remove(input);
            assertEquals(expected, result, () -> "Unexpected result when removing " + input);
        } else if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> set.remove(input), () -> "Expected exception when removing " + input);
        }
    }

    /**
     * -------------------------------------------------------------------- *
     */
    @Test
    void remove_fromEmptySet_returnsFalse() {
        set.clear();
        assertTrue(set.isEmpty());
        boolean removed = set.remove("anything");
        assertFalse(removed, "remove on an empty set must return false");
    }

    /**
     * -------------------------------------------------------------------- *
     */
    @Test
    void remove_duplicateCall_behaviour() {
        assertTrue(set.contains("banana"));
        assertTrue(set.remove("banana"));
        assertFalse(set.remove("banana"), "Second removal of the same element must return false");
    }

    /**
     * -------------------------------------------------------------------- *
     */
    @Test
    void remove_equalButDifferentInstance_returnsTrue() {
        String original = "test";
        // distinct object but equal
        String differentInstance = new String("test");
        assertTrue(set.add(original));
        assertTrue(set.contains(original));
        assertTrue(set.remove(differentInstance), "Removal should succeed for equal but distinct instance");
        assertFalse(set.contains(original));
    }

    /**
     * -------------------------------------------------------------------- *
     */
    @Test
    void remove_caseInsensitiveComparator_successful() throws Exception {
        // Build a NavigableSet with a case‑insensitive comparator
        NavigableSet<Object> ciSet = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
        // Use reflection to inject it into a new instance of the class under test
        ConcurrentNavigableSetNullSafe<String> ciWrapper = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(ciWrapper, ciSet);
        // Populate via the wrapper – it will mask null internally
        assertTrue(ciWrapper.add("Hello"));
        assertTrue(ciWrapper.contains("Hello"));
        // due to comparator
        assertTrue(ciWrapper.contains("hello"));
        // Remove using a different case
        assertTrue(ciWrapper.remove("hello"), "Removal should succeed with case‑insensitive comparator");
        assertFalse(ciWrapper.contains("Hello"));
        assertEquals(0, ciWrapper.size());
    }

    /**
     * -------------------------------------------------------------------- *
     */
    @Test
    void remove_customKeyEquality_successful() throws Exception {
        class CustomKey {

            final int id;

            CustomKey(int id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof CustomKey) && ((CustomKey) o).id == id;
            }

            @Override
            public int hashCode() {
                return Objects.hash(id);
            }

            @Override
            public String toString() {
                return "CustomKey{" + id + '}';
            }
        }
        // Use a raw ConcurrentNavigableSetNullSafe to hold CustomKey objects
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe<CustomKey> customSet = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(customSet, new ConcurrentSkipListSet<>());
        CustomKey key1 = new CustomKey(42);
        // equal but distinct instance
        CustomKey key2 = new CustomKey(42);
        assertTrue(customSet.add(key1));
        assertTrue(customSet.contains(key1));
        assertTrue(customSet.remove(key2), "Removal should succeed for an equal CustomKey instance");
        assertFalse(customSet.contains(key1));
    }

    /**
     * -------------------------------------------------------------------- *
     */
    @Test
    void remove_unmodifiableUnderlyingSet_throwsUnsupportedOperationException() throws Exception {
        NavigableSet<Object> modifiable = new ConcurrentSkipListSet<>();
        NavigableSet<Object> unmodifiable = Collections.unmodifiableNavigableSet(modifiable);
        setInternalSet(set, unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> set.remove("anything"), "Calling remove on an unmodifiable underlying set must throw UOE");
    }

    /**
     * -------------------------------------------------------------------- *
     */
    @Test
    void remove_nullInternalSet_throwsNullPointerException() throws Exception {
        // Force the private final field to null via reflection
        setInternalSet(set, null);
        assertThrows(NullPointerException.class, () -> set.remove("anything"), "Calling remove when internalSet is null must throw NPE");
    }
}
