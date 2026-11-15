package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

// kept to satisfy the provided template; not used here
@ExtendWith(MockitoExtension.class)
class ConcurrentSet_add_Test {

    /**
     * Instance under test – created fresh for each test method.
     */
    private ConcurrentSet<Integer> intSet;

    /**
     * Instance under test for {@code String} values – needed for null‑handling tests.
     */
    private ConcurrentSet<String> stringSet;

    /**
     * Simple class with constant hashCode but equality based on an identifier.
     */
    private static final class CollidingKey {

        private final int hash;

        private final String id;

        CollidingKey(int hash, String id) {
            this.hash = hash;
            this.id = id;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CollidingKey))
                return false;
            CollidingKey that = (CollidingKey) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public String toString() {
            return "CollidingKey{id='" + id + "'}";
        }
    }

    /**
     * Class whose equals method returns true when compared to a String with the same content.
     */
    private static final class WeirdEquals {

        private final String value;

        WeirdEquals(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof String) {
                return value.equals(o);
            }
            if (o instanceof WeirdEquals) {
                return value.equals(((WeirdEquals) o).value);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return "WeirdEquals{" + value + '}';
        }
    }

    /**
     * Element whose hashCode method throws a RuntimeException.
     */
    private static final class BadHashCode {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad hash");
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    /**
     * Element whose equals method throws a RuntimeException.
     */
    private static final class BadEquals {

        private final String value;

        BadEquals(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("bad equals");
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

    /**
     * Dummy type that the ConcurrentSet.wrap implementation is expected to reject.
     */
    private static final class UnsupportedType {
        // no special behavior
    }

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references; helps the GC when many tests run.
        intSet = null;
        stringSet = null;
    }

    @Test
    @DisplayName("add returns true for a brand‑new element and makes it visible")
    void addNewElement() {
        // given
        Integer element = 42;
        // when
        boolean added = intSet.add(element);
        // then
        assertTrue(added, "add should return true for a new element");
        assertTrue(intSet.contains(element), "set must contain the newly added element");
        assertEquals(1, intSet.size(), "size should be 1 after adding one element");
    }

    @Test
    @DisplayName("add returns false for a duplicate element and does not change the set")
    void addDuplicateElement() {
        // given
        Integer element = 7;
        assertTrue(intSet.add(element), "pre‑condition: first add must succeed");
        // when
        boolean addedAgain = intSet.add(element);
        // then
        assertFalse(addedAgain, "add should return false when the element is already present");
        assertEquals(1, intSet.size(), "size must stay unchanged after attempting to add a duplicate");
    }

    @Test
    @DisplayName("add handles null values correctly")
    void addNullElement() {
        // given
        // (stringSet is already instantiated)
        // when
        boolean added = stringSet.add(null);
        // then
        assertTrue(added, "add should return true when inserting null for the first time");
        assertTrue(stringSet.contains(null), "set must report containing null");
        assertEquals(1, stringSet.size(), "size should be 1 after adding null");
    }

    @Test
    @DisplayName("adding null twice returns false on the second attempt")
    void addNullDuplicate() {
        // given
        assertTrue(stringSet.add(null), "first null insertion must succeed");
        // when
        boolean addedAgain = stringSet.add(null);
        // then
        assertFalse(addedAgain, "second insertion of null must return false");
        assertEquals(1, stringSet.size(), "size must stay at 1 after duplicate null add");
    }

    @Test
    @DisplayName("add works correctly after a bulk addAll")
    void addAfterAddAll() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean added = intSet.add(4);
        boolean duplicate = intSet.add(2);
        // then
        assertTrue(added, "add should succeed for a new element after addAll");
        assertFalse(duplicate, "add should fail for a duplicate element after addAll");
        assertTrue(intSet.containsAll(Arrays.asList(1, 2, 3, 4)), "set must contain the original and newly added elements");
        assertEquals(4, intSet.size(), "size must reflect the total unique elements");
    }

    @Test
    @DisplayName("concurrent add operations do not corrupt the set")
    void concurrentAdd() throws InterruptedException {
        // given
        final int threadCount = 8;
        final int addsPerThread = 5_000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    // ensure all threads start at the same time
                    startLatch.await();
                    for (int i = 0; i < addsPerThread; i++) {
                        // each thread adds a distinct range to increase the chance of collisions
                        intSet.add(threadId * addsPerThread + i);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        // when
        // let all threads run
        startLatch.countDown();
        // wait for completion
        doneLatch.await();
        // then
        int expectedMaximum = threadCount * addsPerThread;
        assertTrue(intSet.size() <= expectedMaximum, "size cannot exceed the total number of distinct values attempted");
        // Every value that was added at least once must be present.
        for (int i = 0; i < expectedMaximum; i++) {
            assertTrue(intSet.contains(i), "set should contain value " + i);
        }
    }

    @Test
    @DisplayName("addAll correctly delegates to add for each element, including nulls and duplicates")
    void addAllDelegatesToAdd() {
        // given
        // The collection contains a duplicate (2) and a null.
        var source = Arrays.asList("x", null, "y", "x", "z", null, "y", "2", "2");
        // when
        boolean modified = stringSet.addAll(source);
        // then
        assertTrue(modified, "addAll should report modification when at least one new element appears");
        // Expected distinct elements: "x", null, "y", "z", "2"
        assertEquals(5, stringSet.size(), "size must reflect distinct elements only");
        assertTrue(stringSet.containsAll(Arrays.asList("x", "y", "z", "2", null)), "set must contain all distinct elements from the source collection");
    }

    @Test
    @DisplayName("add a new non‑null element (String) – expect true")
    void addStringAlphaFirst() {
        assertTrue(stringSet.add("alpha"));
        assertTrue(stringSet.contains("alpha"));
        assertEquals(1, stringSet.size());
    }

    @Test
    @DisplayName("add duplicate string element – expect false")
    void addStringAlphaDuplicate() {
        stringSet.add("alpha");
        assertFalse(stringSet.add("alpha"));
        assertEquals(1, stringSet.size());
    }

    @Test
    @DisplayName("add a new StringBuilder (mutable) – expect true")
    void addStringBuilderBetaFirst() {
        StringBuilder sb = new StringBuilder("beta");
        assertTrue(stringSet.add(sb));
        assertTrue(stringSet.contains(sb));
        assertEquals(1, stringSet.size());
    }

    @Test
    @DisplayName("add another StringBuilder with same content – expect true (distinct objects)")
    void addStringBuilderBetaSecond() {
        StringBuilder sb1 = new StringBuilder("beta");
        StringBuilder sb2 = new StringBuilder("beta");
        assertTrue(stringSet.add(sb1));
        // sb2 is a different instance; StringBuilder does not override equals, so should be treated as distinct
        assertTrue(stringSet.add(sb2));
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.contains(sb1));
        assertTrue(stringSet.contains(sb2));
    }

    @Test
    @DisplayName("add element after it has been removed – first add true")
    void addAfterRemovalFirst() {
        assertTrue(stringSet.add("gamma"));
        assertTrue(stringSet.remove("gamma"));
        assertTrue(stringSet.add("gamma"));
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("gamma"));
    }

    @Test
    @DisplayName("add same element again without removal – second add false")
    void addAfterRemovalDuplicate() {
        stringSet.add("gamma");
        assertFalse(stringSet.add("gamma"));
        assertEquals(1, stringSet.size());
    }

    @Test
    @DisplayName("add after clear – first add true")
    void addAfterClearFirst() {
        intSet.add(42);
        intSet.clear();
        assertTrue(intSet.add(42));
        assertEquals(1, intSet.size());
        assertTrue(intSet.contains(42));
    }

    @Test
    @DisplayName("add after clear – duplicate add false")
    void addAfterClearDuplicate() {
        intSet.add(42);
        intSet.clear();
        intSet.add(42);
        assertFalse(intSet.add(42));
        assertEquals(1, intSet.size());
    }

    @Test
    @DisplayName("add CollidingKey with same hash but different id – both true")
    void collidingKeyDifferentIds() {
        CollidingKey a = new CollidingKey(1, "A");
        CollidingKey b = new CollidingKey(1, "B");
        assertTrue(intSet.add(a));
        assertTrue(intSet.add(b));
        assertEquals(2, intSet.size());
        assertTrue(intSet.contains(a));
        assertTrue(intSet.contains(b));
    }

    @Test
    @DisplayName("add CollidingKey duplicate (same id) – second add false")
    void collidingKeyDuplicate() {
        CollidingKey a1 = new CollidingKey(1, "A");
        CollidingKey a2 = new CollidingKey(1, "A");
        assertTrue(intSet.add(a1));
        assertFalse(intSet.add(a2));
        assertEquals(1, intSet.size());
    }

    @Test
    @DisplayName("add plain String then WeirdEquals equal to it – second add false")
    void crossTypeEqualityDuplicate() {
        stringSet.add("delta");
        WeirdEquals we = new WeirdEquals("delta");
        assertFalse(stringSet.add(we));
        assertEquals(1, stringSet.size());
    }

    @Test
    @DisplayName("add WeirdEquals with unique content – true")
    void crossTypeEqualityUnique() {
        WeirdEquals we = new WeirdEquals("epsilon");
        assertTrue(stringSet.add(we));
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains(we));
    }

    @Test
    @DisplayName("add element whose hashCode throws – expect RuntimeException")
    void badHashCodeThrows() {
        BadHashCode bad = new BadHashCode();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> intSet.add(bad), "hashCode exception should propagate");
        assertEquals("bad hash", ex.getMessage());
        // Ensure the set remains empty after the failed insertion
        assertEquals(0, intSet.size());
    }

    @Test
    @DisplayName("add after a good element, then BadHashCode – exception does not affect existing")
    void badHashCodeAfterGoodElement() {
        assertTrue(intSet.add(123));
        BadHashCode bad = new BadHashCode();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> intSet.add(bad));
        assertEquals("bad hash", ex.getMessage());
        assertEquals(1, intSet.size());
        assertTrue(intSet.contains(123));
    }

    @Test
    @DisplayName("add BadEquals that throws during equals – expect RuntimeException")
    void badEqualsThrows() {
        // baseline entry
        stringSet.add("theta");
        BadEquals bad = new BadEquals("theta");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> stringSet.add(bad), "equals exception should propagate");
        assertEquals("bad equals", ex.getMessage());
        // Set should still contain only the original element
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("theta"));
    }

    @Test
    @DisplayName("add UnsupportedType – expect IllegalArgumentException")
    void unsupportedTypeThrows() {
        UnsupportedType ut = new UnsupportedType();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, // force compile‑time generic cast
        () -> intSet.add((Integer) (Object) ut), "wrap should reject UnsupportedType");
        assertTrue(ex.getMessage().contains("Unsupported type"));
    }
}
