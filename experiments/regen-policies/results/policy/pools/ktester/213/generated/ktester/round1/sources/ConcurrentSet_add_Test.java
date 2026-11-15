package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

// kept from the template; not used but harmless
@ExtendWith(MockitoExtension.class)
class ConcurrentSet_add_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        intSet = null;
        stringSet = null;
    }

    @Test
    void addNewElementReturnsTrueAndIsPresent() {
        assertTrue(intSet.add(10), "Adding a new element should return true");
        assertTrue(intSet.contains(10), "Set must contain the added element");
        assertEquals(1, intSet.size(), "Size should be 1 after one addition");
    }

    @Test
    void addDuplicateElementReturnsFalse() {
        assertTrue(intSet.add(5), "First addition of 5 should return true");
        assertFalse(intSet.add(5), "Second addition of the same element should return false");
        assertEquals(1, intSet.size(), "Size must stay 1 after duplicate add");
    }

    @Test
    void addNullElementIsHandledCorrectly() {
        // ConcurrentSet supports null via a sentinel value
        assertTrue(stringSet.add(null), "Adding null for the first time should return true");
        assertTrue(stringSet.contains(null), "Set must contain null after addition");
        assertFalse(stringSet.add(null), "Adding null again should return false");
        assertEquals(1, stringSet.size(), "Size should be 1 after adding null once");
    }

    @Test
    void addMultipleElementsConcurrently() throws InterruptedException {
        int threadCount = 4;
        int perThread = 5000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger added = new AtomicInteger();
        for (int i = 0; i < threadCount; i++) {
            final int base = i * perThread;
            new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    if (intSet.add(base + j)) {
                        added.incrementAndGet();
                    }
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        // All values are distinct, so every add should have succeeded
        assertEquals(threadCount * perThread, added.get(), "All distinct adds must return true");
        assertEquals(threadCount * perThread, intSet.size(), "Set size must equal number of distinct elements added");
    }

    @Test
    void addAfterClearWorksAsExpected() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertEquals(3, intSet.size(), "Pre‑condition size");
        intSet.clear();
        assertTrue(intSet.isEmpty(), "Set must be empty after clear");
        assertTrue(intSet.add(42), "Adding after clear should succeed");
        assertTrue(intSet.contains(42), "Set must contain the newly added element");
        assertEquals(1, intSet.size(), "Size must be 1 after adding post‑clear");
    }

    @Test
    void addAllUsesAddInternally() {
        // The contract of addAll is to delegate to add(T). We verify the observable effect.
        // Adding a collection that contains duplicates should result in the same behaviour
        // as calling add() repeatedly.
        intSet.addAll(Arrays.asList(7, 7, 8));
        assertTrue(intSet.contains(7), "Set must contain 7");
        assertTrue(intSet.contains(8), "Set must contain 8");
        assertEquals(2, intSet.size(), "Duplicate element must not increase size");
    }

    @Test
    void addEmptyCollectionDoesNotChangeSet() {
        intSet.addAll(Collections.emptyList());
        assertTrue(intSet.isEmpty(), "Adding an empty collection must leave the set empty");
    }

    /**
     * Group: add(T) - basic functionality and null handling
     *
     * <p>Runs the four scenarios in order, using a {@code ConcurrentSet<String>}
     * because all values are strings (or {@code null}).</p>
     */
    @Test
    void addBasicFunctionalityAndNullHandling() {
        // 1) add a non‑null element – expect true
        assertTrue(stringSet.add("element1"), "First addition of a non‑null element should return true");
        assertTrue(stringSet.contains("element1"), "Set must contain the element after successful add");
        assertEquals(1, stringSet.size(), "Size should be 1 after adding a single distinct element");
        // 2) add the same element again – expect false
        assertFalse(stringSet.add("element1"), "Adding a duplicate element must return false");
        assertEquals(1, stringSet.size(), "Size must stay unchanged after duplicate addition");
        // 3) add null for the first time – expect true
        assertTrue(stringSet.add(null), "First addition of null should return true");
        assertTrue(stringSet.contains(null), "Set must contain null after it is added");
        assertEquals(2, stringSet.size(), "Size should be 2 after adding a distinct null element");
        // 4) add null again – expect false
        assertFalse(stringSet.add(null), "Adding null a second time must return false");
        assertEquals(2, stringSet.size(), "Size must remain 2 after duplicate null addition");
    }

    /**
     * Group: add(T) - concurrency and wrap uniqueness
     *
     * <p>Executes a sequence of adds that exercise case‑sensitivity,
     * handling of different runtime types, distinct object instances,
     * special characters, and large numeric values.</p>
     *
     * <p>The test uses a raw {@code ConcurrentSet<Object>} so that any
     * object type can be stored.</p>
     */
    @Test
    void addConcurrencyAndWrapUniqueness() {
        // A fresh set for the whole sequence
        ConcurrentSet<Object> mixedSet = new ConcurrentSet<>();
        // 1) distinct element after clear (simulated by starting with an empty set)
        assertTrue(mixedSet.add("element1"), "Adding a distinct element to an empty set should return true");
        assertTrue(mixedSet.contains("element1"));
        assertEquals(1, mixedSet.size());
        // 2) case‑sensitive different string
        assertTrue(mixedSet.add("ELEMENT1"), "Adding a case‑different string must be considered distinct");
        assertTrue(mixedSet.contains("ELEMENT1"));
        assertEquals(2, mixedSet.size());
        // 3) Integer object – first insertion
        Integer int42 = Integer.valueOf(42);
        assertTrue(mixedSet.add(int42), "First insertion of Integer 42 should succeed");
        assertTrue(mixedSet.contains(int42));
        assertEquals(3, mixedSet.size());
        // 4) duplicate Integer – should be detected via equals()
        assertFalse(mixedSet.add(Integer.valueOf(42)), "Adding the same Integer value again must return false");
        assertEquals(3, mixedSet.size(), "Size must not increase after duplicate Integer addition");
        // 5) freshly created Object – first instance
        Object obj1 = new Object();
        assertTrue(mixedSet.add(obj1), "First distinct Object instance should be added successfully");
        assertTrue(mixedSet.contains(obj1));
        assertEquals(4, mixedSet.size());
        // 6) second distinct Object instance – should also succeed
        Object obj2 = new Object();
        assertTrue(mixedSet.add(obj2), "A second distinct Object instance must be considered unique");
        assertTrue(mixedSet.contains(obj2));
        assertEquals(5, mixedSet.size());
        // 7) null after other elements – first null insertion
        assertTrue(mixedSet.add(null), "Adding null when other elements exist should still succeed once");
        assertTrue(mixedSet.contains(null));
        assertEquals(6, mixedSet.size());
        // 8) empty string – first insertion
        assertTrue(mixedSet.add(""), "Empty string should be accepted as a distinct element");
        assertTrue(mixedSet.contains(""));
        assertEquals(7, mixedSet.size());
        // 9) duplicate empty string – must be rejected
        assertFalse(mixedSet.add(""), "Adding the same empty string again must return false");
        assertEquals(7, mixedSet.size(), "Size must stay unchanged after duplicate empty string");
        // 10) string containing a NUL character – first insertion
        String nulString = "\u0000";
        assertTrue(mixedSet.add(nulString), "String with NUL character should be added successfully");
        assertTrue(mixedSet.contains(nulString));
        assertEquals(8, mixedSet.size());
        // 11) duplicate NUL‑character string – must be rejected
        assertFalse(mixedSet.add("\u0000"), "Adding the same NUL‑character string again must return false");
        assertEquals(8, mixedSet.size(), "Size must remain unchanged after duplicate NUL string");
        // 12) Long.MAX_VALUE – first insertion
        Long maxLong = Long.MAX_VALUE;
        assertTrue(mixedSet.add(maxLong), "Adding Long.MAX_VALUE for the first time should succeed");
        assertTrue(mixedSet.contains(maxLong));
        assertEquals(9, mixedSet.size());
        // 13) duplicate Long.MAX_VALUE – must be rejected
        assertFalse(mixedSet.add(Long.MAX_VALUE), "Adding Long.MAX_VALUE a second time must return false");
        assertEquals(9, mixedSet.size(), "Size must stay constant after duplicate Long addition");
    }
}
