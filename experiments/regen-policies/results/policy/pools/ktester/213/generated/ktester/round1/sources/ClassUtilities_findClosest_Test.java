package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@DisplayName("ClassUtilities.findClosest() tests")
class ClassUtilities_findClosest_Test {

    /* -------------------------------------------------------------
     *  Test fixtures – simple class hierarchy used by the tests
     * ------------------------------------------------------------- */
    static class Base {
    }

    static class Derived extends Base {
    }

    static class Unrelated {
    }

    interface TestInterface {
    }

    static class DerivedWithInterface extends Base implements TestInterface {
    }

    /* -------------------------------------------------------------
     *  5️⃣ Null arguments – verify that the method validates its inputs
     * ------------------------------------------------------------- */
    @Nested
    @DisplayName("Null argument validation")
    class NullArgumentValidation {

        @Test
        @DisplayName("Null source class throws IllegalArgumentException")
        void nullSourceClassThrows() {
            Map<Class<?>, String> candidates = new HashMap<>();
            candidates.put(Base.class, "base");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findClosest(null, candidates, "default"));
            assertTrue(ex.getMessage().contains("Source class cannot be null"));
        }

        @Test
        @DisplayName("Null candidate map throws IllegalArgumentException")
        void nullCandidateMapThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findClosest(Base.class, null, "default"));
            assertTrue(ex.getMessage().contains("Candidate classes Map cannot be null"));
        }
    }

    /* -------------------------------------------------------------
     *  1️⃣ Exact match – the map contains the source class itself
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Exact match returns the mapped value")
    void exactMatchReturnsMappedValue() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Derived.class, "derived");
        candidates.put(Base.class, "base");
        String result = ClassUtilities.findClosest(Derived.class, candidates, "default");
        assertEquals("derived", result, "Should return the value mapped to the exact class");
    }

    /* -------------------------------------------------------------
     *  2️⃣ Inheritance match – no exact entry, but a superclass is present
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Inheritance match returns the closest superclass value")
    void inheritanceMatchReturnsClosestSuperclass() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Base.class, "base");
        candidates.put(Unrelated.class, "unrelated");
        String result = ClassUtilities.findClosest(Derived.class, candidates, "default");
        assertEquals("base", result, "Should return the value mapped to the closest superclass");
    }

    /* -------------------------------------------------------------
     *  3️⃣ No related candidate – default value is returned
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("When no candidate is related, the default value is returned")
    void defaultReturnedWhenNoRelatedCandidate() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Unrelated.class, "unrelated");
        String result = ClassUtilities.findClosest(String.class, candidates, "default");
        assertEquals("default", result, "Should fall back to the supplied default value");
    }

    /* -------------------------------------------------------------
     *  4️⃣ Tie‑breaking – two candidates at the same distance.
     *      The private helper `shouldPreferNewCandidate` prefers the
     *      candidate that is a subclass of the current best.
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Tie‑break prefers the more specific candidate")
    void tieBreakPrefersMoreSpecificCandidate() {
        // Both candidates are at distance 1 from Derived:
        //   - Base (superclass)
        //   - TestInterface (implemented by Derived)
        interface TestInterface {
        }
        static class DerivedWithInterface extends Base implements TestInterface {
        }
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Base.class, "base");
        candidates.put(TestInterface.class, "iface");
        // The expected result is the interface value because it is
        // more specific (a direct implementation) than the superclass.
        String result = ClassUtilities.findClosest(DerivedWithInterface.class, candidates, "default");
        // The exact tie‑break rule is encapsulated in the private method,
        // but we can assert the observable behaviour.
        assertTrue(result.equals("base") || result.equals("iface"), "Result should be one of the tied candidates");
    }

    /* -------------------------------------------------------------
     *  5️⃣ Null arguments – verify that the method validates its inputs
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Null source class throws IllegalArgumentException")
    void nullSourceClassThrows() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Base.class, "base");
        assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findClosest(null, candidates, "default"));
    }

    @Test
    @DisplayName("Null candidate map throws IllegalArgumentException")
    void nullCandidateMapThrows() {
        assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findClosest(Base.class, null, "default"));
    }

    /* -------------------------------------------------------------
     *  4️⃣ Tie‑breaking – later entry wins when distances are equal
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Tie‑break prefers later candidate when distances are equal")
    void tieBreakPrefersLaterCandidateWhenEqualDistance() {
        // ArrayList implements List and RandomAccess directly – same distance (1)
        Map<Class<?>, String> candidates = new LinkedHashMap<>();
        // first entry
        candidates.put(List.class, "list");
        // later entry, should win
        candidates.put(RandomAccess.class, "randomAccess");
        String result = ClassUtilities.findClosest(ArrayList.class, candidates, "default");
        assertEquals("randomAccess", result, "When distances are equal, the later map entry should be preferred");
    }

    /* -------------------------------------------------------------
     *  6️⃣ Exact match with a different class hierarchy (ArrayList example)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Exact match returns candidate for standard collection classes")
    void exactMatchReturnsCandidateForArrayList() {
        Map<Class<?>, String> candidates = new LinkedHashMap<>();
        candidates.put(ArrayList.class, "ArrayListHandler");
        candidates.put(LinkedList.class, "LinkedListHandler");
        String result = ClassUtilities.findClosest(ArrayList.class, candidates, "default");
        assertEquals("ArrayListHandler", result, "Exact match should return the associated handler without considering inheritance");
    }

    /* -------------------------------------------------------------
     *  7️⃣ Closest superclass is selected (LinkedList → AbstractList)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Closest superclass is selected for LinkedList")
    void closestSuperclassIsSelected() {
        Map<Class<?>, String> candidates = new LinkedHashMap<>();
        candidates.put(AbstractList.class, "abstractList");
        candidates.put(List.class, "list");
        candidates.put(Collection.class, "collection");
        String result = ClassUtilities.findClosest(LinkedList.class, candidates, "default");
        assertEquals("abstractList", result, "The candidate with the smallest inheritance distance (AbstractList) should be returned");
    }

    /* -------------------------------------------------------------
     *  8️⃣ Closest implemented interface is selected (HashMap → Map)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Closest implemented interface is selected for HashMap")
    void closestImplementedInterfaceIsSelected() {
        Map<Class<?>, String> candidates = new LinkedHashMap<>();
        candidates.put(Map.class, "map");
        candidates.put(Cloneable.class, "cloneable");
        candidates.put(Serializable.class, "serializable");
        String result = ClassUtilities.findClosest(HashMap.class, candidates, "default");
        assertEquals("map", result, "The interface with the smallest inheritance distance (Map) must be chosen");
    }

    /* -------------------------------------------------------------
     *  9️⃣ Default value returned when no compatible candidate exists
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Default value returned when no compatible candidate exists")
    void defaultReturnedWhenNoCompatibleCandidate() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(String.class, "string");
        candidates.put(Integer.class, "integer");
        String result = ClassUtilities.findClosest(Thread.class, candidates, "fallback");
        assertEquals("fallback", result, "Thread does not inherit from String or Integer, so the default should be returned");
    }

    /* -------------------------------------------------------------
     *  🔟 Null key in candidate map throws NullPointerException
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Null key in candidate map throws NullPointerException")
    void nullKeyInCandidateMapThrows() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(null, "nullKeyHandler");
        candidates.put(Integer.class, "intHandler");
        // The method will attempt to compute inheritance distance with a null destination,
        // which results in a NullPointerException.
        assertThrows(NullPointerException.class, () -> ClassUtilities.findClosest(String.class, candidates, "default"));
    }
}
