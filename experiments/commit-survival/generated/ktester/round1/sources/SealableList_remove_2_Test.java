package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableList_remove_2_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    private SealableList<Integer> intList;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // Initialise the SealableList with a mutable backing collection for the integer‑based tests
        intList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        intList.add(10);
        intList.add(20);
        intList.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the list reference is cleared between tests
        intList = null;
    }

    @Test
    void removeByIndex_returnsRemovedElement_andShiftsElements() {
        // given
        assertEquals(3, intList.size());
        assertEquals(Integer.valueOf(20), intList.get(1));
        // when
        // remove element at index 1 (value 20)
        Integer removed = intList.remove(1);
        // then
        assertEquals(Integer.valueOf(20), removed);
        assertEquals(2, intList.size());
        assertFalse(intList.contains(20));
        // element that was at index 2 should now be at index 1
        assertEquals(Integer.valueOf(30), intList.get(1));
    }

    @Test
    void removeByIndex_whenSealed_throwsUnsupportedOperationException() {
        // given
        sealedState = true;
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> intList.remove(0));
    }

    @Test
    void removeByIndex_withNegativeIndex_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> intList.remove(-1));
    }

    @Test
    void removeByIndex_withIndexEqualToSize_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> intList.remove(intList.size()));
    }

    @Test
    void removeByIndex_fromEmptyList_throwsIndexOutOfBoundsException() {
        // given
        // now list is empty
        intList.clear();
        assertTrue(intList.isEmpty());
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> intList.remove(0));
    }

    @Test
    void removeByIndex_multipleRemovalsMaintainIntegrity() {
        // given
        assertEquals(3, intList.size());
        // first removal
        // removes 10
        Integer first = intList.remove(0);
        assertEquals(Integer.valueOf(10), first);
        assertEquals(2, intList.size());
        assertEquals(Integer.valueOf(20), intList.get(0));
        // second removal
        // removes 30 (original index 2)
        Integer second = intList.remove(1);
        assertEquals(Integer.valueOf(30), second);
        assertEquals(1, intList.size());
        assertEquals(Integer.valueOf(20), intList.get(0));
    }

    /**
     * Successful removal from an unsealed list with {@link String} elements.
     *
     * The test data is supplied via {@code @CsvSource} to cover the two scenarios
     * described in the JSON: removing the middle element and removing the first element.
     */
    @ParameterizedTest(name = "[{index}] remove index={0} expects=\"{1}\" resultingList={2}")
    @CsvSource({ // index, expectedRemoved, expectedRemainingList (as a semicolon‑separated string)
    "1, B, A;C", "0, X, Y" })
    void removeStringList_successfulRemoval(int index, String expectedRemoved, String remaining) {
        // given – initialise a SealableList<String> with the appropriate elements
        List<String> backing = new ArrayList<>();
        if (index == 1) {
            // scenario 1: ["A","B","C"]
            backing.addAll(Arrays.asList("A", "B", "C"));
        } else {
            // scenario 2: ["X","Y"]
            backing.addAll(Arrays.asList("X", "Y"));
        }
        SealableList<String> stringList = new SealableList<>(backing, sealedSupplier);
        assertFalse(sealedState, "List must be unsealed for successful removal");
        // when
        String removed = stringList.remove(index);
        // then
        assertEquals(expectedRemoved, removed, "Removed element does not match expectation");
        List<String> expectedRemaining = remaining.isEmpty() ? Collections.emptyList() : Arrays.asList(remaining.split(";"));
        assertEquals(expectedRemaining, stringList, "List contents after removal are incorrect");
    }

    /**
     * Attempt to remove an element from a sealed list.
     *
     * According to the API contract, {@code remove(int)} must throw
     * {@link IllegalStateException} when the list is sealed.
     */
    @Test
    void removeFromSealedList_throwsIllegalStateException() {
        // given – seal the list
        sealedState = true;
        SealableList<String> sealedList = new SealableList<>(new ArrayList<>(List.of("a", "b")), sealedSupplier);
        // when / then
        assertThrows(IllegalStateException.class, () -> sealedList.remove(0), "Removing from a sealed list should raise IllegalStateException");
    }

    /**
     * Invalid index handling – negative index and index equal to size.
     *
     * Both cases must result in {@link IndexOutOfBoundsException}.
     */
    @ParameterizedTest(name = "[{index}] remove with invalid index={0}")
    @CsvSource({ "-1", "3" })
    void removeWithInvalidIndex_throwsIndexOutOfBoundsException(int invalidIndex) {
        // given – a list of size 3 (["A","B","C"])
        SealableList<String> list = new SealableList<>(new ArrayList<>(List.of("A", "B", "C")), sealedSupplier);
        assertEquals(3, list.size());
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(invalidIndex), "Invalid index should trigger IndexOutOfBoundsException");
    }

    /**
     * Boundary index values:
     * <ul>
     *   <li>Removing the sole element from a size‑1 list.</li>
     *   <li>Removing the last element (index size‑1) from a size‑2 list.</li>
     * </ul>
     */
    @Test
    void removeBoundaryIndices_behaveCorrectly() {
        // --- case 1: size 1, remove index 0 ---
        SealableList<String> singleton = new SealableList<>(new ArrayList<>(List.of("first")), sealedSupplier);
        String removedFirst = singleton.remove(0);
        assertEquals("first", removedFirst);
        assertTrue(singleton.isEmpty(), "List should be empty after removing the only element");
        // --- case 2: size 2, remove last index (1) ---
        SealableList<String> twoElements = new SealableList<>(new ArrayList<>(List.of("first", "second")), sealedSupplier);
        String removedSecond = twoElements.remove(1);
        assertEquals("second", removedSecond);
        assertEquals(1, twoElements.size());
        assertEquals("first", twoElements.get(0));
    }

    /**
     * Underlying list does not support removal (e.g., an unmodifiable list).
     *
     * The {@code SealableList} must propagate the {@link UnsupportedOperationException}
     * thrown by the delegate.
     */
    @Test
    void removeFromUnmodifiableUnderlyingList_propagatesUnsupportedOperationException() {
        // given – an unmodifiable list as the delegate
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>(List.of("alpha", "beta")));
        SealableList<String> sealedList = new SealableList<>(unmodifiable, sealedSupplier);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> sealedList.remove(0), "Removal should propagate UnsupportedOperationException from the underlying list");
    }

    /**
     * Underlying list is a Mockito mock that throws {@link UnsupportedOperationException}
     * when {@code remove(int)} is invoked. This verifies that the wrapper does not
     * swallow the exception.
     */
    @Test
    void removeFromMockedList_propagatesUnsupportedOperationException() {
        // given – a mock List that throws on remove
        @SuppressWarnings("unchecked")
        List<String> mockList = mock(List.class);
        when(mockList.remove(anyInt())).thenThrow(new UnsupportedOperationException("mocked"));
        SealableList<String> wrapper = new SealableList<>(mockList, sealedSupplier);
        // when / then
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> wrapper.remove(0));
        assertEquals("mocked", ex.getMessage());
    }
}
