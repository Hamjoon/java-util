package com.cedarsoftware.util;

import java.util.*;
import java.security.Permission;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import java.lang.reflect.*;
// import java.util.concurrent.ConcurrentModificationException;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class DeepEquals_deepHashCode_Test {

    private static final class Person {

        private final String name;

        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private static final class Node {

        Node next;

        final int value;

        Node(int value) {
            this.value = value;
        }
    }

    private static final class SimplePojo {

        private final int id;

        private final String label;

        SimplePojo(int id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    private static final class CustomHashPojo {

        private final int id;

        private final String text;

        CustomHashPojo(int id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, text);
        }
    }

    private static final class SensitivePojo {

        private final String secret;

        SensitivePojo(String secret) {
            this.secret = secret;
        }
    }

    private static final class InaccessibleFieldPojo {

        private final String hidden = "cannotRead";
    }

    private static final class SelfModifyingList<E> extends AbstractList<E> {

        private final List<E> delegate;

        private final Iterator<E> iterator;

        SelfModifyingList(List<E> source) {
            this.delegate = new ArrayList<>(source);
            this.iterator = delegate.iterator();
        }

        @Override
        public E get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public E next() {
                    E e = iterator.next();
                    // mutate while iterating
                    iterator.remove();
                    return e;
                }
            };
        }
    }

    /**
     * SecurityManager that forbids reflective field access.
     */
    private static final class ThrowingSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            // allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // allow everything else
        }

        // Not @Override because some JDK versions treat this method as non‑overridable.
        protected void checkMemberAccess(Class<?> clazz, int which) {
            // Disallow reflective access to fields.
            if (which == Member.PUBLIC || which == Member.DECLARED) {
                throw new SecurityException("Reflective access denied by test SecurityManager");
            }
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        System.setSecurityManager(null);
    }

//     @AfterEach
    void teardownAfterEach() {
        System.setSecurityManager(null);
    }

    /* --------------------------------------------------------------
     * Helper for expected exceptions (JUnit‑4 style)
     * -------------------------------------------------------------- */
//     private static <T extends Throwable> void expectThrows(Class<T> expected, Runnable runnable) {
//         try {
//             runnable.run();
//             Assert.fail("Expected exception: " + expected.getName());
//         } catch (Throwable actual) {
//             if (!expected.isInstance(actual)) {
//                 Assert.fail("Unexpected exception type, expected " + expected.getName() + " but got " + actual.getClass().getName());
//             }
//         }
//     }

//     @Test
//     void deepHashCode_ofSamePrimitiveWrapper_isEqual() {
//         Integer a = 42;
//         Integer b = Integer.valueOf(42);
//         Assert.assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
//     }

//     @Test
//     void deepHashCode_ofNull_returnsZero() {
//         int hash = DeepEquals.deepHashCode(null);
//         Assert.assertEquals(0, hash);
//     }

//     @Test
//     void deepHashCode_ofIdenticalIntArray_isEqual() {
//         int[] arr1 = { 1, 2, 3, 4 };
//         int[] arr2 = { 1, 2, 3, 4 };
//         Assert.assertEquals(DeepEquals.deepHashCode(arr1), DeepEquals.deepHashCode(arr2));
//     }

//     @Test
//     void deepHashCode_ofIdenticalObjectArray_isEqual() {
//         String[] a1 = { "alpha", "bravo", "charlie" };
//         String[] a2 = { "alpha", "bravo", "charlie" };
//         Assert.assertEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a2));
//     }

//     @Test
//     void deepHashCode_ofIdenticalLists_isEqual() {
//         List<String> list1 = List.of("one", "two", "three");
//         List<String> list2 = new ArrayList<>(list1);
//         Assert.assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));
//     }

//     @Test
//     void deepHashCode_ofIdenticalSets_isEqual() {
//         Set<Integer> set1 = new HashSet<>(Arrays.asList(5, 9, 13));
//         Set<Integer> set2 = new LinkedHashSet<>(Arrays.asList(5, 9, 13));
//         Assert.assertEquals(DeepEquals.deepHashCode(set1), DeepEquals.deepHashCode(set2));
//     }

//     @Test
//     void deepHashCode_ofIdenticalMaps_isEqual() {
//         Map<String, Integer> map1 = new LinkedHashMap<>();
//         map1.put("a", 1);
//         map1.put("b", 2);
//         map1.put("c", 3);
//         Map<String, Integer> map2 = new HashMap<>();
//         map2.put("a", 1);
//         map2.put("b", 2);
//         map2.put("c", 3);
//         Assert.assertEquals(DeepEquals.deepHashCode(map1), DeepEquals.deepHashCode(map2));
//     }

