package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepHashCode_Test {

    /* ---------------------------------------------------------------------- */
    private static class Node {

        int value;

        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    /**
     * Simple POJO without overriding {@code hashCode()} or {@code equals()}.
     */
    static class SimplePojo {

        final int id;

        final String name;

        SimplePojo(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * POJO that provides its own deterministic {@code hashCode()}.
     */
    static class CustomHashPojo {

        final int id;

        final String name;

        CustomHashPojo(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode() {
            // deterministic custom hash – just a simple combination
            return 31 * id + (name == null ? 0 : name.hashCode());
        }
    }

    /**
     * POJO whose {@code hashCode()} deliberately throws a {@code RuntimeException}.
     */
    static class ExceptionHashPojo {

        @Override
        public int hashCode() {
            throw new RuntimeException("hashCode exception");
        }
    }

    /**
     * Collection whose iterator throws a {@code RuntimeException} on {@code next()}.
     */
    static class ExceptionIterCollection extends AbstractCollection<Object> {

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Object next() {
                    throw new RuntimeException("iterator.next() exception");
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }

    /**
     * Key object whose {@code hashCode()} throws a {@code RuntimeException}.
     */
    static class ExceptionHashKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("key hashCode exception");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /**
     * Map that contains a key whose {@code hashCode()} throws.
     */
    static class ExceptionKeyMap extends HashMap<Object, Object> {

        ExceptionKeyMap() {
            put(new ExceptionHashKey(), "value");
        }
    }

    /**
     * List that modifies itself while being iterated.
     * This triggers a {@link ConcurrentModificationException}
     * when the iterator detects the structural change.
     */
    static class ModifyingList<E> extends ArrayList<E> {

        ModifyingList(Collection<? extends E> c) {
            super(c);
        }

        @Override
        public Iterator<E> iterator() {
            final Iterator<E> it = super.iterator();
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    // Remove the first element during iteration to cause CME
                    if (!isEmpty()) {
                        ModifyingList.this.remove(0);
                    }
                    return it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }
    }

    /* ---------------------------------------------------------------------- */
    private static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(42), Integer.valueOf(42).hashCode()), Arguments.of(Long.valueOf(123456789L), Long.valueOf(123456789L).hashCode()), Arguments.of(Boolean.TRUE, Boolean.TRUE.hashCode()));
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofInteger_isIntegerHashCode() {
        Integer i = 42;
        // Integer.hashCode() returns the int value
        int expected = i.hashCode();
        int actual = DeepEquals.deepHashCode(i);
        assertEquals(expected, actual, "deepHashCode should delegate to Integer.hashCode()");
    }

    @Test
    void deepHashCode_ofString_isStringHashCode() {
        String s = "deepHashTest";
        int expected = s.hashCode();
        int actual = DeepEquals.deepHashCode(s);
        assertEquals(expected, actual, "deepHashCode should delegate to String.hashCode()");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofTwoEqualListsDifferentImplementations() {
        List<String> arrayList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c"));
        int hashArrayList = DeepEquals.deepHashCode(arrayList);
        int hashLinkedList = DeepEquals.deepHashCode(linkedList);
        assertEquals(hashArrayList, hashLinkedList, "Two lists with the same ordered contents must have identical deep hash codes");
    }

    @Test
    void deepHashCode_ofTwoEqualSetsDifferentImplementations() {
        Set<Integer> hashSet = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> linkedHashSet = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        int hashHashSet = DeepEquals.deepHashCode(hashSet);
        int hashLinkedHashSet = DeepEquals.deepHashCode(linkedHashSet);
        assertEquals(hashHashSet, hashLinkedHashSet, "Two sets with the same elements must have identical deep hash codes");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofTwoEqualMapsDifferentImplementations() {
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("one", 1);
        hashMap.put("two", 2);
        Map<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("one", 1);
        treeMap.put("two", 2);
        int hashHashMap = DeepEquals.deepHashCode(hashMap);
        int hashTreeMap = DeepEquals.deepHashCode(treeMap);
        assertEquals(hashHashMap, hashTreeMap, "Two maps with identical entries must produce the same deep hash code");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofIntArray_isDeterministic() {
        int[] a1 = { 1, 2, 3, 4 };
        int[] a2 = { 1, 2, 3, 4 };
        int hash1 = DeepEquals.deepHashCode(a1);
        int hash2 = DeepEquals.deepHashCode(a2);
        assertEquals(hash1, hash2, "Identical primitive arrays must have the same deep hash code");
    }

    @Test
    void deepHashCode_ofObjectArray_isDeterministic() {
        String[] a1 = { "x", "y", "z" };
        String[] a2 = { "x", "y", "z" };
        int hash1 = DeepEquals.deepHashCode(a1);
        int hash2 = DeepEquals.deepHashCode(a2);
        assertEquals(hash1, hash2, "Identical object arrays must have the same deep hash code");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_handlesSelfReferencingObjectGraph() {
        Node node = new Node(10);
        // create a cycle
        node.next = node;
        // The call must complete without throwing StackOverflowError or other exceptions.
        assertDoesNotThrow(() -> DeepEquals.deepHashCode(node), "deepHashCode should handle cyclic object graphs gracefully");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofNestedCollection_isConsistent() {
        List<List<String>> nested1 = new ArrayList<>();
        nested1.add(Arrays.asList("a", "b"));
        nested1.add(Arrays.asList("c", "d"));
        List<List<String>> nested2 = new LinkedList<>();
        nested2.add(Arrays.asList("a", "b"));
        nested2.add(Arrays.asList("c", "d"));
        int hash1 = DeepEquals.deepHashCode(nested1);
        int hash2 = DeepEquals.deepHashCode(nested2);
        assertEquals(hash1, hash2, "Two nested collections with identical contents must yield the same deep hash code");
    }

    @Test
    void deepHashCode_ofLong_isLongHashCode() {
        Long l = 123456789L;
        // Long.hashCode() = (int)(value ^ (value>>>32))
        int expected = l.hashCode();
        int actual = DeepEquals.deepHashCode(l);
        assertEquals(expected, actual, "deepHashCode should delegate to Long.hashCode()");
    }

    @Test
    void deepHashCode_ofBoolean_isBooleanHashCode() {
        Boolean b = Boolean.TRUE;
        // defined as 1231
        int expected = b.hashCode();
        int actual = DeepEquals.deepHashCode(b);
        assertEquals(expected, actual, "deepHashCode should delegate to Boolean.hashCode()");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofNull_isZero() {
        assertEquals(0, DeepEquals.deepHashCode(null), "When the input is null the method should return 0");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofBigDecimal_isNonZero() {
        BigDecimal bd = new BigDecimal("123.45");
        int hash = DeepEquals.deepHashCode(bd);
        assertNotEquals(0, hash, "BigDecimal should produce a deterministic non‑zero hash code");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofDate_isNonZero() {
        // fixed epoch for reproducibility
        Date d = new Date(1633024800000L);
        int hash = DeepEquals.deepHashCode(d);
        assertNotEquals(0, hash, "Date should delegate to Long.hashCode() of its internal time");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofIntArray_matchesExpected() {
        int[] arr = new int[] { 1, 2, 3 };
        // value taken from the supplied test case
        int expected = 30817;
        int actual = DeepEquals.deepHashCode(arr);
        assertEquals(expected, actual, "int[] deep hash should match the expected constant");
    }

    @Test
    void deepHashCode_ofObjectArray_isNonZero() {
        Object[] arr = new Object[] { Integer.valueOf(5), "test", null };
        int hash = DeepEquals.deepHashCode(arr);
        assertNotEquals(0, hash, "Object[] deep hash should combine element hashes and be non‑zero");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofList_isNonZero() {
        List<Integer> list = Arrays.asList(10, 20, 30);
        int hash = DeepEquals.deepHashCode(list);
        assertNotEquals(0, hash, "List hash should be order‑sensitive and non‑zero");
    }

    @Test
    void deepHashCode_ofSet_isNonZero() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        int hash = DeepEquals.deepHashCode(set);
        assertNotEquals(0, hash, "Set hash should be order‑independent and non‑zero");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofMap_isNonZero() {
        Map<String, Integer> map = Map.of("one", 1, "two", 2);
        int hash = DeepEquals.deepHashCode(map);
        assertNotEquals(0, hash, "Map hash should combine entry hashes and be non‑zero");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofSimplePojo_usesIdentityHashCode() {
        SimplePojo pojo = new SimplePojo(7, "seven");
        int expected = System.identityHashCode(pojo);
        int actual = DeepEquals.deepHashCode(pojo);
        assertEquals(expected, actual, "When a class does not override hashCode(), deepHashCode should fall back to identity hash");
    }

    @Test
    void deepHashCode_ofCustomHashPojo_usesOverriddenHashCode() {
        CustomHashPojo pojo = new CustomHashPojo(7, "seven");
        // deterministic custom hash
        int expected = pojo.hashCode();
        int actual = DeepEquals.deepHashCode(pojo);
        assertEquals(expected, actual, "deepHashCode must use the custom hashCode() implementation when present");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void deepHashCode_ofExceptionHashPojo_propagatesException() {
        ExceptionHashPojo pojo = new ExceptionHashPojo();
        assertThrows(RuntimeException.class, () -> DeepEquals.deepHashCode(pojo), "Exception thrown from hashCode() should propagate");
    }

    @Test
    void deepHashCode_ofExceptionIterCollection_propagatesException() {
        ExceptionIterCollection coll = new ExceptionIterCollection();
        assertThrows(RuntimeException.class, () -> DeepEquals.deepHashCode(coll), "Exception thrown from iterator.next() should propagate");
    }

    @Test
    void deepHashCode_ofExceptionKeyMap_propagatesException() {
        ExceptionKeyMap map = new ExceptionKeyMap();
        assertThrows(RuntimeException.class, () -> DeepEquals.deepHashCode(map), "Exception thrown from a map key's hashCode() should propagate");
    }

    @Test
    void deepHashCode_ofModifyingList_throwsConcurrentModificationException() {
        ModifyingList<Integer> list = new ModifyingList<>(Arrays.asList(1, 2, 3));
        assertThrows(ConcurrentModificationException.class, () -> DeepEquals.deepHashCode(list), "Concurrent modification during hashing should result in CME");
    }

    @Test
    void deepHashCode_handlesSelfReferencingList() {
        List<Object> list = new ArrayList<>();
        // list contains itself
        list.add(list);
        assertDoesNotThrow(() -> DeepEquals.deepHashCode(list), "deepHashCode must not overflow when a list contains itself");
    }

    @Test
    void deepHashCode_ofDeeplyNestedMaps_isEqual() {
        Map<String, Object> innerA = new HashMap<>();
        innerA.put("num", 42);
        innerA.put("txt", "foo");
        Map<String, Object> nestedMapA = new HashMap<>();
        nestedMapA.put("inner", innerA);
        nestedMapA.put("list", Arrays.asList(1, 2, 3));
        // Create a second, separate but structurally identical map
        Map<String, Object> innerB = new HashMap<>();
        innerB.put("num", 42);
        innerB.put("txt", "foo");
        Map<String, Object> nestedMapB = new HashMap<>();
        nestedMapB.put("inner", innerB);
        nestedMapB.put("list", Arrays.asList(1, 2, 3));
        int hashA = DeepEquals.deepHashCode(nestedMapA);
        int hashB = DeepEquals.deepHashCode(nestedMapB);
        assertEquals(hashA, hashB, "Identical nested map structures must produce the same deep hash code");
    }

    @ParameterizedTest(name = "deepHashCode({0}) == {1}")
    @MethodSource("primitiveWrapperProvider")
    void deepHashCode_ofPrimitiveWrappers(Object input, int expected) {
        assertEquals(expected, DeepEquals.deepHashCode(input));
    }
}
