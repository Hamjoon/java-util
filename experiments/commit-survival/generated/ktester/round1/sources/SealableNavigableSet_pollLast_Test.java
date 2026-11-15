package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_pollLast_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with known values
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no cross‑test contamination
        set = null;
        sealedState = false;
    }

    /* -------------------------------------------------
       Helper to inject a mock or custom NavigableSet into the
       private field `navSet` using reflection.
       ------------------------------------------------- */
    private void injectNavSet(NavigableSet<Integer> nav) throws Exception {
        Field f = SealableNavigableSet.class.getDeclaredField("navSet");
        f.setAccessible(true);
        f.set(set, nav);
    }

    @Test
    void pollLastWhenUnsealed_returnsAndRemovesHighestElement() {
        // given: set is unsealed (sealedState == false) and contains 10,20,30
        assertEquals(3, set.size());
        // when
        Integer last = set.pollLast();
        // then
        assertEquals(30, last);
        assertEquals(2, set.size());
        assertFalse(set.contains(30));
    }

    @Test
    void pollLastWhenSealed_throwsUnsupportedOperationException() {
        // given: seal the set
        sealedState = true;
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> set.pollLast());
    }

    @Test
    void pollLastAfterSealingThenUnsealing_behavesAccordingly() {
        // given: seal, attempt removal (should fail)
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> set.pollLast());
        // when: unseal again
        sealedState = false;
        Integer last = set.pollLast();
        // then: operation succeeds
        assertEquals(30, last);
        assertEquals(2, set.size());
    }

    @Test
    void pollLastOnEmptySet_returnsNull() {
        // given: remove all elements while unsealed
        // removes 30
        assertEquals(30, set.pollLast());
        // removes 20
        assertEquals(20, set.pollLast());
        // removes 10
        assertEquals(10, set.pollLast());
        assertTrue(set.isEmpty());
        // when
        Integer result = set.pollLast();
        // then
        assertNull(result);
    }

    /* -------------------------------------------------
       2. Sealed set – should throw IllegalStateException
       ------------------------------------------------- */
    @Test
    void pollLastWhenSealed_throwsIllegalStateException() {
        sealedState = true;
        assertThrows(IllegalStateException.class, () -> set.pollLast());
    }

    /* -------------------------------------------------
       4. Single‑element set – returns element and becomes empty
       ------------------------------------------------- */
    @Test
    void pollLastSingleElement_returnsElementAndEmptiesSet() {
        // rebuild a set that contains only one element
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(42);
        assertEquals(1, set.size());
        Integer last = set.pollLast();
        assertEquals(42, last);
        assertTrue(set.isEmpty());
    }

    /* -------------------------------------------------
       5. Custom comparator (reverse order) – greatest according to comparator
       ------------------------------------------------- */
    @Test
    void pollLastWithCustomComparator_returnsMaxAccordingToComparator() throws Exception {
        // TreeSet with reverse order: natural order 1<2<3, but comparator makes 3 smallest.
        NavigableSet<Integer> reverse = new TreeSet<>(Comparator.reverseOrder());
        reverse.addAll(Arrays.asList(1, 2, 3));
        // inject the custom underlying set
        set = new SealableNavigableSet<>(sealedSupplier);
        injectNavSet(reverse);
        // According to reverse order, the "greatest" element is 1
        Integer last = set.pollLast();
        assertEquals(1, last);
        assertEquals(2, set.size());
        assertFalse(set.contains(1));
    }

    /* -------------------------------------------------
       6. Subsequent call after set becomes empty – second call returns null
       ------------------------------------------------- */
    @Test
    void pollLastSubsequentCallAfterEmpty_returnsNull() {
        // remove all elements with a single call
        // 30
        set.pollLast();
        // 20
        set.pollLast();
        // 10
        set.pollLast();
        assertTrue(set.isEmpty());
        // second call on already‑empty set
        assertNull(set.pollLast());
    }

    /* -------------------------------------------------
       7‑11. Underlying NavigableSet throws various exceptions – they must propagate
       ------------------------------------------------- */
    @ParameterizedTest(name = "{index} => exception={0}")
    @ValueSource(classes = { RuntimeException.class, UnsupportedOperationException.class, ClassCastException.class, NullPointerException.class, ConcurrentModificationException.class })
    void pollLastUnderlyingSetThrowsException_propagates(Class<? extends Throwable> exceptionClass) throws Exception {
        // create a mock NavigableSet that throws the given exception when pollLast() is invoked
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockNav = mock(NavigableSet.class);
        when(mockNav.pollLast()).thenThrow(exceptionClass.getDeclaredConstructor().newInstance());
        // inject the mock into the SealableNavigableSet instance
        set = new SealableNavigableSet<>(sealedSupplier);
        injectNavSet(mockNav);
        // verify that the same exception type bubbles up
        Throwable thrown = assertThrows(exceptionClass, () -> set.pollLast());
        assertNotNull(thrown);
    }

    /* -------------------------------------------------
       12. Internal navSet is null – NullPointerException expected
       ------------------------------------------------- */
    @Test
    void pollLastWhenInternalNavSetIsNull_throwsNullPointerException() throws Exception {
        // force the private field to null
        set = new SealableNavigableSet<>(sealedSupplier);
        injectNavSet(null);
        assertThrows(NullPointerException.class, () -> set.pollLast());
    }
}
