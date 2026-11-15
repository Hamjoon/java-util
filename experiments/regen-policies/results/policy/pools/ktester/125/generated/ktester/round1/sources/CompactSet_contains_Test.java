package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CompactSet_contains_Test {

    /**
     * A case‑sensitive CompactSet used in several tests.
     */
    private CompactSet<String> caseSensitiveSet;

    /**
     * A case‑insensitive CompactSet built via the builder.
     */
    private CompactSet<String> caseInsensitiveSet;

    /**
     * Simple immutable point used for equality testing.
     */
    private static final class Point {

        private final int x;

        private final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Point))
                return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * A key whose {@code hashCode()} is constant to force collisions.
     * Equality depends on the {@code id} field.
     */
    private static final class CollisionKey {

        private final int id;

        CollisionKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CollisionKey))
                return false;
            return id == ((CollisionKey) o).id;
        }

        @Override
        public int hashCode() {
            // Constant hash to simulate collision.
            return 42;
        }
    }

    /**
     * A mutable key where {@code equals}/ {@code hashCode} depend on the mutable field.
     * Changing the field after insertion should break lookup.
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
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MutableKey))
                return false;
            return Objects.equals(value, ((MutableKey) o).value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test to avoid cross‑test contamination.
        caseSensitiveSet = new CompactSet<>();
        caseInsensitiveSet = CompactSet.<String>builder().caseSensitive(// make it case‑insensitive
        false).build();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the sets – not strictly required because a new instance
        // is created for each test, but it demonstrates proper cleanup practice.
        caseSensitiveSet.clear();
        caseInsensitiveSet.clear();
    }

    static Stream<Arguments> emptySetProvider() {
        return Stream.of(arguments("anyValue", false), arguments(null, false));
    }

    static Stream<Arguments> singleElementProvider() {
        return Stream.of(arguments("test", true), arguments("different", false));
    }

    static Stream<Arguments> nullElementProvider() {
        return Stream.of(arguments(null, true), arguments("notNull", false));
    }

    @Test
    void containsOnEmptySetReturnsFalse() {
        assertTrue(caseSensitiveSet.isEmpty(), "Set should start empty");
        assertFalse(caseSensitiveSet.contains(null), "Empty set must not contain null");
        assertFalse(caseSensitiveSet.contains("anything"), "Empty set must not contain any element");
    }

    @Test
    void containsAfterAddingElements() {
        caseSensitiveSet.add("foo");
        caseSensitiveSet.add("bar");
        assertTrue(caseSensitiveSet.contains("foo"), "Set should contain previously added element 'foo'");
        assertTrue(caseSensitiveSet.contains("bar"), "Set should contain previously added element 'bar'");
        assertFalse(caseSensitiveSet.contains("baz"), "Set should not contain element that was never added");
        assertFalse(caseSensitiveSet.contains(null), "Set should not contain null unless explicitly added");
    }

    @Test
    void containsRespectsCaseSensitivity() {
        caseSensitiveSet.add("Hello");
        assertTrue(caseSensitiveSet.contains("Hello"));
        assertFalse(caseSensitiveSet.contains("hello"), "Case‑sensitive set must distinguish case");
    }

    @Test
    void containsIgnoresCaseWhenConfigured() {
        caseInsensitiveSet.add("Hello");
        // The builder configured the set to be case‑insensitive, so any case variation should match.
        assertTrue(caseInsensitiveSet.contains("hello"));
        assertTrue(caseInsensitiveSet.contains("HELLO"));
        assertTrue(caseInsensitiveSet.contains("HeLLo"));
    }

    @Test
    void containsWorksWithDifferentObjectTypes() {
        CompactSet<Integer> intSet = new CompactSet<>();
        intSet.add(42);
        intSet.add(7);
        assertTrue(intSet.contains(42));
        assertTrue(intSet.contains(7));
        assertFalse(intSet.contains(0));
        // different type – should be false
        assertFalse(intSet.contains("42"));
    }

    @Test
    void containsDoesNotThrowWhenNullIsAddedExplicitly() {
        // CompactSet permits adding null (the underlying map does not allow null keys,
        // but the Set contract requires contains(null) to be well‑defined).
        // The current implementation forwards to map.containsKey(null) which returns false.
        // This test documents the observed behaviour.
        caseSensitiveSet.add(null);
        assertTrue(caseSensitiveSet.contains(null), "After adding null, contains(null) must be true");
        caseSensitiveSet.remove(null);
        assertFalse(caseSensitiveSet.contains(null), "After removal, contains(null) must be false");
    }

    /**
     * 1. Empty set behavior **
     */
    @ParameterizedTest(name = "[{index}] Empty set contains {0} → {1}")
    @MethodSource("emptySetProvider")
    void emptySetBehavior(Object query, boolean expected) {
        assertFalse(caseSensitiveSet.contains(query), "Empty set should return false for any query");
    }

    /**
     * 2. Single element present **
     */
    @ParameterizedTest(name = "[{index}] After adding \"test\", contains {0} → {1}")
    @MethodSource("singleElementProvider")
    void singleElementPresent(Object query, boolean expected) {
        caseSensitiveSet.add("test");
        assertEquals(expected, caseSensitiveSet.contains(query));
    }

    /**
     * 3. Null element handling **
     */
    @ParameterizedTest(name = "[{index}] Null handling query {0} → {1}")
    @MethodSource("nullElementProvider")
    void nullElementHandling(Object query, boolean expected) {
        caseSensitiveSet.add(null);
        assertEquals(expected, caseSensitiveSet.contains(query));
    }

    /**
     * 4. Multiple heterogeneous elements **
     */
    @Test
    void multipleHeterogeneousElements() {
        CompactSet<Object> mixedSet = new CompactSet<>();
        Integer intVal = 42;
        String strVal = "hello";
        Point point = new Point(5, 10);
        mixedSet.add(intVal);
        mixedSet.add(strVal);
        mixedSet.add(point);
        assertTrue(mixedSet.contains(42));
        assertTrue(mixedSet.contains("hello"));
        assertTrue(mixedSet.contains(new Point(5, 10)));
        assertFalse(mixedSet.contains(100L));
    }

    /**
     * 5. Equality semantics (case sensitivity) **
     */
    @Test
    void equalitySemanticsCaseSensitivity() {
        caseSensitiveSet.add("CaseSensitive");
        // Different case – should be false
        assertFalse(caseSensitiveSet.contains("caseSensitive"));
        // Exact case – should be true
        assertTrue(caseSensitiveSet.contains("CaseSensitive"));
    }

    /**
     * 6. Removal behavior **
     */
    @Test
    void removalBehavior() {
        caseSensitiveSet.add("alpha");
        caseSensitiveSet.add("beta");
        assertTrue(caseSensitiveSet.remove("alpha"));
        assertFalse(caseSensitiveSet.contains("alpha"));
        assertTrue(caseSensitiveSet.contains("beta"));
    }

    /**
     * 7. Duplicate addition **
     */
    @Test
    void duplicateAddition() {
        caseSensitiveSet.add("dup");
        // duplicate
        caseSensitiveSet.add("dup");
        assertTrue(caseSensitiveSet.contains("dup"));
        assertEquals(1, caseSensitiveSet.size(), "Duplicate addition must not increase size");
    }

    /**
     * 8. Equality with distinct instance **
     */
    @Test
    void equalityWithDistinctStringInstance() {
        // literal
        caseSensitiveSet.add("sample");
        assertTrue(caseSensitiveSet.contains(new String("sample")), "Contains must rely on equals, not on reference identity");
    }

    /**
     * 9. Custom object equality (Point) **
     */
    @Test
    void customObjectEquality() {
        CompactSet<Point> pointSet = new CompactSet<>();
        Point stored = new Point(5, 10);
        pointSet.add(stored);
        // Same coordinates, different instance
        assertTrue(pointSet.contains(new Point(5, 10)));
        // Different coordinates
        assertFalse(pointSet.contains(new Point(5, 11)));
    }

    /**
     * 10. Hash collision handling **
     */
    @Test
    void hashCollisionHandling() {
        CompactSet<CollisionKey> set = new CompactSet<>();
        CollisionKey key1 = new CollisionKey(1);
        CollisionKey key2 = new CollisionKey(2);
        set.add(key1);
        assertTrue(set.contains(new CollisionKey(1)), "Exact key must be found despite collisions");
        assertFalse(set.contains(key2), "Different logical key with same hash must not be found");
    }

    /**
     * 11. Mutable key after change **
     */
    @Test
    void mutableKeyAfterChange() {
        CompactSet<MutableKey> set = new CompactSet<>();
        MutableKey mutable = new MutableKey("original");
        set.add(mutable);
        // Mutate after insertion
        mutable.setValue("changed");
        assertFalse(set.contains(mutable), "Mutable key whose hash/equals changed must not be found");
    }

    /**
     * 12. Post‑clear behavior **
     */
    @Test
    void postClearBehavior() {
        caseSensitiveSet.add("stillThere");
        caseSensitiveSet.add("another");
        caseSensitiveSet.clear();
        assertFalse(caseSensitiveSet.contains("stillThere"));
        assertFalse(caseSensitiveSet.contains("another"));
    }
}
