package com.cedarsoftware.util;

import java.util.*;
import java.lang.reflect.*;
import java.security.Permission;
// import java.security.ReflectPermission;
// import org.junit.*;
// import org.junit.Assert;
import org.junit.jupiter.api.Test;

class DeepEquals_deepHashCode_Test {

    /**
     * Simple node class used to create a circular reference.
     */
    private static class Node {

        Node next;

        final String name;

        Node(String name) {
            this.name = name;
        }
    }

    /**
     * Class with a private final field used for the reflective‑access test.
     */
    private static class ClassWithPrivateFinalField {

        private final String secret = "hidden";
    }

    /**
     * Iterable whose iterator mutates the underlying collection during traversal.
     */
    private static class ModifyingIterable implements Iterable<Integer> {

        private final List<Integer> backing;

        ModifyingIterable(List<Integer> list) {
            this.backing = list;
        }

        @Override
        public Iterator<Integer> iterator() {
            Iterator<Integer> it = backing.iterator();
            return new Iterator<Integer>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Integer next() {
                    if (!backing.isEmpty()) {
                        backing.remove(0);
                    }
                    return it.next();
                }
            };
        }
    }

    /**
     * Dummy interface for creating a proxy that does not implement Collection/Map.
     */
    private interface Dummy {
    }

    /**
     * Helper that creates a nested list where each element is a list containing the next level.
     * Depth of 0 returns an empty list.
     */
    private static List<Object> createNestedList(int depth) {
        if (depth == 0) {
            return new ArrayList<>();
        }
        List<Object> inner = createNestedList(depth - 1);
        List<Object> outer = new LinkedList<>();
        outer.add(inner);
        return outer;
    }

//     @Test
//     void deepHashCode_samePrimitiveWrapper_returnsSameHash() {
//         Integer a = 42;
//         Integer b = 42;
//         int hashA = DeepEquals.deepHashCode(a);
//         int hashB = DeepEquals.deepHashCode(b);
//         Assert.assertEquals("Hash codes for equal Integer values must match", hashA, hashB);
//     }

//     @Test
//     void deepHashCode_differentPrimitiveWrapper_returnsDifferentHash() {
//         Integer a = 42;
//         Integer b = 43;
//         int hashA = DeepEquals.deepHashCode(a);
//         int hashB = DeepEquals.deepHashCode(b);
//         Assert.assertNotEquals("Hash codes for different Integer values should differ", hashA, hashB);
//     }

//     @Test
//     void deepHashCode_sameIntArray_returnsSameHash() {
//         int[] arr1 = { 1, 2, 3, 4, 5 };
//         int[] arr2 = { 1, 2, 3, 4, 5 };
//         int hash1 = DeepEquals.deepHashCode(arr1);
//         int hash2 = DeepEquals.deepHashCode(arr2);
//         Assert.assertEquals("Identical int[] contents must produce identical hash codes", hash1, hash2);
//     }

//     @Test
//     void deepHashCode_differentIntArray_returnsDifferentHash() {
//         int[] arr1 = { 1, 2, 3 };
//         int[] arr2 = { 1, 2, 4 };
//         int hash1 = DeepEquals.deepHashCode(arr1);
//         int hash2 = DeepEquals.deepHashCode(arr2);
//         Assert.assertNotEquals("Different int[] contents should produce different hash codes", hash1, hash2);
//     }

//     @Test
//     void deepHashCode_sameList_returnsSameHash() {
//         List<String> list1 = Arrays.asList("alpha", "bravo", "charlie");
//         List<String> list2 = Arrays.asList("alpha", "bravo", "charlie");
//         int hash1 = DeepEquals.deepHashCode(list1);
//         int hash2 = DeepEquals.deepHashCode(list2);
//         Assert.assertEquals("Lists with identical ordered elements must have the same hash", hash1, hash2);
//     }

//     @Test
//     void deepHashCode_differentListOrder_returnsDifferentHash() {
//         List<String> list1 = Arrays.asList("alpha", "bravo", "charlie");
//         List<String> list2 = Arrays.asList("charlie", "bravo", "alpha");
//         int hash1 = DeepEquals.deepHashCode(list1);
//         int hash2 = DeepEquals.deepHashCode(list2);
//         Assert.assertNotEquals("Lists with same elements but different order must have different hashes", hash1, hash2);
//     }

//     @Test
//     void deepHashCode_sameSet_returnsSameHashRegardlessOfInsertionOrder() {
//         Set<String> set1 = new LinkedHashSet<>(Arrays.asList("x", "y", "z"));
//         Set<String> set2 = new LinkedHashSet<>(Arrays.asList("z", "y", "x"));
//         int hash1 = DeepEquals.deepHashCode(set1);
//         int hash2 = DeepEquals.deepHashCode(set2);
//         Assert.assertEquals("Sets with identical elements must produce identical hash codes irrespective of order", hash1, hash2);
//     }

