package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableSet_toString_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // The default constructor used in the original test suite creates an empty underlying set.
        set = new SealableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary
        set = null;
    }

    /**
     * Creates a {@link SealableSet} that wraps the supplied {@code underlying} set.
     * The method uses reflection to replace the private {@code set} field.
     */
    private <T> SealableSet<T> wrapWith(Set<T> underlying) {
        SealableSet<T> wrapper = new SealableSet<>(sealedSupplier);
        try {
            Field f = SealableSet.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(wrapper, underlying);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return wrapper;
    }

    private static Stream<Arguments> basicToStringProvider() {
        return Stream.of(Arguments.of(new LinkedHashSet<>(Collections.emptyList()), "[]", "When the underlying set is empty, toString() should return the empty set representation \"[]\"."), Arguments.of(new LinkedHashSet<>(Collections.singletonList("foo")), "[foo]", "When the underlying set contains a single element \"foo\", toString() must return \"[foo]\"."), Arguments.of(new LinkedHashSet<>(Arrays.asList("a", "b", "c")), "[a, b, c]", "When the underlying set contains multiple elements, toString() should preserve the order provided by that Set implementation."));
    }

    /**
     * Verify that {@code toString()} returns a string that contains all elements
     * of the set, regardless of order.
     */
    @Test
    void testToStringContainsAllElements() {
        String s = set.toString();
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        // Extract the comma‑separated values inside the brackets
        String inner = s.substring(1, s.length() - 1).trim();
        Set<String> actualParts = new HashSet<>();
        if (!inner.isEmpty()) {
            for (String part : inner.split(",")) {
                actualParts.add(part.trim());
            }
        }
        Set<String> expectedParts = new HashSet<>();
        expectedParts.add("10");
        expectedParts.add("20");
        expectedParts.add("null");
        assertEquals(expectedParts, actualParts, "toString() should contain exactly the elements 10, 20 and null");
    }

    /**
     * An empty {@code SealableSet} should render as {@code []}.
     */
    @Test
    void testToStringEmptySet() {
        SealableSet<Integer> empty = new SealableSet<>(sealedSupplier);
        assertEquals("[]", empty.toString(), "An empty SealableSet should produce \"[]\"");
    }

    /**
     * The sealed state must not affect {@code toString()} – it should still
     * delegate to the underlying set.
     */
    @Test
    void testToStringWhenSealed() {
        // change the supplier's state
        sealed = true;
        String s = set.toString();
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        String inner = s.substring(1, s.length() - 1).trim();
        Set<String> actualParts = new HashSet<>();
        if (!inner.isEmpty()) {
            for (String part : inner.split(",")) {
                actualParts.add(part.trim());
            }
        }
        Set<String> expectedParts = new HashSet<>();
        expectedParts.add("10");
        expectedParts.add("20");
        expectedParts.add("null");
        assertEquals(expectedParts, actualParts, "toString() must remain correct even when the set is sealed");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicToStringProvider")
    void testBasicToString(Set<String> underlying, String expected, String description) {
        @SuppressWarnings("unchecked")
        SealableSet<String> ss = wrapWith(underlying);
        assertEquals(expected, ss.toString(), description);
    }

    @Test
    void testCustomSetToStringDelegation() {
        // Custom Set that overrides toString()
        Set<Integer> custom = new AbstractSet<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public String toString() {
                return "CustomSet[1,2]";
            }
        };
        SealableSet<Integer> ss = wrapWith(custom);
        assertEquals("CustomSet[1,2]", ss.toString(), "SealableSet should delegate to the wrapped Set's toString() unchanged.");
    }

    @Test
    void testToStringAfterMutation() {
        Set<String> underlying = new LinkedHashSet<>();
        SealableSet<String> ss = wrapWith(underlying);
        ss.add("x");
        ss.add("y");
        assertEquals("[x, y]", ss.toString(), "After adding elements, toString() must reflect the updated contents.");
    }

    @Test
    void testToStringAfterRemoval() {
        Set<String> underlying = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        SealableSet<String> ss = wrapWith(underlying);
        assertTrue(ss.remove("b"), "Removal should succeed");
        assertEquals("[a, c]", ss.toString(), "After removing \"b\", toString() should reflect the removal preserving order.");
    }

    @Test
    void testToStringAfterClear() {
        Set<String> underlying = new LinkedHashSet<>(Arrays.asList("x", "y"));
        SealableSet<String> ss = wrapWith(underlying);
        ss.clear();
        assertEquals("[]", ss.toString(), "After clear(), toString() must return the empty set representation.");
    }

    @Test
    void testToStringWithNullElement() {
        Set<String> underlying = new LinkedHashSet<>(Arrays.asList(null, "foo"));
        SealableSet<String> ss = wrapWith(underlying);
        assertEquals("[null, foo]", ss.toString(), "Null element should be rendered as the literal \"null\" in the correct order.");
    }

    @Test
    void testDuplicateAddDoesNotChangeToString() {
        Set<String> underlying = new LinkedHashSet<>(Collections.singletonList("a"));
        SealableSet<String> ss = wrapWith(underlying);
        assertFalse(ss.add("a"), "Adding a duplicate should return false");
        assertEquals("[a]", ss.toString(), "Duplicate add must not alter the string representation.");
    }

    @Test
    void testTreeSetSortedOrder() {
        Set<String> underlying = new TreeSet<>();
        underlying.add("b");
        underlying.add("a");
        underlying.add("c");
        SealableSet<String> ss = wrapWith(underlying);
        assertEquals("[a, b, c]", ss.toString(), "TreeSet should produce a naturally sorted representation.");
    }

    @Test
    void testWrappedSetToStringReturnsNull() {
        Set<Integer> nullToStringSet = new AbstractSet<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public String toString() {
                return null;
            }
        };
        SealableSet<Integer> ss = wrapWith(nullToStringSet);
        assertNull(ss.toString(), "If the wrapped Set's toString() returns null, the SealableSet should also return null.");
    }

    @Test
    void testWrappedSetToStringThrowsException() {
        Set<Integer> explodingSet = new AbstractSet<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        };
        SealableSet<Integer> ss = wrapWith(explodingSet);
        IllegalStateException ex = assertThrows(IllegalStateException.class, ss::toString, "Exception from wrapped Set should propagate unchanged.");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testNullUnderlyingSetThrowsNPE() throws Exception {
        // Create a SealableSet normally, then replace its internal set with null via reflection.
        SealableSet<Integer> ss = new SealableSet<>(sealedSupplier);
        Field f = SealableSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(ss, null);
        assertThrows(NullPointerException.class, ss::toString, "Calling toString() when the underlying set is null must throw NullPointerException.");
    }
}
