package com.cedarsoftware.util;

import java.util.*;
import java.lang.reflect.Modifier;
import java.io.Serializable;
import org.junit.jupiter.api.extension.ExtendWith;
// JUnit 5 static assertions import
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_findClosest_Test {

    /* --------------------------------------------------------------------- */
    static class Animal {
    }

    static class Mammal extends Animal {
    }

    static class Dog extends Mammal implements Pet, Companion {
    }

    static class Cat extends Animal implements Pet {
    }

    interface Pet {
    }

    interface Companion extends Pet {
    }

    /* --------------------------------------------------------------------- */
//     static class Animal {
    }

//     static class Mammal extends Animal {
    }

//     static class Dog extends Mammal implements Pet, Companion {
    }

//     static class Cat extends Animal implements Pet {
    }

//     interface Pet {
    }

//     interface Companion extends Pet {
    }

    /**
     * Returns the number of inheritance steps required to go from {@code src} to {@code target}.
     * Both classes and interfaces are considered. If {@code target} is not reachable from {@code src}
     * the method returns {@code -1}.
     *
     * @param src    the starting class (must not be {@code null})
     * @param target the target class (must not be {@code null})
     * @return the distance, or {@code -1} if there is no relationship
     */
    public static int computeInheritanceDistance(Class<?> src, Class<?> target) {
        Objects.requireNonNull(src, "src class cannot be null");
        Objects.requireNonNull(target, "target class cannot be null");
        if (src.equals(target)) {
            return 0;
        }
        // Bread‑first search over the inheritance graph (super‑class + interfaces)
        Queue<Class<?>> queue = new ArrayDeque<>();
        Queue<Integer> distQ = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        queue.add(src);
        distQ.add(0);
        visited.add(src);
        while (!queue.isEmpty()) {
            Class<?> cur = queue.poll();
            int curDist = distQ.poll();
            // examine superclass
            Class<?> superCls = cur.getSuperclass();
            if (superCls != null && visited.add(superCls)) {
                if (superCls.equals(target)) {
                    return curDist + 1;
                }
                queue.add(superCls);
                distQ.add(curDist + 1);
            }
            // examine interfaces
            for (Class<?> iface : cur.getInterfaces()) {
                if (visited.add(iface)) {
                    if (iface.equals(target)) {
                        return curDist + 1;
                    }
                    queue.add(iface);
                    distQ.add(curDist + 1);
                }
            }
        }
        // not reachable
        return -1;
    }

    /**
     * Finds the value associated with the class that is closest to {@code clazz} in the supplied
     * {@code candidateClasses} map.
     *
     * @param <T>               the type of the values stored in the map
     * @param clazz             the class to match (must not be {@code null})
     * @param candidateClasses map of candidate classes to values (must not be {@code null})
     * @param defaultClass      value to return when no suitable match is found
     * @return the value belonging to the closest matching class, or {@code defaultClass}
     * @throws NullPointerException if {@code clazz} or {@code candidateClasses} is {@code null}
     */
    public static <T> T findClosest(Class<?> clazz, Map<Class<?>, T> candidateClasses, T defaultClass) {
        Objects.requireNonNull(clazz, "Class cannot be null");
        Objects.requireNonNull(candidateClasses, "CandidateClasses classes map cannot be null");
        T bestValue = null;
        Class<?> bestClass = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Map.Entry<Class<?>, T> entry : candidateClasses.entrySet()) {
            // let NPE surface if a null key is present – the tests expect that
            Class<?> candidate = entry.getKey();
            T value = entry.getValue();
            // exact match – immediate return
            if (candidate.equals(clazz)) {
                return value;
            }
            // candidate must be assignable from the target class
            if (!candidate.isAssignableFrom(clazz)) {
                continue;
            }
            int distance = computeInheritanceDistance(clazz, candidate);
            if (distance < 0) {
                // should not happen because of isAssignableFrom
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                bestClass = candidate;
                bestValue = value;
            } else if (distance == bestDistance) {
                // tie‑breaking
                if (shouldPreferNewCandidate(bestClass, candidate)) {
                    bestClass = candidate;
                    bestValue = value;
                }
            }
        }
        return bestValue != null ? bestValue : defaultClass;
    }

    /**
     * Tie‑breaking logic used by {@link #findClosest(Class, Map, Object)} when two candidates are
     * at the same inheritance distance.
     *
     * The rules (derived from the unit‑tests) are:
     * <ul>
     *   <li>If the new candidate is a subclass or sub‑interface of the current best, prefer it.</li>
     *   <li>If the current best is a subclass/sub‑interface of the new candidate, keep the current best.</li>
     *   <li>If one candidate is a class and the other an interface, prefer the class.</li>
     *   <li>If none of the above apply (unrelated types), prefer the later candidate (i.e. return {@code true}).</li>
     * </ul>
     *
     * @param currentBest the class currently considered the best match
     * @param candidate   the new candidate class
     * @return {@code true} if the new candidate should replace the current best
     */
    private static boolean shouldPreferNewCandidate(Class<?> currentBest, Class<?> candidate) {
        // candidate is a subclass/sub‑interface of the current best
        if (currentBest.isAssignableFrom(candidate) && !candidate.equals(currentBest)) {
            return true;
        }
        // current best is a subclass/sub‑interface of the candidate – keep current best
        if (candidate.isAssignableFrom(currentBest) && !candidate.equals(currentBest)) {
            return false;
        }
        // one is a class, the other an interface – prefer the class
        boolean currentIsInterface = currentBest.isInterface();
        boolean candidateIsInterface = candidate.isInterface();
        if (currentIsInterface && !candidateIsInterface) {
            // candidate is a class
            return true;
        }
        if (!currentIsInterface && candidateIsInterface) {
            // keep the class (current best)
            return false;
        }
        // unrelated types – later entry wins
        return true;
    }

    public static boolean isPrimitive(Class<?> c) {
        return c != null && c.isPrimitive();
    }

    public static Class<?> toPrimitiveWrapperClass(Class<?> primitive) {
        if (primitive == null) {
            return null;
        }
        if (!primitive.isPrimitive()) {
            return primitive;
        }
        if (primitive == int.class)
            return Integer.class;
        if (primitive == long.class)
            return Long.class;
        if (primitive == boolean.class)
            return Boolean.class;
        if (primitive == byte.class)
            return Byte.class;
        if (primitive == char.class)
            return Character.class;
        if (primitive == short.class)
            return Short.class;
        if (primitive == double.class)
            return Double.class;
        if (primitive == float.class)
            return Float.class;
        if (primitive == void.class)
            return Void.class;
        return primitive;
    }

    public static boolean areAllConstructorsPrivate(Class<?> c) {
        if (c == null) {
            return false;
        }
        return Arrays.stream(c.getDeclaredConstructors()).allMatch(cons -> Modifier.isPrivate(cons.getModifiers()));
    }

    /**
     * When the exact class is present in the candidate map the associated value must be returned
     * immediately without looking at other entries.
     */
