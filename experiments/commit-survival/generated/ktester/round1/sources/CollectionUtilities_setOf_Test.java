package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_setOf_Test {

    /**
     * Simple POJO used for equality testing.
     */
    private static final class Person {

        final int id;

        final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person p = (Person) o;
            return id == p.id && Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "Person{id=" + id + ", name='" + name + "'}";
        }
    }

    /**
     * Element whose hashCode() throws a RuntimeException
     */
    private static final class BadHash {

        @Override
        public int hashCode() {
            throw new IllegalStateException("Bad hashCode");
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    /**
     * Element whose equals() throws a RuntimeException
     */
    private static final class BadEquals {

        @Override
        public int hashCode() {
            // constant hash code to ensure equals() is invoked
            return 42;
        }

        @Override
        public boolean equals(Object obj) {
            throw new IllegalArgumentException("Bad equals");
        }
    }

    /**
     * A normal object used together with BadEquals to trigger duplicate detection
     */
    private static final class GoodObject {

        final String value;

        GoodObject(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof GoodObject))
                return false;
            GoodObject that = (GoodObject) o;
            return Objects.equals(value, that.value);
        }
    }

    @Test
    @DisplayName("setOf(null) returns a shared empty unmodifiable set")
    void setOf_NullArray_ReturnsEmptyUnmodifiableSet() {
        // given
        String[] input = null;
        // when
        Set<String> result = CollectionUtilities.setOf(input);
        Set<String> secondCall = CollectionUtilities.setOf(input);
        // then
        assertNotNull(result, "Result must not be null");
        assertTrue(result.isEmpty(), "Resulting set should be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"), "Set should be unmodifiable");
        // The implementation returns the same static instance for null/empty inputs
        assertSame(result, secondCall, "Both calls should return the same empty set instance");
    }

    @Test
    @DisplayName("setOf() with empty var‑args returns a shared empty unmodifiable set")
    void setOf_EmptyArray_ReturnsEmptyUnmodifiableSet() {
        // given / when
        Set<Integer> result = CollectionUtilities.setOf();
        Set<Integer> secondCall = CollectionUtilities.setOf();
        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.add(1));
        assertSame(result, secondCall, "Both empty calls should return the same set instance");
    }

    @Test
    @DisplayName("setOf(single element) returns an unmodifiable set containing that element")
    void setOf_SingleElement_ReturnsSetWithElement() {
        // given
        String element = "alpha";
        // when
        Set<String> result = CollectionUtilities.setOf(element);
        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(element));
        assertThrows(UnsupportedOperationException.class, () -> result.remove(element));
    }

    @Test
    @DisplayName("setOf(multiple elements) preserves insertion order and removes duplicates")
    void setOf_MultipleElements_PreservesOrderAndDeduplicates() {
        // given
        String a = "a";
        String b = "b";
        String c = "c";
        String duplicateB = "b";
        // when
        Set<String> result = CollectionUtilities.setOf(a, b, c, duplicateB);
        // then
        assertEquals(3, result.size(), "Duplicate values must be eliminated");
        assertTrue(result.containsAll(List.of(a, b, c)));
        // Verify insertion order (LinkedHashSet semantics)
        Iterator<String> it = result.iterator();
        assertEquals(a, it.next());
        assertEquals(b, it.next());
        assertEquals(c, it.next());
    }

    @Test
    @DisplayName("setOf with null elements inside the var‑args is allowed and treated as a value")
    void setOf_WithNullElement_IncludesNullInSet() {
        // given
        String first = "first";
        String second = null;
        String third = "third";
        // when
        Set<String> result = CollectionUtilities.setOf(first, second, third);
        // then
        assertEquals(3, result.size());
        assertTrue(result.contains(first));
        assertTrue(result.contains(null));
        assertTrue(result.contains(third));
    }

    @Test
    @DisplayName("setOf([null]) returns a set containing only null and is unmodifiable")
    void setOf_NullOnly_ReturnsSetWithNull() {
        Set<String> result = CollectionUtilities.setOf((String) null);
        assertEquals(1, result.size());
        assertTrue(result.contains(null));
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"));
    }

    @Test
    @DisplayName("setOf([null,null,\"a\",null]) removes duplicate nulls and preserves order")
    void setOf_DuplicateNulls_RemovesDuplicatesPreservesOrder() {
        Set<String> result = CollectionUtilities.setOf(null, null, "a", null);
        assertEquals(2, result.size());
        Iterator<String> it = result.iterator();
        assertNull(it.next(), "First element should be the first null occurrence");
        assertEquals("a", it.next(), "Second element should be the non‑null value");
    }

    @Test
    @DisplayName("setOf(Integer var‑args) removes duplicates and preserves order")
    void setOf_PrimitiveWrapper_DuplicatesRemoved() {
        Set<Integer> result = CollectionUtilities.setOf(1, 2, 3, 2, 1);
        assertEquals(List.of(1, 2, 3), new ArrayList<>(result), "Order must be first occurrence, duplicates eliminated");
        assertThrows(UnsupportedOperationException.class, () -> result.add(4));
    }

    @Test
    @DisplayName("setOf(custom objects) removes duplicates based on equals/hashCode")
    void setOf_CustomObject_Deduplication() {
        Person alice1 = new Person(1, "Alice");
        Person bob = new Person(2, "Bob");
        // equal to alice1
        Person alice2 = new Person(1, "Alice");
        Set<Person> result = CollectionUtilities.setOf(alice1, bob, alice2);
        assertEquals(2, result.size(), "Duplicate Person objects should be collapsed");
        assertTrue(result.containsAll(List.of(alice1, bob)));
    }

    @Test
    @DisplayName("setOf([\"x\",\"y\"]) throws UnsupportedOperationException on add")
    void setOf_Unmodifiable_AddThrows() {
        Set<String> result = CollectionUtilities.setOf("x", "y");
        assertThrows(UnsupportedOperationException.class, () -> result.add("z"));
    }

    @Test
    @DisplayName("setOf([\"x\",\"y\"]) throws UnsupportedOperationException on remove")
    void setOf_Unmodifiable_RemoveThrows() {
        Set<String> result = CollectionUtilities.setOf("x", "y");
        assertThrows(UnsupportedOperationException.class, () -> result.remove("x"));
    }

    @Test
    @DisplayName("Mutating the original array after setOf does not affect the returned set")
    void setOf_OriginalArrayMutation_UnchangedSet() {
        String[] source = new String[] { "orig1", "orig2" };
        Set<String> result = CollectionUtilities.setOf(source);
        // mutate the original array
        source[0] = "changed";
        // the set must still contain the original values
        assertTrue(result.containsAll(List.of("orig1", "orig2")));
        assertFalse(result.contains("changed"));
    }

    @Test
    @DisplayName("setOf propagates exception thrown from element.hashCode()")
    void setOf_HashCodeException_Propagated() {
        BadHash bad = new BadHash();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> CollectionUtilities.setOf(bad));
        assertEquals("Bad hashCode", thrown.getMessage());
    }

    @Test
    @DisplayName("setOf propagates exception thrown from element.equals() during duplicate detection")
    void setOf_EqualsException_Propagated() {
        GoodObject good = new GoodObject("good");
        BadEquals bad = new BadEquals();
        // The BadEquals instance has a constant hash code, so when the set tries to
        // determine if it already contains an equal element, equals() will be invoked.
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> CollectionUtilities.setOf(good, bad));
        assertEquals("Bad equals", thrown.getMessage());
    }
}
