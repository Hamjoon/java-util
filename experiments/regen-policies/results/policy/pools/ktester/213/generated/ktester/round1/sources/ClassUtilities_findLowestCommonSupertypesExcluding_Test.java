package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertypesExcluding_Test {

    /* -----------------------------------------------------------
       0) Test fixtures – simple hierarchy used by several tests
       ----------------------------------------------------------- */
    interface IA {
    }

    interface IB {
    }

    // sub‑interface of IA
    interface IC extends IA {
    }

    static class Base implements IA {
    }

    // inherits IA via Base, also implements IB
    static class Sub extends Base implements IB {
    }

    // implements both IA and IB directly
    static class Other implements IA, IB {
    }

    static class ImplementsOnlyIA implements IA {
    }

    static class ImplementsOnlyIB implements IB {
    }

    /* -----------------------------------------------------------
       1) Null handling
       ----------------------------------------------------------- */
    @Test
    void testNullArguments() {
        Set<Class<?>> result1 = ClassUtilities.findLowestCommonSupertypesExcluding(null, null, Collections.emptySet());
        assertTrue(result1.isEmpty(), "Both null => empty set");
        Set<Class<?>> result2 = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, null, Collections.emptySet());
        assertTrue(result2.isEmpty(), "One null => empty set");
        Set<Class<?>> result3 = ClassUtilities.findLowestCommonSupertypesExcluding(null, Integer.class, Collections.emptySet());
        assertTrue(result3.isEmpty(), "One null => empty set");
    }

    /* -----------------------------------------------------------
       2) Same class – excluded vs. not excluded
       ----------------------------------------------------------- */
    @Test
    void testSameClassNotExcluded() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, String.class, Collections.emptySet());
        assertEquals(Set.of(String.class), result, "Same class without exclusion should return singleton");
    }

    @Test
    void testSameClassExcluded() {
        Set<Class<?>> excluded = Set.of(String.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, String.class, excluded);
        assertTrue(result.isEmpty(), "Same class but excluded => empty set");
    }

    /* -----------------------------------------------------------
       3) Simple inheritance – lowest common superclass
       ----------------------------------------------------------- */
    @Test
    void testLowestCommonSuperclass() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Sub.class, Base.class, Collections.emptySet());
        // Sub extends Base, so the lowest common supertype is Base itself
        assertEquals(Set.of(Base.class), result);
    }

    /* -----------------------------------------------------------
       4) Common interface only
       ----------------------------------------------------------- */
    @Test
    void testCommonInterfaceOnly() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Sub.class, ImplementsOnlyIA.class, Collections.emptySet());
        // Both implement IA (Sub via Base), IA is the lowest common supertype
        assertEquals(Set.of(IA.class), result);
    }

    /* -----------------------------------------------------------
       5) Multiple lowest common supertypes (two unrelated interfaces)
       ----------------------------------------------------------- */
    @Test
    void testMultipleLowestCommonSupertypes() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Other.class, Sub.class, Collections.emptySet());
        // Both IA and IB are common and neither is a subtype of the other
        Set<Class<?>> expected = Set.of(IA.class, IB.class);
        assertEquals(expected, result);
    }

    /* -----------------------------------------------------------
       6) Excluding one of the common supertypes
       ----------------------------------------------------------- */
    @Test
    void testExcludingOneCommonSupertype() {
        Set<Class<?>> excluded = Set.of(IA.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Other.class, Sub.class, excluded);
        // IA excluded, only IB should remain
        assertEquals(Set.of(IB.class), result);
    }

    /* -----------------------------------------------------------
       7) No common supertype other than Object
       ----------------------------------------------------------- */
    @Test
    void testNoCommonSupertypeOtherThanObject() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Runnable.class, Error.class, Collections.emptySet());
        // Object is filtered out by the implementation (it is not part of the hierarchy info)
        assertTrue(result.isEmpty(), "Only Object in common => empty set");
    }

    /* -----------------------------------------------------------
       8) Excluding all common supertypes yields empty set
       ----------------------------------------------------------- */
    @Test
    void testExcludeAllCommonSupertypes() {
        Set<Class<?>> excluded = Set.of(IA.class, IB.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Other.class, Sub.class, excluded);
        assertTrue(result.isEmpty(), "All common supertypes excluded => empty set");
    }

    /* -----------------------------------------------------------
       9) Interface hierarchy – sub‑interface preferred over super‑interface
       ----------------------------------------------------------- */
    @Test
    void testInterfaceSubHierarchy() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(IC.class, ImplementsOnlyIA.class, Collections.emptySet());
        // IC extends IA, so the lowest common supertype is IA (IC is not common)
        assertEquals(Set.of(IA.class), result);
    }

    /* -----------------------------------------------------------
       1) Null excluded set should cause NPE
       ----------------------------------------------------------- */
    @Test
    void testNullExcludedSetThrowsNullPointerException() {
        // The method does not guard against a null 'excluded' argument.
        // Passing null must therefore result in a NullPointerException.
        assertThrows(NullPointerException.class, () -> ClassUtilities.findLowestCommonSupertypesExcluding(String.class, Integer.class, null), "Providing a null excluded set should throw NullPointerException");
    }

    /* -----------------------------------------------------------
       2) Excluded set that throws an unchecked exception
       ----------------------------------------------------------- */
    @Test
    void testExceptionThrowingExcludedSetPropagatesException() {
        // Create a Set implementation whose 'contains' method always throws IllegalStateException.
        Set<Class<?>> throwingSet = new Set<Class<?>>() {

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public boolean contains(Object o) {
                throw new IllegalStateException("contains() deliberately failed");
            }

            @Override
            public Iterator<Class<?>> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return a;
            }

            @Override
            public boolean add(Class<?> e) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends Class<?>> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
            }
        };
        // The IllegalStateException from 'contains' must bubble up unchanged.
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, HashSet.class, throwingSet), "Exception from excluded set should propagate");
        assertEquals("contains() deliberately failed", thrown.getMessage(), "Exception message should be preserved");
    }

    /* -----------------------------------------------------------
       3) Re‑use an existing functional test to ensure no regression
       ----------------------------------------------------------- */
    @Test
    void testLowestCommonSuperclassRegressionCheck() {
        // This mirrors an existing test but is kept here to guarantee that
        // the edge‑case tests did not affect normal behaviour.
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Sub.class, Base.class, Collections.emptySet());
        assertEquals(Set.of(Base.class), result, "Sub extends Base – the lowest common supertype must be Base");
    }
}