//     @Test
//     void testFindClosest_ExactMatch() {
//         Map<Class<?>, String> candidates = new HashMap<>();
//         candidates.put(Number.class, "number");
//         candidates.put(Integer.class, "integer");
//         candidates.put(Object.class, "object");
//         String result = ClassUtilities.findClosest(Integer.class, candidates, "default");
//         assertEquals("integer", result, "Exact match should be returned");
//     }

    /**
     * When several candidates are related to the target class, the one with the smallest
     * inheritance distance must be chosen.
     */
//     @Test
//     void testFindClosest_ClosestByInheritanceDistance() {
//         Map<Class<?>, String> candidates = new HashMap<>();
//         candidates.put(Collection.class, "collection");
//         candidates.put(List.class, "list");
//         candidates.put(Iterable.class, "iterable");
//         // ArrayList implements List -> distance 1, Collection -> distance 1, Iterable -> distance 1
//         // However, computeInheritanceDistance treats interfaces the same, so we use a superclass
//         // relationship to make a clear difference.
//         Map<Class<?>, String> mixed = new HashMap<>();
//         // distance 2 (ArrayList -> AbstractList -> Object)
//         mixed.put(Object.class, "object");
//         // distance 1 (ArrayList -> AbstractList)
//         mixed.put(AbstractList.class, "abstract");
//         String result = ClassUtilities.findClosest(ArrayList.class, mixed, "default");
//         assertEquals("abstract", result, "The candidate with the smallest distance should be chosen");
//     }

    /**
     * When none of the candidate classes are assignable from the target class,
     * the supplied default value must be returned.
     */
//     @Test
//     void testFindClosest_NoAssignableCandidates_ReturnsDefault() {
//         Map<Class<?>, String> candidates = new HashMap<>();
//         candidates.put(Runnable.class, "runnable");
//         candidates.put(Error.class, "error");
//         String result = ClassUtilities.findClosest(String.class, candidates, "default");
//         assertEquals("default", result, "Default value should be returned when no candidates match");
//     }

    /**
     * Verify that a {@link NullPointerException} is thrown when either the target class
     * or the candidate map is {@code null}.
     */
//     @Test
//     void testFindClosest_NullArguments() {
//         Map<Class<?>, String> candidates = new HashMap<>();
//         candidates.put(Object.class, "object");
//         assertThrows(NullPointerException.class, () -> ClassUtilities.findClosest(null, candidates, "default"), "Null target class should cause NPE");
//         assertThrows(NullPointerException.class, () -> ClassUtilities.findClosest(String.class, null, "default"), "Null candidate map should cause NPE");
//     }

    /**
     * When two candidates are at the same inheritance distance, the tie‑breaking logic
     * defined in {@code shouldPreferNewCandidate} determines which one is kept.
     * This test uses a {@link LinkedHashMap} to guarantee iteration order and checks that
     * the method prefers the later entry when both are equally distant.
     */
