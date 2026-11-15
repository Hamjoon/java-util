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
class SealableSet_hashCode_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    class CustomObj {

        private final int fixedHash;

        CustomObj(int h) {
            this.fixedHash = h;
        }

        @Override
        public int hashCode() {
            return fixedHash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomObj && ((CustomObj) o).fixedHash == fixedHash;
        }
    }

    class CustomObj {

        private final int fixedHash;

        CustomObj(int h) {
            this.fixedHash = h;
        }

        @Override
        public int hashCode() {
            return fixedHash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomObj && ((CustomObj) o).fixedHash == fixedHash;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor creates a HashSet internally
        set = new SealableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(null);
    }

    private static Stream<Arguments> delegationScenarios() throws Exception {
        // empty set
        SealableSet<String> empty = new SealableSet<>(() -> false);
        setPrivateBackingSet(empty, new HashSet<>());
        // three distinct strings
        SealableSet<String> three = new SealableSet<>(() -> false);
        Set<String> threeSet = new HashSet<>(Arrays.asList("a", "b", "c"));
        setPrivateBackingSet(three, threeSet);
        // duplicate adds (HashSet will ignore duplicates)
        SealableSet<String> dup = new SealableSet<>(() -> false);
        Set<String> dupSet = new HashSet<>();
        dupSet.add("x");
        // duplicate
        dupSet.add("x");
        setPrivateBackingSet(dup, dupSet);
        // set containing null
        SealableSet<String> withNull = new SealableSet<>(() -> false);
        Set<String> nullSet = new HashSet<>(Collections.singletonList(null));
        setPrivateBackingSet(withNull, nullSet);
        // mutable set after add/remove
        SealableSet<String> mutable = new SealableSet<>(() -> false);
        Set<String> mutableSet = new HashSet<>(Arrays.asList("foo", "bar"));
        // already present
        mutableSet.add("foo");
        mutableSet.remove("bar");
        setPrivateBackingSet(mutable, mutableSet);
        return Stream.of(Arguments.of("Empty HashSet delegation", Collections.emptySet(), empty), Arguments.of("Three distinct strings delegation", threeSet, three), Arguments.of("Duplicate element delegation", dupSet, dup), Arguments.of("Null element delegation", nullSet, withNull), Arguments.of("Mutable after add/remove delegation", mutableSet, mutable));
    }

    /* --------------------------------------------------------------
     *  Helper to inject a backing set via reflection
     * -------------------------------------------------------------- */
    private static <T> void setPrivateBackingSet(SealableSet<T> sealable, Set<T> backing) throws Exception {
        Field f = SealableSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(sealable, backing);
    }

    @Test
    void testHashCodeMatchesUnderlyingSet() {
        // Build a plain Set with the same contents
        Set<Integer> reference = new HashSet<>();
        reference.add(10);
        reference.add(20);
        reference.add(null);
        assertEquals(reference.hashCode(), set.hashCode(), "SealableSet.hashCode() should delegate to the wrapped set's hashCode");
    }

    @Test
    void testHashCodeChangesWhenSetMutates() {
        int originalHash = set.hashCode();
        // Mutate the set – hash code should change
        set.add(30);
        int newHash = set.hashCode();
        assertNotEquals(originalHash, newHash, "Hash code must change after the set's contents change");
    }

    @Test
    void testHashCodeWhenSealedDoesNotThrow() {
        // make the set read‑only
        sealed = true;
        // hashCode should still be callable and return the same value as before
        int hashWhenSealed = set.hashCode();
        assertEquals(set.hashCode(), hashWhenSealed, "Calling hashCode on a sealed set should not throw and should be consistent");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("delegationScenarios")
    void testHashCodeDelegation(String description, Set<?> underlying, SealableSet<?> sealable) {
        assertEquals(underlying.hashCode(), sealable.hashCode(), "hashCode should be delegated and equal to the underlying set's hashCode");
    }

    /* --------------------------------------------------------------
     *  TreeSet delegation test
     * -------------------------------------------------------------- */
    @Test
    void testHashCodeDelegationWithTreeSet() throws Exception {
        SealableSet<Integer> treeSealable = new SealableSet<>(() -> false);
        Set<Integer> treeSet = new TreeSet<>(Arrays.asList(1, 2, 3));
        setPrivateBackingSet(treeSealable, treeSet);
        assertEquals(treeSet.hashCode(), treeSealable.hashCode(), "hashCode should delegate correctly when underlying set is a TreeSet");
    }

    /* --------------------------------------------------------------
     *  After clear()
     * -------------------------------------------------------------- */
    @Test
    void testHashCodeAfterClear() throws Exception {
        SealableSet<String> clearSet = new SealableSet<>(() -> false);
        Set<String> backing = new HashSet<>(Arrays.asList("a", "b", "c"));
        setPrivateBackingSet(clearSet, backing);
        clearSet.clear();
        assertEquals(0, clearSet.hashCode(), "After clear() the underlying set is empty, hashCode must be 0");
    }

    /* --------------------------------------------------------------
     *  After addAll()
     * -------------------------------------------------------------- */
    @Test
    void testHashCodeAfterAddAll() throws Exception {
        SealableSet<String> addAllSet = new SealableSet<>(() -> false);
        Set<String> backing = new HashSet<>();
        setPrivateBackingSet(addAllSet, backing);
        addAllSet.addAll(Arrays.asList("x", "y", "z"));
        assertEquals(backing.hashCode(), addAllSet.hashCode(), "hashCode must reflect contents after addAll()");
    }

    /* --------------------------------------------------------------
     *  After retainAll()
     * -------------------------------------------------------------- */
    @Test
    void testHashCodeAfterRetainAll() throws Exception {
        SealableSet<String> retainSet = new SealableSet<>(() -> false);
        Set<String> backing = new HashSet<>(Arrays.asList("keep", "discard"));
        setPrivateBackingSet(retainSet, backing);
        retainSet.retainAll(Collections.singleton("keep"));
        assertEquals(backing.hashCode(), retainSet.hashCode(), "hashCode must reflect contents after retainAll()");
    }

    /* --------------------------------------------------------------
     *  After removeAll()
     * -------------------------------------------------------------- */
    @Test
    void testHashCodeAfterRemoveAll() throws Exception {
        SealableSet<String> removeSet = new SealableSet<>(() -> false);
        Set<String> backing = new HashSet<>(Arrays.asList("a", "b", "c"));
        setPrivateBackingSet(removeSet, backing);
        removeSet.removeAll(Arrays.asList("a", "b"));
        assertEquals(backing.hashCode(), removeSet.hashCode(), "hashCode must reflect contents after removeAll()");
    }

    /* --------------------------------------------------------------
     *  Custom element hashCode
     * -------------------------------------------------------------- */
    @Test
    void testHashCodeWithCustomElementHashCode() throws Exception {
        class CustomObj {

            private final int fixedHash;

            CustomObj(int h) {
                this.fixedHash = h;
            }

            @Override
            public int hashCode() {
                return fixedHash;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof CustomObj && ((CustomObj) o).fixedHash == fixedHash;
            }
        }
        SealableSet<CustomObj> customSet = new SealableSet<>(() -> false);
        Set<CustomObj> backing = new HashSet<>();
        backing.add(new CustomObj(42));
        backing.add(new CustomObj(99));
        setPrivateBackingSet(customSet, backing);
        assertEquals(backing.hashCode(), customSet.hashCode(), "hashCode should incorporate custom element hashCodes");
    }

    /* --------------------------------------------------------------
     *  Null underlying set – expect NPE
     * -------------------------------------------------------------- */
    @Test
    void testHashCodeWhenUnderlyingSetIsNull() throws Exception {
        SealableSet<Integer> nullBacked = new SealableSet<>(() -> false);
        // Force the private 'set' field to null via reflection
        Field f = SealableSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(nullBacked, null);
        assertThrows(NullPointerException.class, nullBacked::hashCode, "Calling hashCode() when the underlying set is null should throw NPE");
    }
}
