package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_add_Test {

    /**
     * The mock map that will be injected into the {@link CompactSet} under test.
     */
    @Mock
    CompactMap<String, Object> mockMap;

    /**
     * The {@link CompactSet} instance that uses the mocked {@code CompactMap}.
     */
    CompactSet<String> setUnderTest;

    /**
     * Simple POJO used to test logical equality handling in the set.
     */
    static final class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person p = (Person) o;
            return age == p.age && name.equals(p.name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + age;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // which satisfies the validation, so we can safely use the mock.
        setUnderTest = new CompactSet<>(mockMap);
        // The protected constructor validates compactSize() >= 2.
        // The default CompactMap builder creates a map with compactSize() = 70,
        // because this test resides in the same package.
        setUnderTest = new CompactSet<>(mockMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(mockMap);
        reset(mockMap);
    }

    /**
     * Parameter source for the {@code add(E) method behavior} group.
     *
     * Each row contains:
     *   - element to add (String or null)
     *   - the value that {@code mockMap.put(...)} should return
     *   - the expected boolean result from {@code add}
     */
    private static Stream<Arguments> addBehaviorProvider() {
        // sentinel for “key already present”
        Object previous = new Object();
        return Stream.of(// brand‑new element → map.put returns null → true
        Arguments.of("alpha", null, true, "Adding a brand‑new element should return true"), // duplicate element → map.put returns previous → false
        Arguments.of("alpha", previous, false, "Adding the same element again should return false"), // different new element → true
        Arguments.of("beta", null, true, "Adding a different new element should return true"), // first null insertion → true
        Arguments.of(null, null, true, "Adding null for the first time should return true"), // duplicate null insertion → false
        Arguments.of(null, previous, false, "Adding null a second time should return false"));
    }

    @Test
    void addReturnsTrueWhenElementWasNotPresent() {
        // Arrange: map.put returns null to indicate the key was absent.
        when(mockMap.put("alpha", CompactSet.PRESENT)).thenReturn(null);
        // Act
        boolean result = setUnderTest.add("alpha");
        // Assert
        assertTrue(result, "add should return true when the element is newly inserted");
        verify(mockMap, times(1)).put("alpha", CompactSet.PRESENT);
    }

    @Test
    void addReturnsFalseWhenElementWasAlreadyPresent() {
        // Arrange: map.put returns a non‑null sentinel to indicate replacement.
        Object previous = new Object();
        when(mockMap.put("beta", CompactSet.PRESENT)).thenReturn(previous);
        // Act
        boolean result = setUnderTest.add("beta");
        // Assert
        assertFalse(result, "add should return false when the element already existed");
        verify(mockMap, times(1)).put("beta", CompactSet.PRESENT);
    }

    @Test
    void addPropagatesExceptionFromUnderlyingMap() {
        // Arrange: map.put throws a runtime exception.
        RuntimeException ex = new RuntimeException("map failure");
        when(mockMap.put("gamma", CompactSet.PRESENT)).thenThrow(ex);
        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> setUnderTest.add("gamma"));
        assertSame(ex, thrown, "add should propagate exceptions thrown by the underlying map");
        verify(mockMap, times(1)).put("gamma", CompactSet.PRESENT);
    }

    @Test
    void addHandlesNullElement() {
        // Arrange: map.put(null, PRESENT) returns null (first insertion).
        when(mockMap.put(null, CompactSet.PRESENT)).thenReturn(null);
        // Act
        boolean result = setUnderTest.add(null);
        // Assert
        assertTrue(result, "add should return true when null is added for the first time");
        verify(mockMap, times(1)).put(null, CompactSet.PRESENT);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("addBehaviorProvider")
    void addBehavior(String element, Object mapPutResult, boolean expected, String description) {
        // Arrange: configure the mock to return the supplied result.
        when(mockMap.put(element, CompactSet.PRESENT)).thenReturn(mapPutResult);
        // Act
        boolean result = setUnderTest.add(element);
        // Assert
        assertEquals(expected, result, () -> "add(" + element + ") should return " + expected);
        verify(mockMap, times(1)).put(element, CompactSet.PRESENT);
    }

    @Test
    void addAfterRemoval() {
        CompactSet<String> realSet = CompactSet.<String>builder().build();
        // add, remove, then add again – should return true both times
        assertTrue(realSet.add("alpha"), "First add should succeed");
        assertTrue(realSet.remove("alpha"), "Removal should succeed");
        assertTrue(realSet.add("alpha"), "Add after removal should succeed");
        // repeat with null
        assertTrue(realSet.add(null), "First add of null should succeed");
        assertTrue(realSet.remove(null), "Removal of null should succeed");
        assertTrue(realSet.add(null), "Add null after removal should succeed");
    }

    @Test
    void addAfterClear() {
        CompactSet<String> realSet = CompactSet.<String>builder().build();
        // Populate with several elements
        realSet.add("alpha");
        realSet.add("beta");
        realSet.add(null);
        assertEquals(3, realSet.size(), "Set should contain three elements before clear");
        // Clear the set
        realSet.clear();
        assertTrue(realSet.isEmpty(), "Set should be empty after clear");
        // Adding after clear should return true
        assertTrue(realSet.add("beta"), "Add after clear should succeed for a new element");
        assertTrue(realSet.add(null), "Add null after clear should succeed");
    }

    @Test
    void addEqualButDistinctObject() {
        CompactSet<Person> set = CompactSet.<Person>builder().build();
        Person p1 = new Person("John", 30);
        // equal to p1 but a different instance
        Person p2 = new Person("John", 30);
        assertTrue(set.add(p1), "First insertion of equal object should return true");
        assertFalse(set.add(p2), "Second insertion of equal object should return false");
        assertEquals(1, set.size(), "Set size should be 1 after adding equal objects");
    }

    @Test
    void addNewElementToNonEmptySet() {
        CompactSet<String> set = CompactSet.<String>builder().build();
        // Pre‑populate the set
        set.add("alpha");
        set.add("beta");
        assertEquals(2, set.size(), "Set should contain two elements before adding gamma");
        // Add a new distinct element
        assertTrue(set.add("gamma"), "Adding a new distinct element should return true");
        assertEquals(3, set.size(), "Set size should increase to three after adding gamma");
    }

    @Test
    void addOnUnmodifiableSetThrows() {
        CompactSet<String> unmodifiable = CompactSet.<String>builder().unmodifiable().build();
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add("newElement"), "Adding to an unmodifiable CompactSet should throw UnsupportedOperationException");
    }
}
