package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_add_Test {

    /**
     * Set used by the tests – created fresh before each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor – natural ordering, null‑safe.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set to help GC and to make sure no state leaks.
        if (set != null) {
            set.clear();
        }
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> basicAddProvider() {
        return Stream.of(// first insert of a non‑null element
        Arguments.of("alpha", true, "Add a non‑null element for the first time"), // duplicate of the same element
        Arguments.of("alpha", false, "Add the same non‑null element a second time"), // first null insertion
        Arguments.of(null, true, "Add a null value – should succeed"), // duplicate null insertion
        Arguments.of(null, false, "Add null a second time – should return false"), // distinct non‑null elements
        Arguments.of("beta", true, "Add another distinct non‑null element"), Arguments.of("gamma", true, "Add a third distinct non‑null element"), // duplicate after several inserts
        Arguments.of("beta", false, "Re‑add an element that was added earlier"), // element greater than all existing (natural order)
        Arguments.of("delta", true, "Add an element greater than all existing elements"), // element less than all existing (natural order)
        Arguments.of("0", true, "Add an element less than all existing elements"));
    }

    @Test
    void addNonNullElements_FirstTimeReturnsTrue() {
        assertTrue(set.isEmpty(), "Set should start empty");
        assertTrue(set.add("apple"), "First addition of 'apple' must return true");
        assertTrue(set.add("banana"), "First addition of 'banana' must return true");
        assertTrue(set.add("cherry"), "First addition of 'cherry' must return true");
        assertEquals(3, set.size(), "Size should reflect three distinct elements");
        assertTrue(set.containsAll(Arrays.asList("apple", "banana", "cherry")));
    }

    @Test
    void addDuplicateElements_ReturnsFalse() {
        assertTrue(set.add("apple"));
        assertFalse(set.add("apple"), "Adding a duplicate element must return false");
        assertEquals(1, set.size(), "Size must not increase when adding a duplicate");
    }

    @Test
    void addNullElement_IsSupportedAndOrderedCorrectly() {
        assertTrue(set.add(null), "Adding null must succeed");
        assertTrue(set.add("apple"), "Adding a non‑null after null must succeed");
        // The natural ordering puts null (sentinel) at the end of the set.
        Iterator<String> it = set.iterator();
        assertEquals("apple", it.next(), "First element should be the non‑null value");
        assertEquals(null, it.next(), "Second element should be the null sentinel");
        assertFalse(it.hasNext(), "No more elements expected");
        assertEquals(2, set.size());
        assertTrue(set.contains(null), "Set must report containing null");
        assertTrue(set.contains("apple"));
    }

    @Test
    void addWithComparator_OrdersAccordingToComparator() {
        // Use a comparator that orders strings by length, null being the shortest.
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 0
        assertTrue(set.add(null));
        // length 6
        assertTrue(set.add("banana"));
        // Expected order: null, kiwi, apple, banana
        Iterator<String> it = set.iterator();
        assertEquals(null, it.next(), "Null (length 0) should be first");
        assertEquals("kiwi", it.next(), "Next should be the shortest non‑null string");
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
        assertEquals(4, set.size());
        assertTrue(set.containsAll(Arrays.asList(null, "kiwi", "apple", "banana")));
    }

    @Test
    void addFromCollectionConstructor_ElementsAreMaskedCorrectly() {
        Collection<String> source = Arrays.asList("alpha", null, "beta");
        set = new ConcurrentNavigableSetNullSafe<>(source);
        // The collection constructor delegates to addAll, which uses add(E) internally.
        assertEquals(3, set.size(), "All elements from the collection must be present");
        assertTrue(set.containsAll(source));
        // Verify ordering (natural order, null sentinel at the end)
        Iterator<String> it = set.iterator();
        assertEquals("alpha", it.next());
        assertEquals("beta", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void addConcurrentModifications_DoNotCorruptSet() throws InterruptedException {
        // This test exercises thread‑safety of add(E) without relying on private details.
        final int THREAD_COUNT = 10;
        final int PER_THREAD = 100;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int base = i * PER_THREAD;
            Thread t = new Thread(() -> {
                for (int j = 0; j < PER_THREAD; j++) {
                    set.add("val-" + (base + j));
                }
            });
            threads.add(t);
            t.start();
        }
        // Wait for all threads to finish.
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(THREAD_COUNT * PER_THREAD, set.size(), "All elements added concurrently must be present");
        // Spot‑check a few values.
        assertTrue(set.contains("val-0"));
        assertTrue(set.contains("val-" + (THREAD_COUNT * PER_THREAD - 1)));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicAddProvider")
    void addBasicFunctionality(String element, boolean expectedResult, String description) {
        // The first call for each distinct element must succeed, duplicates must fail.
        boolean result = set.add(element);
        assertEquals(expectedResult, result, description);
    }

    @Test
    void addBasicFunctionality_FinalDuplicateAlpha() {
        // After the series above, "alpha" is already present – adding again must be false.
        // ensure present
        set.add("alpha");
        assertFalse(set.add("alpha"), "After a series of inserts, duplicate 'alpha' must return false");
    }

    // -----------------------------------------------------------------
    @Test
    void addAfterRemoval() {
        assertTrue(set.add("zeta"), "Add a new element \"zeta\" – should succeed");
        assertFalse(set.add("zeta"), "Add the same element again – duplicate, should return false");
        assertTrue(set.remove("zeta"), "Remove the element so it can be added again");
        assertTrue(set.add("zeta"), "After removing \"zeta\", adding it again – must return true");
    }

    // -----------------------------------------------------------------
    @Test
    void addAfterClear() {
        assertTrue(set.add("theta"), "Add an element before clearing – should succeed");
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertTrue(set.add("theta"), "Add the same element after clear – must succeed");
    }

    // -----------------------------------------------------------------
    @Test
    void addDistinctObjectsEqualValue() {
        // distinct instance
        String first = new String("omega");
        assertTrue(set.add(first), "Add a new String instance with value \"omega\" – should succeed");
        assertFalse(set.add("omega"), "Add a literal \"omega\" – duplicate according to compareTo, should return false");
    }

    // -----------------------------------------------------------------
    @Test
    void addWithReverseComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        set = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertTrue(set.add("apple"), "Adding \"apple\" with reverse comparator – should succeed");
        assertTrue(set.add("banana"), "Adding \"banana\" (smaller in reverse order) – should succeed");
        assertFalse(set.add("apple"), "Adding duplicate \"apple\" – must return false");
        // Verify ordering: reverse natural order => "banana" then "apple"
        Iterator<String> it = set.iterator();
        assertEquals("banana", it.next(), "First element should be \"banana\" (greater in reverse order)");
        assertEquals("apple", it.next(), "Second element should be \"apple\"");
        assertFalse(it.hasNext());
    }

    // -----------------------------------------------------------------
    @Test
    void nullAddRemoveAdd() {
        assertTrue(set.add(null), "Add null for the first time – must return true");
        assertFalse(set.add(null), "Add null again – sentinel already present, must return false");
        assertTrue(set.remove(null), "Remove null via remove(null) – should succeed");
        assertTrue(set.add(null), "Add null again after removal – must return true");
    }

    // -----------------------------------------------------------------
    @Test
    void addThroughSubSetView() {
        assertTrue(set.add("kappa"), "Add \"kappa\" directly to the main set – succeeds");
        // Create a view from "kappa" (inclusive) to "lambda" (exclusive)
        NavigableSet<String> view = set.subSet("kappa", true, "lambda", false);
        assertTrue(view.add("lambda"), "Add \"lambda\" through the subSet view – should affect the underlying set");
        assertFalse(view.add("kappa"), "Add \"kappa\" again via subSet view – duplicate, returns false");
        // Verify that both elements are present in the main set
        assertTrue(set.containsAll(Arrays.asList("kappa", "lambda")));
    }

    // -----------------------------------------------------------------
    @Test
    void addIncompatibleObject_throwsClassCastException() {
        // The set is typed to String, but we try to add a raw Object.
        assertThrows(ClassCastException.class, () -> {
            //noinspection unchecked
            ((NavigableSet) set).add(new Object());
        }, "Adding an Object to a String set must throw ClassCastException");
    }

    @Test
    void addIncompatibleThread_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> {
            //noinspection unchecked
            ((NavigableSet) set).add(new Thread());
        }, "Adding a Thread to a String set must throw ClassCastException");
    }

    @Test
    void addDate_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> {
            //noinspection unchecked
            ((NavigableSet) set).add(new Date(0L));
        }, "Adding a Date to a String set must throw ClassCastException");
    }

    @Test
    void addNullWithNonNullAcceptingComparator_throwsNullPointerException() {
        // Comparator that throws NPE when it receives a non‑String (the sentinel)
        Comparator<Object> badComparator = (a, b) -> {
            if (!(a instanceof String) || !(b instanceof String)) {
                throw new NullPointerException("Comparator does not accept sentinel");
            }
            return ((String) a).compareTo((String) b);
        };
        // Construct a set with the bad comparator
        NavigableSet<String> badSet = new ConcurrentNavigableSetNullSafe<>(badComparator);
        assertThrows(NullPointerException.class, () -> badSet.add(null), "Adding null when comparator cannot handle the sentinel must throw NullPointerException");
    }

    // -----------------------------------------------------------------
    @Test
    void addWithLengthComparator_OrdersCorrectly() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // sentinel (treated as length 0)
        assertTrue(set.add(null));
        // length 6
        assertTrue(set.add("banana"));
        // Expected order: null, kiwi, apple, banana
        Iterator<String> it = set.iterator();
        assertEquals(null, it.next(), "Null (length 0) should be first");
        assertEquals("kiwi", it.next(), "Next should be the shortest non‑null string");
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
        assertEquals(4, set.size());
        assertTrue(set.containsAll(Arrays.asList(null, "kiwi", "apple", "banana")));
    }
}
