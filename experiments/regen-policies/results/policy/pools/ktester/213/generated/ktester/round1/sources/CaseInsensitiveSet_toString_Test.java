package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_toString_Test {

    /**
     * Used by several tests – a fresh set for each test method.
     */
    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor gives a case‑insensitive map with insertion order.
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help the GC; not strictly required.
        set.clear();
    }

    @Test
    void toStringEmptySet() {
        assertEquals("[]", set.toString(), "An empty CaseInsensitiveSet should be represented as []");
    }

    @Test
    void toStringPreservesOriginalCaseAndIgnoresDuplicates() {
        // Add strings that differ only by case – only the first case should appear.
        set.add("Hello");
        // duplicate, different case – should be ignored
        set.add("HELLO");
        set.add("World");
        // duplicate, different case – should be ignored
        set.add("world");
        // non‑String element, should be included as‑is
        set.add(42);
        // The expected order is the insertion order of the first occurrence of each element.
        String expected = "[Hello, World, 42]";
        assertEquals(expected, set.toString(), "toString() must preserve the original case of the first inserted element " + "and must not include case‑duplicate strings.");
    }

    @Test
    void toStringWithLinkedHashMapBackingPreservesInsertionOrder() {
        // Build a set that uses a LinkedHashMap as its backing map (preserves insertion order).
        List<String> source = Arrays.asList("b", "a", "c");
        Set<String> linked = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()));
        linked.addAll(source);
        // The order should be exactly the insertion order.
        assertEquals("[b, a, c]", linked.toString(), "When backed by a LinkedHashMap the order must follow insertion order.");
    }

    @Test
    void toStringWithTreeMapBackingIsSorted() {
        // Build a set that uses a TreeMap as its backing map (sorted order).
        List<String> source = Arrays.asList("b", "a", "c");
        Set<String> sorted = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new TreeMap<>()));
        sorted.addAll(source);
        // TreeMap sorts keys according to their natural ordering (case‑insensitive here).
        // The natural order of the original strings is a, b, c.
        assertEquals("[a, b, c]", sorted.toString(), "When backed by a TreeMap the order must be sorted (case‑insensitive).");
    }

    @Test
    void toStringReflectsContentsAfterRemoval() {
        set.add("One");
        set.add("Two");
        set.add("Three");
        set.add(99);
        assertEquals("[One, Two, Three, 99]", set.toString());
        // Remove an element (case‑insensitively) and verify the string representation updates.
        // different case
        set.remove("two");
        assertEquals("[One, Three, 99]", set.toString(), "After removal the toString() output must reflect the current contents.");
    }

    @Test
    void toStringSingleElement() {
        set.add("Apple");
        assertEquals("[Apple]", set.toString(), "A set containing a single element should be rendered with that element only.");
    }

    @Test
    void toStringSortedWhenBackedByTreeMap() {
        // Use a TreeMap backing to enforce natural (case‑insensitive) ordering.
        Set<String> sorted = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new TreeMap<>()));
        // Insert in a non‑sorted order.
        sorted.addAll(Arrays.asList("cherry", "Apple", "banana"));
        // Expected natural order ignoring case: Apple, banana, cherry
        assertEquals("[Apple, banana, cherry]", sorted.toString(), "Elements must appear sorted according to the TreeMap's natural ordering.");
    }

    @Test
    void toStringDuplicateIgnoredAfterInitialInsert() {
        set.add("Apple");
        set.add("banana");
        set.add("cherry");
        // Duplicate differing only by case.
        set.add("apple");
        assertEquals("[Apple, banana, cherry]", set.toString(), "Adding a case‑duplicate must not change the string representation.");
    }

    @Test
    void toStringAfterRemovalOfExistingElement() {
        set.addAll(Arrays.asList("Apple", "banana", "cherry"));
        // Remove using a different case.
        set.remove("APPLE");
        assertEquals("[banana, cherry]", set.toString(), "Removal should be case‑insensitive and reflected in the toString output.");
    }

    @Test
    void toStringAfterClear() {
        set.addAll(Arrays.asList("Apple", "banana", "cherry"));
        set.clear();
        assertEquals("[]", set.toString(), "After clear() the set must be represented as an empty list.");
    }

    @Test
    void toStringIdempotentCalls() {
        set.addAll(Arrays.asList("Apple", "banana"));
        String first = set.toString();
        String second = set.toString();
        assertEquals(first, second, "Consecutive calls to toString() must return identical results (no side effects).");
    }

    @Test
    void toStringHandlesEmptyStringAndSpace() {
        set.add("");
        set.add(" ");
        assertEquals("[,  ]", set.toString(), "Empty string and a single space should be represented distinctly.");
    }

    @Test
    void toStringThrowsNullPointerExceptionWhenMapIsNull() throws Exception {
        // Use reflection to corrupt the internal map reference.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        // simulate a broken instance
        mapField.set(set, null);
        assertThrows(NullPointerException.class, set::toString, "If the internal map is null, toString() should throw NPE.");
    }

    @Test
    void toStringPropagatesExceptionFromElementToString() {
        // An element whose toString() throws IllegalStateException.
        Object bad = new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        };
        set.add(bad);
        IllegalStateException ex = assertThrows(IllegalStateException.class, set::toString, "Exception from an element's toString() must propagate.");
        assertEquals("boom", ex.getMessage());
    }
}
