package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_remove_Test {

    private CaseInsensitiveSet<String> stringSet;

    private CaseInsensitiveSet<Integer> intSet;

    @BeforeEach
    void setupBeforeEach() {
        // Default set – the constructor creates a case‑insensitive map for Strings.
        stringSet = new CaseInsensitiveSet<>();
        stringSet.add("Apple");
        stringSet.add("Banana");
        stringSet.add("Cherry");
        intSet = new CaseInsensitiveSet<>();
        intSet.add(1);
        intSet.add(2);
        intSet.add(3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the sets; not strictly necessary because a new instance is created per test,
        // but it demonstrates good cleanup practice.
        stringSet.clear();
        intSet.clear();
    }

    /**
     * Uses reflection to replace the private final {@code map} field of a {@link CaseInsensitiveSet}
     * with a custom implementation supplied by the test.
     *
     * @param set the {@code CaseInsensitiveSet} instance to modify
     * @param map the custom {@code Map} to inject
     */
    private static void injectMap(CaseInsensitiveSet<?> set, Map<?, ?> map) throws Exception {
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        // The field is final; we have to remove the final modifier via reflection.
        // In Java 12+ this works without additional hacks because setAccessible(true) allows us to write.
        mapField.set(set, map);
    }

    private static Stream<Arguments> provideRemoveExistingElementScenarios() {
        return Stream.of(Arguments.of("Apple", true), Arguments.of("Banana", true), Arguments.of("Cherry", true));
    }

    @Test
    void removeExistingElementReturnsTrueAndRemovesIt() {
        // given
        int originalSize = stringSet.size();
        assertTrue(stringSet.contains("Banana"));
        // when
        boolean removed = stringSet.remove("Banana");
        // then
        assertTrue(removed, "remove should return true for an element that existed");
        assertFalse(stringSet.contains("Banana"), "element should no longer be present after removal");
        assertEquals(originalSize - 1, stringSet.size(), "size should decrease by one");
    }

    @Test
    void removeNonExistingElementReturnsFalseAndLeavesSetUnchanged() {
        // given
        int originalSize = stringSet.size();
        assertFalse(stringSet.contains("Durian"));
        // when
        boolean removed = stringSet.remove("Durian");
        // then
        assertFalse(removed, "remove should return false for an element that did not exist");
        assertEquals(originalSize, stringSet.size(), "size should remain unchanged");
    }

    @Test
    void removeStringIsCaseInsensitive() {
        // given
        assertTrue(stringSet.contains("Apple"));
        // case‑insensitive view
        assertTrue(stringSet.contains("apple"));
        // when
        // different case
        boolean removed = stringSet.remove("aPPle");
        // then
        assertTrue(removed, "remove should succeed regardless of case");
        assertFalse(stringSet.contains("Apple"));
        assertFalse(stringSet.contains("apple"));
        assertEquals(2, stringSet.size(), "size should reflect the removal");
    }

    @Test
    void removeNonStringElementIsCaseSensitive() {
        // given
        assertTrue(intSet.contains(2));
        // when
        boolean removed = intSet.remove(2);
        // then
        assertTrue(removed);
        assertFalse(intSet.contains(2));
        assertEquals(2, intSet.size());
    }

    @Test
    void removeNullElementBehavesAccordingToMapImplementation() {
        // given
        CaseInsensitiveSet<String> setWithNull = new CaseInsensitiveSet<>();
        setWithNull.add(null);
        assertTrue(setWithNull.contains(null));
        // when
        boolean removed = setWithNull.remove(null);
        // then
        assertTrue(removed, "remove should return true when null was present");
        assertFalse(setWithNull.contains(null), "null should no longer be present");
        assertEquals(0, setWithNull.size());
    }

    @Test
    void removeFromEmptySetReturnsFalse() {
        // given
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        assertTrue(empty.isEmpty());
        // when
        boolean removed = empty.remove("anything");
        // then
        assertFalse(removed, "remove should return false when the set is empty");
        assertTrue(empty.isEmpty(), "set should remain empty");
    }

    @Test
    void removeAllElementsOneByOne() {
        // given
        // initial three elements
        Collection<String> elements = stringSet;
        // when & then
        for (String e : elements) {
            boolean removed = stringSet.remove(e);
            assertTrue(removed, "each element should be removable");
        }
        assertTrue(stringSet.isEmpty(), "set should be empty after removing all elements");
    }

    @Test
    void removeNullWhenNullIsNotPresentReturnsFalse() {
        // Default set does not contain null.
        assertFalse(stringSet.contains(null));
        boolean removed = stringSet.remove(null);
        assertFalse(removed, "remove(null) should return false when null is absent");
        assertEquals(3, stringSet.size());
    }

    @Test
    void removeElementWhoseValueInMapIsNullReturnsTrue() throws Exception {
        // Create a set backed by a plain HashMap so we can store a null value.
        CaseInsensitiveSet<String> customSet = new CaseInsensitiveSet<>();
        injectMap(customSet, new HashMap<>());
        // Directly put a key with a null value (the Set API does not expose values).
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) mapField.get(customSet);
        map.put("NullValue", null);
        assertTrue(customSet.contains("NullValue"));
        boolean removed = customSet.remove("NullValue");
        assertTrue(removed, "remove should return true even if the stored value is null");
        assertFalse(customSet.contains("NullValue"));
        assertEquals(0, customSet.size());
    }

    @Test
    void removeFromSetBackedByUnmodifiableMapThrowsUnsupportedOperationException() throws Exception {
        Map<String, Object> base = new HashMap<>();
        base.put("Immutable", new Object());
        Map<String, Object> unmodifiable = Collections.unmodifiableMap(base);
        CaseInsensitiveSet<String> immutableSet = new CaseInsensitiveSet<>();
        injectMap(immutableSet, unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> immutableSet.remove("Immutable"), "remove should propagate UnsupportedOperationException from the underlying map");
    }

    @Test
    void removeExistingElementWithCaseInsensitiveTreeMapComparator() throws Exception {
        TreeMap<String, Object> caseInsensitiveTreeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveTreeMap.put("Orange", new Object());
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        injectMap(set, caseInsensitiveTreeMap);
        assertTrue(set.contains("Orange"));
        boolean removed = set.remove("orange");
        assertTrue(removed, "case‑insensitive TreeMap should match different case");
        assertFalse(set.contains("Orange"));
        assertEquals(0, set.size());
    }

    @Test
    void removeExistingElementWithDifferentCase_CaseSensitiveMapReturnsFalse() throws Exception {
        // Natural ordering – case‑sensitive.
        ConcurrentSkipListMap<String, Object> caseSensitiveMap = new ConcurrentSkipListMap<>();
        caseSensitiveMap.put("Apple", new Object());
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        injectMap(set, caseSensitiveMap);
        assertTrue(set.contains("Apple"));
        boolean removed = set.remove("apple");
        assertFalse(removed, "case‑sensitive map should not match different case");
        assertTrue(set.contains("Apple"));
        assertEquals(1, set.size());
    }

    @Test
    void removeExistingElementWithDifferentCase_CaseInsensitiveMapReturnsTrue() throws Exception {
        // Comparator that ignores case.
        ConcurrentSkipListMap<String, Object> caseInsensitiveMap = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveMap.put("Apple", new Object());
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        injectMap(set, caseInsensitiveMap);
        assertTrue(set.contains("Apple"));
        boolean removed = set.remove("aPPle");
        assertTrue(removed, "case‑insensitive map should match different case");
        assertFalse(set.contains("Apple"));
        assertEquals(0, set.size());
    }

    @ParameterizedTest(name = "remove existing element \"{0}\" should return {1}")
    @MethodSource("provideRemoveExistingElementScenarios")
    void removeExistingElementParameterized(String element, boolean expected) {
        assertTrue(stringSet.contains(element));
        boolean removed = stringSet.remove(element);
        assertEquals(expected, removed);
        assertFalse(stringSet.contains(element));
    }
}
