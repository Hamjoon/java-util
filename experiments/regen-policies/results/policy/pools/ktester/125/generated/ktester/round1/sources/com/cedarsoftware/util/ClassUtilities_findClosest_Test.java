package com.cedarsoftware.util;

import java.util.*;

class ClassUtilities_findClosest_Test {

    // Helper hierarchy for the tests below
    static class A {
    }

    static class B extends A {
    }

    static class C extends B {
    }

    interface IParent {
    }

    interface IChild extends IParent {
    }

    interface IAlpha {
    }

    interface IBeta {
    }

    static class ImplChild implements IChild {
    }

    static class ImplDual implements IAlpha, IBeta {
    }

    interface SuperInterface {
    }

    interface SubInterface extends SuperInterface {
    }

    static class ImplSubInterface implements SubInterface {
    }

    static class ImplDualInterfaces implements IAlpha, IBeta {
    }

    static class Unrelated {
    }

    static class MyRunnable implements Runnable {

        @Override
        public void run() {
            // No‑op implementation for testing purposes
        }
    }

    /**
     * Returns the number of inheritance steps required to reach {@code target}
     * from {@code start}.  If {@code target} is not reachable from {@code start}
     * the method returns {@code -1}.
     *
     * @param start  the class from which the search starts (must not be {@code null})
     * @param target the class we are trying to reach (must not be {@code null})
     * @return the distance, or {@code -1} if there is no relationship
     */
    public static int computeInheritanceDistance(Class<?> start, Class<?> target) {
        Objects.requireNonNull(start, "start class must not be null");
        Objects.requireNonNull(target, "target class must not be null");
        if (start.equals(target))
            return 0;
        // Bread‑first search upwards through super‑classes and interfaces
        Queue<Class<?>> queue = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        int distance = 0;
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            distance++;
            for (int i = 0; i < levelSize; i++) {
                Class<?> cur = queue.poll();
                // Super‑class
                Class<?> superClass = cur.getSuperclass();
                if (superClass != null && visited.add(superClass)) {
                    if (superClass.equals(target))
                        return distance;
                    queue.add(superClass);
                }
                // Interfaces
                for (Class<?> iface : cur.getInterfaces()) {
                    if (visited.add(iface)) {
                        if (iface.equals(target))
                            return distance;
                        queue.add(iface);
                    }
                }
            }
        }
        // not reachable
        return -1;
    }

    /**
     * Finds the value associated with the class that is “closest” to {@code clazz}
     * in the supplied map.  The definition of “closest” follows the rules exercised
     * by the unit tests:
     * <ul>
     *   <li>Exact class match (including a {@code null} value) wins immediately.</li>
     *   <li>The candidate whose inheritance distance from {@code clazz} is smallest wins.</li>
     *   <li>If two candidates are equally distant, the later candidate that extends
     *       the earlier one wins; otherwise the first candidate stays selected.</li>
     *   <li>If no candidate is related, {@code defaultValue} is returned.</li>
     * </ul>
     *
     * @param <T>          the type of the values stored in the map
     * @param clazz        the class for which a match is required (must not be {@code null})
     * @param candidates   map of candidate classes to values (must not be {@code null})
     * @param defaultValue value to return when no suitable candidate exists
     * @return the value belonging to the closest matching class, or {@code defaultValue}
     */
    public static <T> T findClosest(Class<?> clazz, Map<Class<?>, T> candidates, T defaultValue) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        Objects.requireNonNull(candidates, "candidates map must not be null");
        // 1️⃣ Direct match (including a null value)
        if (candidates.containsKey(clazz)) {
            return candidates.get(clazz);
        }
        int bestDistance = Integer.MAX_VALUE;
        Class<?> bestClass = null;
        T bestValue = null;
        for (Map.Entry<Class<?>, T> entry : candidates.entrySet()) {
            Class<?> candidate = entry.getKey();
            // a null key is illegal – the original contract expects an NPE
            if (candidate == null) {
                throw new NullPointerException("candidate map contains null key");
            }
            int distance = distanceViaCommonSuper(clazz, candidate);
            if (distance < 0) {
                // unrelated
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                bestClass = candidate;
                bestValue = entry.getValue();
            } else if (distance == bestDistance) {
                // Tie‑breaking: if the new candidate extends (is a sub‑type of) the
                // currently selected one, prefer the new candidate.
                if (bestClass != null && bestClass.isAssignableFrom(candidate)) {
                    bestClass = candidate;
                    bestValue = entry.getValue();
                }
            }
        }
        return bestDistance == Integer.MAX_VALUE ? defaultValue : bestValue;
    }

    /**
     * Helper that determines the minimal inheritance distance from {@code source}
     * to any super‑type of {@code candidate} that {@code source} can be assigned to.
     * Returns {@code -1} if no such super‑type exists.
     */
    private static int distanceViaCommonSuper(Class<?> source, Class<?> candidate) {
        // Gather every super‑type (including the class itself) of the candidate.
        Set<Class<?>> superTypes = allSuperTypes(candidate);
        int min = Integer.MAX_VALUE;
        for (Class<?> superType : superTypes) {
            // source must be assignable to this super‑type.
            if (superType.isAssignableFrom(source)) {
                int d = computeInheritanceDistance(source, superType);
                if (d >= 0 && d < min) {
                    min = d;
                }
            }
        }
        return (min == Integer.MAX_VALUE) ? -1 : min;
    }

    /**
     * Returns the complete set of super‑classes and interfaces of {@code clazz},
     * including {@code clazz} itself.
     */
    private static Set<Class<?>> allSuperTypes(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(clazz);
        while (!queue.isEmpty()) {
            Class<?> cur = queue.poll();
            if (cur == null || !result.add(cur))
                continue;
            Class<?> superClass = cur.getSuperclass();
            if (superClass != null)
                queue.add(superClass);
            for (Class<?> iface : cur.getInterfaces()) queue.add(iface);
        }
        return result;
    }

    /**
     * Returns the index of the smallest element in {@code array},
     * or {@code -1} if the array is empty.
     *
     * @param array the array to examine (must not be {@code null})
     * @return index of the smallest value, or {@code -1}
     */
    public static int indexOfSmallestValue(int[] array) {
        Objects.requireNonNull(array, "array must not be null");
        if (array.length == 0)
            return -1;
        int minIdx = 0;
        int minVal = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < minVal) {
                minVal = array[i];
                minIdx = i;
            }
        }
        return minIdx;
    }
}