//     @Test
//     void deepHashCode_ofObjectGraphWithCircularReference_doesNotLoop() {
//         Node n1 = new Node(10);
//         Node n2 = new Node(20);
//         n1.next = n2;
//         // cycle
//         n2.next = n1;
//         int hash1 = DeepEquals.deepHashCode(n1);
//         int hash2 = DeepEquals.deepHashCode(n2);
//         Assert.assertEquals(hash1, hash2);
//     }

//     @Test
//     void deepHashCode_ofTwoDifferentObjects_isNotEqual() {
//         Person p1 = new Person("Alice", 30);
//         Person p2 = new Person("Bob", 30);
//         Assert.assertNotEquals(DeepEquals.deepHashCode(p1), DeepEquals.deepHashCode(p2));
//     }

//     @Test
//     void deepHashCode_ofObjectsWithSameLogicalState_isEqualEvenIfCustomEqualsIsIgnored() {
//         Person p1 = new Person("Charlie", 40);
//         Person p2 = new Person("Charlie", 40);
//         Assert.assertEquals(DeepEquals.deepHashCode(p1), DeepEquals.deepHashCode(p2));
//     }

//     @Test
//     void deepHashCode_ofNestedStructures_isConsistent() {
//         Map<String, List<Integer>> complex1 = new HashMap<>();
//         complex1.put("numbers", List.of(1, 2, 3));
//         complex1.put("empty", Collections.emptyList());
//         Map<String, List<Integer>> complex2 = new LinkedHashMap<>();
//         complex2.put("empty", Collections.emptyList());
//         complex2.put("numbers", new ArrayList<>(Arrays.asList(1, 2, 3)));
//         Assert.assertEquals(DeepEquals.deepHashCode(complex1), DeepEquals.deepHashCode(complex2));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_primitiveWrapper_Integer() {
//         Integer obj = Integer.valueOf(42);
//         int expected = obj.hashCode();
//         Assert.assertEquals(expected, DeepEquals.deepHashCode(obj));
//     }

//     @Test
//     void deepHashCode_primitiveWrapper_Long() {
//         Long obj = Long.valueOf(123456789L);
//         int expected = Long.hashCode(123456789L);
//         Assert.assertEquals(expected, DeepEquals.deepHashCode(obj));
//     }

//     @Test
//     void deepHashCode_primitiveWrapper_String() {
//         String obj = "hello world";
//         int expected = obj.hashCode();
//         Assert.assertEquals(expected, DeepEquals.deepHashCode(obj));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_primitiveIntArray_consistent() {
//         int[] arr = { 1, 2, 3 };
//         int hash = DeepEquals.deepHashCode(arr);
//         Assert.assertEquals(hash, DeepEquals.deepHashCode(new int[] { 1, 2, 3 }));
//     }

//     @Test
//     void deepHashCode_objectArrayMixedTypes_consistent() {
//         Object[] arr = { "a", 10, null };
//         int hash = DeepEquals.deepHashCode(arr);
//         Assert.assertEquals(hash, DeepEquals.deepHashCode(new Object[] { "a", 10, null }));
//     }

//     @Test
//     void deepHashCode_emptyPrimitiveArray_isZero() {
//         int[] empty = new int[0];
//         Assert.assertEquals(0, DeepEquals.deepHashCode(empty));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_list_orderSensitive() {
//         List<Integer> list = Arrays.asList(1, 2, 3);
//         int hash = DeepEquals.deepHashCode(list);
//         Assert.assertEquals(hash, DeepEquals.deepHashCode(Arrays.asList(1, 2, 3)));
//     }

//     @Test
//     void deepHashCode_set_orderIndependent() {
//         Set<String> set = new HashSet<>(Arrays.asList("x", "y"));
//         int hash = DeepEquals.deepHashCode(set);
//         Set<String> linked = new LinkedHashSet<>(Arrays.asList("y", "x"));
//         Assert.assertEquals(hash, DeepEquals.deepHashCode(linked));
//     }

//     @Test
//     void deepHashCode_emptyCollection_isZero() {
//         List<Object> empty = new LinkedList<>();
//         Assert.assertEquals(0, DeepEquals.deepHashCode(empty));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_map_orderIndependent() {
//         Map<String, Integer> map = new HashMap<>();
//         map.put("a", 1);
//         map.put("b", 2);
//         int hash = DeepEquals.deepHashCode(map);
//         Map<String, Integer> linked = new LinkedHashMap<>();
//         linked.put("b", 2);
//         linked.put("a", 1);
//         Assert.assertEquals(hash, DeepEquals.deepHashCode(linked));
//     }

