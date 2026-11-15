package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_isEmpty_Test {

    /**
     * Set used by the majority of tests – created with the default constructor.
     */
    private CaseInsensitiveSet<String> defaultSet;

    /**
     * Set created from an empty collection but with a custom {@link CaseInsensitiveMap} backed by a {@link HashMap}.
     */
    private Set<String> hashMapBackedSet;

    /**
     * Set created from an empty collection but with a custom {@link CaseInsensitiveMap} backed by a {@link LinkedHashMap}.
     */
    private Set<String> linkedHashMapBackedSet;

    @BeforeEach
    void setupBeforeEach() {
        // Default instance – empty at the start of each test.
        defaultSet = new CaseInsensitiveSet<>();
        // Custom‑backed instances – also start empty.
        hashMapBackedSet = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>()));
        linkedHashMapBackedSet = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()));
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary but keeps the pattern consistent.
        defaultSet = null;
        hashMapBackedSet = null;
        linkedHashMapBackedSet = null;
    }

    @Test
    void isEmpty_onNewDefaultSet_returnsTrue() {
        assertTrue(defaultSet.isEmpty(), "A newly created set should be empty");
    }

    @Test
    void isEmpty_afterAddingElement_returnsFalse() {
        defaultSet.add("Hello");
        assertFalse(defaultSet.isEmpty(), "Set should not be empty after adding an element");
    }

    @Test
    void isEmpty_afterAddingAndRemovingSameElement_returnsTrue() {
        defaultSet.add("World");
        // different case, should match
        defaultSet.remove("world");
        assertTrue(defaultSet.isEmpty(), "Set should be empty after removing the only element");
    }

    @Test
    void isEmpty_whenConstructedFromNonEmptyCollection_returnsFalse() {
        Set<String> source = new CaseInsensitiveSet<>();
        source.add("One");
        source.add("Two");
        CaseInsensitiveSet<String> setFromCollection = new CaseInsensitiveSet<>(source);
        assertFalse(setFromCollection.isEmpty(), "Set built from a non‑empty collection should not be empty");
    }

    @Test
    void isEmpty_whenConstructedFromEmptyCollection_returnsTrue() {
        CaseInsensitiveSet<String> setFromEmpty = new CaseInsensitiveSet<>(Collections.emptySet());
        assertTrue(setFromEmpty.isEmpty(), "Set built from an empty collection should be empty");
    }

    @Test
    void isEmpty_customHashMapBackedSet_behavesLikeDefault() {
        assertTrue(hashMapBackedSet.isEmpty(), "Custom‑backed set should start empty");
        hashMapBackedSet.add("Apple");
        assertFalse(hashMapBackedSet.isEmpty(), "Custom‑backed set should report non‑empty after add");
        hashMapBackedSet.remove("APPLE");
        assertTrue(hashMapBackedSet.isEmpty(), "Custom‑backed set should be empty after removing the only element");
    }

    @Test
    void isEmpty_customLinkedHashMapBackedSet_preservesOrderButIsEmptyLogicUnchanged() {
        assertTrue(linkedHashMapBackedSet.isEmpty(), "LinkedHashMap‑backed set should start empty");
        linkedHashMapBackedSet.add("Banana");
        linkedHashMapBackedSet.add("Cherry");
        assertFalse(linkedHashMapBackedSet.isEmpty(), "Set should not be empty after adding elements");
        linkedHashMapBackedSet.removeAll(Set.of("BANANA", "CHERRY"));
        assertTrue(linkedHashMapBackedSet.isEmpty(), "Set should be empty after removing all elements");
    }

    @Test
    void isEmpty_afterClear_returnsTrue() {
        defaultSet.add("first");
        defaultSet.add("second");
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty(), "Set should be empty after clear() is invoked");
    }

    @Test
    void isEmpty_addingDuplicateElement_stillNonEmpty() {
        defaultSet.add("A");
        // Adding a duplicate (case‑insensitive) should not change emptiness
        defaultSet.add("a");
        assertFalse(defaultSet.isEmpty(), "Adding a duplicate element must leave the set non‑empty");
    }

    @Test
    void isEmpty_addThenRemoveSingleElement_returnsTrue() {
        defaultSet.add("X");
        // case‑insensitive removal
        defaultSet.remove("x");
        assertTrue(defaultSet.isEmpty(), "Removing the only element should make the set empty");
    }

    @Test
    void isEmpty_addMultipleThenRemoveAll_returnsTrue() {
        defaultSet.addAll(List.of("one", "two", "three"));
        defaultSet.removeAll(List.of("ONE", "Two", "THREE"));
        assertTrue(defaultSet.isEmpty(), "removeAll with all present elements must empty the set");
    }

    @Test
    void isEmpty_addMultipleThenRetainAllEmpty_returnsTrue() {
        defaultSet.addAll(List.of("a", "b"));
        // retainAll with an empty collection discards everything
        defaultSet.retainAll(Collections.emptyList());
        assertTrue(defaultSet.isEmpty(), "retainAll with an empty collection should empty the set");
    }

    @Test
    void isEmpty_addMultipleThenIterateAndRemoveAll_returnsTrue() {
        defaultSet.addAll(List.of("alpha", "beta"));
        Iterator<String> it = defaultSet.iterator();
        while (it.hasNext()) {
            it.next();
            // iterator‑based removal
            it.remove();
        }
        assertTrue(defaultSet.isEmpty(), "Removing each element via iterator.remove() must leave the set empty");
    }

    @Test
    void isEmpty_addAllEmptyCollection_keepsEmpty() {
        // addAll on a fresh set with an empty collection should not change emptiness
        defaultSet.addAll(Collections.emptyList());
        assertTrue(defaultSet.isEmpty(), "addAll([]) on a new set must keep it empty");
    }
}