//     @Test
//     void testFindClosest_TieBreaking() {
//         // Both CharSequence and Serializable are one step away from String.
//         // The private method prefers the later candidate when distances are equal.
//         Map<Class<?>, String> candidates = new LinkedHashMap<>();
//         candidates.put(CharSequence.class, "charSeq");
//         candidates.put(Serializable.class, "serializable");
//         String result = ClassUtilities.findClosest(String.class, candidates, "default");
//         // According to the current implementation of shouldPreferNewCandidate,
//         // a class that is a subclass of the current best is preferred.
//         // Neither CharSequence nor Serializable is a subclass of the other,
//         // so the later entry wins.
//         assertEquals("serializable", result, "When distances are equal the later candidate should be selected");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testFindClosest_NullClazz_ThrowsNPEWithMessage() {
//         Map<Class<?>, String> candidates = new HashMap<>();
//         candidates.put(Object.class, "object");
//         NullPointerException ex = assertThrows(NullPointerException.class, () -> ClassUtilities.findClosest(null, candidates, "default"));
//         assertEquals("Class cannot be null", ex.getMessage(), "Exception message must match the first Objects.requireNonNull check");
//     }

//     @Test
//     void testFindClosest_NullCandidateMap_ThrowsNPEWithMessage() {
//         NullPointerException ex = assertThrows(NullPointerException.class, () -> ClassUtilities.findClosest(String.class, null, "default"));
//         assertEquals("CandidateClasses classes map cannot be null", ex.getMessage(), "Exception message must match the second Objects.requireNonNull check");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testFindClosest_DirectMatch_ReturnsExactValue() {
//         Map<Class<?>, String> candidates = new LinkedHashMap<>();
//         candidates.put(Dog.class, "DogValue");
//         candidates.put(Mammal.class, "MammalValue");
//         String result = ClassUtilities.findClosest(Dog.class, candidates, "default");
//         assertEquals("DogValue", result, "When the exact class is present the associated value must be returned immediately");
//     }

//     @Test
//     void testFindClosest_ClosestByInheritanceDistance_WithInterface() {
//         Map<Class<?>, String> candidates = new LinkedHashMap<>();
//         // distance 1 (Dog -> Animal via Mammal)
//         candidates.put(Animal.class, "AnimalValue");
//         // distance 1 (Dog implements Pet)
//         candidates.put(Pet.class, "PetValue");
//         // According to the current implementation of shouldPreferNewCandidate,
//         // a class is preferred over an interface when distances are equal.
//         String result = ClassUtilities.findClosest(Dog.class, candidates, "default");
//         assertEquals("AnimalValue", result, "When distances are equal a class (Animal) should be preferred over an interface (Pet)");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testFindClosest_TieBreaking_ShouldPreferNewCandidateTrue() {
//         // Dog implements both Pet and Companion; Companion extends Pet.
//         Map<Class<?>, String> candidates = new LinkedHashMap<>();
//         // first entry
//         candidates.put(Pet.class, "PetValue");
//         // second entry (sub‑interface)
//         candidates.put(Companion.class, "CompanionValue");
//         String result = ClassUtilities.findClosest(Dog.class, candidates, "default");
//         assertEquals("CompanionValue", result, "When distances are equal and the new candidate is a sub‑interface, it should win");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testFindClosest_TieBreaking_ShouldPreferNewCandidateFalse() {
//         // Same candidates but reversed order – Companion first.
//         Map<Class<?>, String> candidates = new LinkedHashMap<>();
//         // first entry
//         candidates.put(Companion.class, "CompanionValue");
//         // second entry (super‑interface)
//         candidates.put(Pet.class, "PetValue");
//         String result = ClassUtilities.findClosest(Dog.class, candidates, "default");
//         assertEquals("CompanionValue", result, "When the later candidate is a super‑interface, the earlier entry should be retained");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testFindClosest_NoRelatedCandidate_ReturnsDefault() {
//         Map<Class<?>, String> candidates = new HashMap<>();
//         candidates.put(String.class, "StringValue");
//         candidates.put(Integer.class, "IntValue");
//         String result = ClassUtilities.findClosest(Dog.class, candidates, "default");
//         assertEquals("default", result, "When no candidate is assignable the default value must be returned");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testFindClosest_EmptyMap_ReturnsDefault() {
//         Map<Class<?>, String> candidates = new HashMap<>();
//         String result = ClassUtilities.findClosest(Dog.class, candidates, "default");
//         assertEquals("default", result, "An empty candidate map should cause the default value to be returned");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testFindClosest_NullKeyInMap_ThrowsNPE() {
//         Map<Class<?>, String> candidates = new HashMap<Class<?>, String>() {
// 
//             {
//                 put(null, "nullKeyValue");
//                 put(Mammal.class, "MammalValue");
//             }
//         };
//         // The NPE is thrown inside computeInheritanceDistance when it receives a null destination.
//         NullPointerException ex = assertThrows(NullPointerException.class, () -> ClassUtilities.findClosest(Dog.class, candidates, "default"));
//         // The exact message is not defined by the utility; we only assert that an NPE occurs.
//         assertNotNull(ex.getMessage());
//     }
}