//     @Test
//     void deepHashCode_sameMap_returnsSameHashRegardlessOfEntryOrder() {
//         Map<String, Integer> map1 = new LinkedHashMap<>();
//         map1.put("one", 1);
//         map1.put("two", 2);
//         map1.put("three", 3);
//         Map<String, Integer> map2 = new LinkedHashMap<>();
//         map2.put("three", 3);
//         map2.put("one", 1);
//         map2.put("two", 2);
//         int hash1 = DeepEquals.deepHashCode(map1);
//         int hash2 = DeepEquals.deepHashCode(map2);
//         Assert.assertEquals("Maps with identical key/value pairs must have the same hash code regardless of entry order", hash1, hash2);
//     }

//     @Test
//     void deepHashCode_differentMapValues_returnsDifferentHash() {
//         Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
//         Map<String, Integer> map2 = Map.of("a", 1, "b", 3);
//         int hash1 = DeepEquals.deepHashCode(map1);
//         int hash2 = DeepEquals.deepHashCode(map2);
//         Assert.assertNotEquals("Maps with different values must produce different hash codes", hash1, hash2);
//     }

    @Test
    void deepHashCode_circularReferenceDoesNotStackOverflow() {
        Node n1 = new Node("first");
        Node n2 = new Node("second");
        n1.next = n2;
        // creates a cycle
        n2.next = n1;
        // If an exception is thrown the test will fail automatically.
        DeepEquals.deepHashCode(n1);
    }

//     @Test
//     void deepHashCode_complexObjectGraph_consistentAcrossEqualStructures() {
//         Map<String, Object> mapA = new HashMap<>();
//         List<Object> listA = new ArrayList<>();
//         listA.add("hello");
//         listA.add(123);
//         mapA.put("list", listA);
//         mapA.put("number", 42);
//         mapA.put("nestedMap", Map.of("key", "value"));
//         Map<String, Object> mapB = new LinkedHashMap<>();
//         List<Object> listB = new LinkedList<>();
//         listB.add("hello");
//         listB.add(123);
//         mapB.put("list", listB);
//         mapB.put("number", 42);
//         mapB.put("nestedMap", new HashMap<>(Map.of("key", "value")));
//         int hashA = DeepEquals.deepHashCode(mapA);
//         int hashB = DeepEquals.deepHashCode(mapB);
//         Assert.assertEquals("Two deeply equal object graphs must yield the same hash code", hashA, hashB);
//     }

//     @Test
//     void deepHashCode_nullInput_returnsZero() {
//         int result = DeepEquals.deepHashCode(null);
//         Assert.assertEquals("deepHashCode(null) must return 0", 0, result);
//     }

//     @Test
//     void deepHashCode_deepRecursionStackOverflow_throwsStackOverflowError() {
//         List<Object> root = createNestedList(5000);
//         try {
//             DeepEquals.deepHashCode(root);
//             Assert.fail("Expected StackOverflowError");
//         } catch (StackOverflowError e) {
//             // expected
//         }
//     }

//     @Test
//     void deepHashCode_inaccessibleFieldAccess_throwsRuntimeException() {
//         SecurityManager original = System.getSecurityManager();
//         System.setSecurityManager(new SecurityManager() {
// 
//             @Override
//             public void checkPermission(Permission perm) {
//                 if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
//                     throw new SecurityException("Reflective access denied by test SecurityManager");
//                 }
//             }
//         });
//         try {
//             ClassWithPrivateFinalField obj = new ClassWithPrivateFinalField();
//             RuntimeException thrown = Assert.assertThrows(RuntimeException.class, () -> DeepEquals.deepHashCode(obj));
//             Assert.assertTrue("The cause of the RuntimeException must be a SecurityException", thrown.getCause() instanceof SecurityException);
//         } finally {
//             System.setSecurityManager(original);
//         }
//     }

//     @Test
//     void deepHashCode_concurrentModificationDuringTraversal_throwsConcurrentModificationException() {
//         List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
//         ModifyingIterable iterable = new ModifyingIterable(backing);
//         Assert.assertThrows(ConcurrentModificationException.class, () -> DeepEquals.deepHashCode(iterable));
//     }

//     @Test
//     void deepHashCode_unsupportedProxyType_throwsIllegalArgumentException() {
//         Dummy proxy = (Dummy) Proxy.newProxyInstance(Dummy.class.getClassLoader(), new Class<?>[] { Dummy.class }, (proxyObj, method, args) -> null);
//         Assert.assertThrows(IllegalArgumentException.class, () -> DeepEquals.deepHashCode(proxy));
//     }
}