//     @Test
//     void deepHashCode_emptyMap_isZero() {
//         Assert.assertEquals(0, DeepEquals.deepHashCode(Collections.emptyMap()));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_customPojo_withoutOverrides_isFieldBased() {
//         SimplePojo pojo = new SimplePojo(5, "test");
//         int hash = DeepEquals.deepHashCode(pojo);
//         SimplePojo other = new SimplePojo(5, "test");
//         Assert.assertEquals(hash, DeepEquals.deepHashCode(other));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_customPojo_withOverrides_delegatesToHashCode() {
//         CustomHashPojo pojo = new CustomHashPojo(7, "foo");
//         int expected = pojo.hashCode();
//         Assert.assertEquals(expected, DeepEquals.deepHashCode(pojo));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_cyclicGraph_isConsistent() {
//         Node a = new Node(1);
//         Node b = new Node(2);
//         a.next = b;
//         // cycle
//         b.next = a;
//         int hash = DeepEquals.deepHashCode(a);
//         Assert.assertEquals(hash, DeepEquals.deepHashCode(b));
//     }

//     @Test
//     void deepHashCode_multiDimensionalArray_isConsistent() {
//         int[][] matrix = { { 1, 2 }, { 3, 4 } };
//         int hash = DeepEquals.deepHashCode(matrix);
//         Assert.assertEquals(hash, DeepEquals.deepHashCode(new int[][] { { 1, 2 }, { 3, 4 } }));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_Integer_MAX_VALUE() {
//         Integer max = Integer.valueOf(Integer.MAX_VALUE);
//         Assert.assertEquals(max.hashCode(), DeepEquals.deepHashCode(max));
//     }

//     @Test
//     void deepHashCode_Integer_MIN_VALUE() {
//         Integer min = Integer.valueOf(Integer.MIN_VALUE);
//         Assert.assertEquals(min.hashCode(), DeepEquals.deepHashCode(min));
//     }

//     @Test
//     void deepHashCode_Long_MAX_VALUE() {
//         Long max = Long.valueOf(Long.MAX_VALUE);
//         Assert.assertEquals(Long.hashCode(Long.MAX_VALUE), DeepEquals.deepHashCode(max));
//     }

//     @Test
//     void deepHashCode_Long_MIN_VALUE() {
//         Long min = Long.valueOf(Long.MIN_VALUE);
//         Assert.assertEquals(Long.hashCode(Long.MIN_VALUE), DeepEquals.deepHashCode(min));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_sensitivePojo_throwsSecurityException() {
//         System.setSecurityManager(new ThrowingSecurityManager());
//         SensitivePojo pojo = new SensitivePojo("secret");
//         expectThrows(SecurityException.class, () -> DeepEquals.deepHashCode(pojo));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_inaccessibleFieldPojo_throwsIllegalAccessException() throws Exception {
//         InaccessibleFieldPojo pojo = new InaccessibleFieldPojo();
//         Field hidden = InaccessibleFieldPojo.class.getDeclaredField("hidden");
//         hidden.setAccessible(false);
//         expectThrows(IllegalAccessException.class, () -> {
//             Method helper = DeepEquals.class.getDeclaredMethod("deepHashCode", Object.class, Set.class);
//             helper.setAccessible(true);
//             Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
//             helper.invoke(null, pojo, visited);
//         });
//     }

    // -----------------------------------------------------------------
//     @Test
//     @Disabled("Current DeepEquals implementation correctly handles primitive arrays; " + "the test documents the expected failure mode if a ClassCastException were to occur.")
//     void deepHashCode_primitiveArray_castException() {
//         int[] arr = { 1, 2, 3 };
//         expectThrows(ClassCastException.class, () -> DeepEquals.deepHashCode(arr));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_selfModifyingList_throwsConcurrentModificationException() {
//         List<Integer> source = Arrays.asList(1, 2, 3);
//         SelfModifyingList<Integer> list = new SelfModifyingList<>(source);
//         expectThrows(ConcurrentModificationException.class, () -> DeepEquals.deepHashCode(list));
//     }

    // -----------------------------------------------------------------
//     @Test
//     void deepHashCode_unmodifiableMap_throwsUnsupportedOperationException() {
//         Map<String, Integer> mutable = new HashMap<>();
//         mutable.put("a", 1);
//         mutable.put("b", 2);
//         Map<String, Integer> unmodifiable = Collections.unmodifiableMap(mutable);
//         expectThrows(UnsupportedOperationException.class, () -> DeepEquals.deepHashCode(unmodifiable));
//     }
}