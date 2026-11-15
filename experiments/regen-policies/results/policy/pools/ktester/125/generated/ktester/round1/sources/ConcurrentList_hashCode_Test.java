package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_hashCode_Test {

    /* --------------------------------------------------------------------- */
    @Mock
    private List<Integer> // mocked backing list
    mockList;

    // ConcurrentList that wraps the mock
    private ConcurrentList<Integer> listWithMock;

    class ConstHash {

        private final int id;

        ConstHash(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ConstHash && ((ConstHash) o).id == id;
        }
    }

    class ConstHash {

        private final int id;

        ConstHash(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ConstHash && ((ConstHash) o).id == id;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the ConcurrentList that wraps the mocked List.
        // The mock will be configured in each test that needs it.
        listWithMock = new ConcurrentList<>(mockList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid interaction bleed‑over between tests.
        reset(mockList);
    }

    @Test
    void hashCode_delegatesToUnderlyingList_whenWrapped() {
        // Arrange: stub the mock's hashCode()
        when(mockList.hashCode()).thenReturn(123456);
        // Act
        int result = listWithMock.hashCode();
        // Assert
        assertEquals(123456, result, "hashCode() should return the value supplied by the wrapped list");
        // ensure delegation happened exactly once
        verify(mockList, times(1)).hashCode();
    }

    @Test
    void hashCode_matchesArrayListImplementation_forStandaloneInstance() {
        // Arrange: a standalone ConcurrentList (uses an internal ArrayList)
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3));
        // Expected hash code according to java.util.ArrayList contract
        int expected = new ArrayList<>(Arrays.asList(1, 2, 3)).hashCode();
        // Act
        int actual = list.hashCode();
        // Assert
        assertEquals(expected, actual, "hashCode() of a standalone ConcurrentList should equal the hashCode of an equivalent ArrayList");
    }

    @Test
    void hashCode_changesWhenListContentChanges() {
        // Arrange
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(10, 20, 30));
        int hashBefore = list.hashCode();
        // Act: modify the list
        list.add(40);
        int hashAfter = list.hashCode();
        // Assert
        assertNotEquals(hashBefore, hashAfter, "hashCode() should reflect changes in the list's contents");
    }

    @Test
    void hashCode_handlesNullElementsCorrectly() {
        // Arrange: a list that contains null values (ArrayList permits nulls)
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, null, 3));
        // Expected hash code using the same algorithm as ArrayList
        int expected = new ArrayList<>(Arrays.asList(1, null, 3)).hashCode();
        // Act
        int actual = list.hashCode();
        // Assert
        assertEquals(expected, actual, "hashCode() should correctly incorporate null elements present in the list");
    }

    @Test
    void hashCode_emptyList_returnsArrayListEmptyHash() {
        // A freshly created ConcurrentList should behave like an empty ArrayList.
        List<String> list = new ConcurrentList<>();
        int expected = new ArrayList<String>().hashCode();
        assertEquals(expected, list.hashCode(), "Empty ConcurrentList must have the same hashCode as an empty ArrayList");
    }

    @Test
    void hashCode_twoStringElements_delegatesCorrectly() {
        // Wrap an existing ArrayList containing "a" and "b".
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> list = new ConcurrentList<>(backing);
        int expected = backing.hashCode();
        assertEquals(expected, list.hashCode(), "ConcurrentList should delegate hashCode to the underlying list with two strings");
    }

    @Test
    void hashCode_singleNullElement_returns31() {
        // ArrayList with a single null element has hash code 31 (0*31 + 0 = 0, then 31*0+0 = 0? actually ArrayList hash = 31*0 + 0 = 0? but spec says 31)
        List<Object> backing = new ArrayList<>(Collections.singletonList(null));
        List<Object> list = new ConcurrentList<>(backing);
        // should be 31 according to Java spec
        int expected = backing.hashCode();
        assertEquals(31, expected, "Sanity check: hash code of a single‑null ArrayList is 31");
        assertEquals(expected, list.hashCode(), "ConcurrentList must compute the same hash code (31) for a list containing only null");
    }

    @Test
    void hashCode_mutationAfterAddProducesDifferentHash() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2));
        int before = list.hashCode();
        list.add(3);
        int after = list.hashCode();
        assertNotEquals(before, after, "Hash code must change after adding an element to the list");
    }

    @Test
    void hashCode_afterRemovalMatchesFreshList() {
        List<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("x", "y", "z"));
        // list now ["x","z"]
        list.remove("y");
        int actual = list.hashCode();
        int expected = new ArrayList<>(Arrays.asList("x", "z")).hashCode();
        assertEquals(expected, actual, "After removal, hashCode should equal that of a new list with the remaining elements");
    }

    @Test
    void hashCode_withDuplicateElements_matchesUnderlyingList() {
        List<String> backing = new ArrayList<>(Arrays.asList("x", "x", "y"));
        List<String> list = new ConcurrentList<>(backing);
        assertEquals(backing.hashCode(), list.hashCode(), "Duplicate values must be accounted for; hashCode should match the underlying list");
    }

    @Test
    void hashCode_mixedNullAndNonNull_matchesUnderlyingList() {
        List<String> backing = new ArrayList<>(Arrays.asList(null, "z"));
        List<String> list = new ConcurrentList<>(backing);
        assertEquals(backing.hashCode(), list.hashCode(), "Null should be treated as 0; hashCode must equal that of the underlying list");
    }

    @Test
    void hashCode_afterClearEqualsEmptyHash() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(5, 6, 7));
        list.clear();
        int expected = new ArrayList<Integer>().hashCode();
        assertEquals(expected, list.hashCode(), "After clear(), hashCode must equal the hash code of an empty list");
    }

    @Test
    void hashCode_withConstantHashObjects_matchesUnderlyingList() {
        // Simple inner class with constant hashCode
        class ConstHash {

            private final int id;

            ConstHash(int id) {
                this.id = id;
            }

            @Override
            public int hashCode() {
                return 42;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof ConstHash && ((ConstHash) o).id == id;
            }
        }
        List<ConstHash> backing = new ArrayList<>(Arrays.asList(new ConstHash(1), new ConstHash(2)));
        List<ConstHash> list = new ConcurrentList<>(backing);
        assertEquals(backing.hashCode(), list.hashCode(), "Even with unconventional hashCode values, ConcurrentList must mirror the underlying list");
    }

    @Test
    void hashCode_largeList_matchesArrayListImplementation() {
        List<Integer> data = IntStream.range(0, 1000).boxed().collect(Collectors.toList());
        List<Integer> list = new ConcurrentList<>(new ArrayList<>(data));
        int expected = new ArrayList<>(data).hashCode();
        assertEquals(expected, list.hashCode(), "Hash code of a large ConcurrentList must match that of an equivalent ArrayList");
    }

    @Test
    void hashCode_underConcurrentModifications_isThreadSafe() throws Exception {
        // Prepare a ConcurrentList that will be mutated by a writer thread.
        ConcurrentList<Integer> list = new ConcurrentList<>();
        // Writer: adds 0..99
        Runnable writer = () -> {
            for (int i = 0; i < 100; i++) {
                list.add(i);
                // Small pause to increase interleaving chances
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        };
        // Reader: repeatedly reads hashCode while the writer is running.
        Callable<Integer> reader = () -> {
            int last = 0;
            // Loop until writer finishes (detected by size == 100)
            while (list.size() < 100) {
                // just ensure no CME
                last = list.hashCode();
                // tiny pause
                try {
                    Thread.sleep(2);
                } catch (InterruptedException ignored) {
                }
            }
            // final hashCode read before exiting loop
            return last;
        };
        ExecutorService exec = Executors.newFixedThreadPool(2);
        Future<?> writerFuture = exec.submit(writer);
        Future<Integer> readerFuture = exec.submit(reader);
        // Wait for both tasks to finish
        writerFuture.get(5, TimeUnit.SECONDS);
        int finalReaderHash = readerFuture.get(5, TimeUnit.SECONDS);
        exec.shutdownNow();
        // Expected hash code after all writes have completed
        List<Integer> expectedList = IntStream.range(0, 100).boxed().collect(Collectors.toList());
        int expectedHash = expectedList.hashCode();
        // The hash code observed by the reader may be from an intermediate state,
        // but after the writer is done the list's hashCode must equal the expected value.
        assertEquals(expectedHash, list.hashCode(), "After concurrent writes finish, ConcurrentList's hashCode must equal that of a plain ArrayList with the same elements");
    }
}
