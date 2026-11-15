package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_contains_Test {

    /**
     * Two distinct objects that deliberately share the same hashCode.
     */
    private static final class CollidingKey {

        private final int id;

        CollidingKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            // constant hash to force a collision
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CollidingKey) && ((CollidingKey) o).id == this.id;
        }

        @Override
        public String toString() {
            return "CollidingKey{id=" + id + '}';
        }
    }

    /**
     * Mutable key whose hashCode / equals depend on an internal field.
     */
    private static final class MutableKey {

        private String value;

        MutableKey(String value) {
            this.value = value;
        }

        void setValue(String newValue) {
            this.value = newValue;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof MutableKey) && Objects.equals(((MutableKey) o).value, this.value);
        }

        @Override
        public String toString() {
            return "MutableKey{value='" + value + "'}";
        }
    }

    /**
     * Equals implementation deliberately throws an exception.
     */
    private static final class BadEqualsKey {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("Bad equals invoked");
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static Stream<Arguments> presentElements() {
        return Stream.of(Arguments.of("apple"), // used later for wrap‑behaviour test
        Arguments.of("wrapped"));
    }

    private static Stream<Arguments> absentElements() {
        return Stream.of(Arguments.of("banana"), Arguments.of("anyValue"));
    }

    /* ------------------------------------------------------------------------
     *  Basic positive/negative contains checks
     * ------------------------------------------------------------------------ */
    @Test
    void containsShouldReturnTrueForExistingElement() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(42);
        assertTrue(set.contains(42), "Set must contain the element that was added");
    }

    @Test
    void containsShouldReturnFalseForMissingElement() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("alpha", "beta"));
        assertFalse(set.contains("gamma"), "Set must not contain an element that was never added");
    }

    /* ------------------------------------------------------------------------
     *  Null handling – the implementation uses a sentinel, but the contract
     *  of Set permits null values.  Verify that {@code contains(null)} works
     *  correctly both when null is present and when it is absent.
     * ------------------------------------------------------------------------ */
    @Test
    void containsShouldDetectNullWhenAdded() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add(null);
        assertTrue(set.contains(null), "Set must report that it contains null after null was added");
    }

    @Test
    void containsShouldReturnFalseForNullWhenNotAdded() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b"));
        assertFalse(set.contains(null), "Set must not report containing null when null was never added");
    }

    /* ------------------------------------------------------------------------
     *  Interaction with other Set constructors
     * ------------------------------------------------------------------------ */
    @Test
    void containsWorksWithCollectionConstructor() {
        Set<String> source = new HashSet<>(Arrays.asList("x", "y", null));
        ConcurrentSet<String> set = new ConcurrentSet<>(source);
        assertTrue(set.contains("x"));
        assertTrue(set.contains("y"));
        assertTrue(set.contains(null));
        assertFalse(set.contains("z"));
    }

    @Test
    void containsWorksWithSetConstructor() {
        Set<Integer> source = new HashSet<>(Arrays.asList(1, 2, 3));
        ConcurrentSet<Integer> set = new ConcurrentSet<>(source);
        assertTrue(set.containsAll(source));
        assertFalse(set.contains(99));
    }

    /* ------------------------------------------------------------------------
     *  contains should be consistent after bulk operations (addAll / removeAll)
     * ------------------------------------------------------------------------ */
    @Test
    void containsReflectsStateAfterAddAllAndRemoveAll() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(10, 20, 30));
        assertTrue(set.containsAll(Arrays.asList(10, 20, 30)));
        set.removeAll(Collections.singleton(20));
        assertTrue(set.contains(10));
        assertFalse(set.contains(20));
        assertTrue(set.contains(30));
    }

    /* ------------------------------------------------------------------------
     *  Simple concurrency sanity check – multiple threads add elements,
     *  then we verify that {@code contains} sees at least some of them.
     *  This does not aim to prove thread‑safety, only that the method does
     *  not throw or return incorrect results under concurrent mutation.
     * ------------------------------------------------------------------------ */
    @Test
    void containsDuringConcurrentModification() throws InterruptedException {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1_000; i++) {
                set.add(i);
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 500; i < 1_500; i++) {
                set.add(i);
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        // Elements 0‑1499 should be present, but we only assert a representative sample
        assertTrue(set.contains(0), "Set should contain element added by first thread");
        assertTrue(set.contains(1_000), "Set should contain element added by second thread");
        assertTrue(set.contains(1_499), "Set should contain the last element added by second thread");
        // Verify a missing element is indeed missing
        assertFalse(set.contains(2_000), "Set must not contain an element that was never added");
    }

    @ParameterizedTest(name = "contains should return true when element \"{0}\" is present")
    @MethodSource("presentElements")
    void containsReturnsTrueWhenElementPresent(String element) {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add(element);
        assertTrue(set.contains(element), "Expected contains to return true for element that was added");
    }

    @ParameterizedTest(name = "contains should return false when element \"{0}\" is absent")
    @MethodSource("absentElements")
    void containsReturnsFalseWhenElementAbsent(String element) {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // deliberately leave the set empty
        assertFalse(set.contains(element), "Expected contains to return false for element that was never added");
    }

    @Test
    void containsNullWhenAdded() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add(null);
        assertTrue(set.contains(null), "Set must report that it contains null after null was added");
    }

    @Test
    void containsNullWhenNotAdded() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b"));
        assertFalse(set.contains(null), "Set must not report containing null when null was never added");
    }

    @Test
    void containsWrongTypeThrowsClassCastException() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("hello");
        assertThrows(ClassCastException.class, () -> set.contains(123), "Passing an element of the wrong generic type should throw ClassCastException");
    }

    @Test
    void containsWorksWhenUnderlyingSetHasWrappedValue() throws Exception {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // Access the private backing set
        Field setField = ConcurrentSet.class.getDeclaredField("set");
        setField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Object> backing = (Set<Object>) setField.get(set);
        // Obtain the private wrap method
        Method wrapMethod = ConcurrentSet.class.getDeclaredMethod("wrap", Object.class);
        wrapMethod.setAccessible(true);
        Object wrapped = wrapMethod.invoke(set, "wrapped");
        // Directly insert the wrapped representation
        backing.add(wrapped);
        // Now contains should succeed even though we never called add()
        assertTrue(set.contains("wrapped"), "contains should delegate to the backing set with the same wrap transformation");
    }

    @Test
    void containsAfterRemovalReturnsFalse() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("temp");
        assertTrue(set.contains("temp"));
        set.remove("temp");
        assertFalse(set.contains("temp"), "Element removed from the set must not be reported as present");
    }

    @Test
    void containsAfterClearAndReadd() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("first");
        set.add("second");
        set.clear();
        set.add("readded");
        assertTrue(set.contains("readded"));
        assertFalse(set.contains("first"));
    }

    @Test
    void containsHashCollisionButNotEqual() {
        ConcurrentSet<CollidingKey> set = new ConcurrentSet<>();
        CollidingKey key1 = new CollidingKey(1);
        // same hash, different id
        CollidingKey key2 = new CollidingKey(2);
        set.add(key1);
        assertFalse(set.contains(key2), "Different object with same hash must not be considered present");
    }

    @Test
    void containsSubclassInstanceMatches() {
        ConcurrentSet<Number> set = new ConcurrentSet<>();
        Integer intVal = 42;
        set.add(intVal);
        // same concrete class
        Number query = Integer.valueOf(42);
        assertTrue(set.contains(query), "Integer instance added should be found via Number reference");
    }

    @Test
    void containsSubclassInstanceMismatchedType() {
        ConcurrentSet<Number> set = new ConcurrentSet<>();
        set.add(Integer.valueOf(42));
        Number query = Long.valueOf(42L);
        assertFalse(set.contains(query), "Long instance should not match stored Integer even if numeric value equals");
    }

    @Test
    void containsMutableKeyStateChange() {
        ConcurrentSet<MutableKey> set = new ConcurrentSet<>();
        MutableKey key = new MutableKey("key1");
        set.add(key);
        // Mutate after insertion
        key.setValue("changed");
        assertFalse(set.contains(new MutableKey("key1")), "After the stored key mutates, contains should not find the original logical value");
    }

    @Test
    void containsBadEqualsKeyPropagatesException() {
        ConcurrentSet<BadEqualsKey> set = new ConcurrentSet<>();
        BadEqualsKey bad = new BadEqualsKey();
        set.add(bad);
        BadEqualsKey query = new BadEqualsKey();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> set.contains(query), "Exception thrown from equals() must propagate");
        assertEquals("Bad equals invoked", ex.getMessage());
    }

    @Test
    void containsConcurrentModification() throws InterruptedException {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        Thread adder = new Thread(() -> set.add("race"));
        adder.start();
        // ensure the element is added before we query
        adder.join();
        assertTrue(set.contains("race"), "contains must see element added by another thread");
    }

    @Test
    void containsWrapThrowsIllegalArgumentException() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // According to the library contract, the sentinel "BAD_WRAP" triggers an
        // IllegalArgumentException from the private wrap method.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> set.contains("BAD_WRAP"), "wrap should reject the sentinel value and propagate IllegalArgumentException");
        // Message is implementation‑specific; we only assert the type.
        assertNotNull(ex.getMessage());
    }

    @Test
    void containsWithNullBackingSetThrowsNPE() throws Exception {
        // Obtain the (presumably) protected constructor that accepts a Set<Object>
        Constructor<ConcurrentSet> ctor = ConcurrentSet.class.getDeclaredConstructor(Set.class);
        ctor.setAccessible(true);
        ConcurrentSet<Object> broken = ctor.newInstance((Set<Object>) null);
        assertThrows(NullPointerException.class, () -> broken.contains("any"), "Calling contains on a ConcurrentSet with a null backing set must throw NPE");
    }

    @Test
    void containsOnEmptySetAlwaysFalse() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertFalse(set.contains("anyValue"), "Empty set must return false for any contains query");
    }
}
