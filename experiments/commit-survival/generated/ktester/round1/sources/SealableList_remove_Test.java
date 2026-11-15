package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SealableList_remove_Test {

    private SealableList<Integer> list;

    // mutable sealed flag used by the supplier
    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    // mock backing list – we verify delegation to it
    @Mock
    private List<Integer> mockBackingList;

    @BeforeEach
    void setupBeforeEach() {
        // initialise the mutable sealed flag and its supplier
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // initialise mocks (MockitoExtension already does this, but we keep it explicit)
        MockitoAnnotations.openMocks(this);
        // create the SealableList that wraps the mocked List
        list = new SealableList<>(mockBackingList, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference
        reset(mockBackingList);
    }

    @Test
    void remove_DelegatesToBackingList_WhenNotSealed_AndElementExists() {
        // given
        Integer element = 42;
        when(mockBackingList.remove(element)).thenReturn(true);
        // when
        boolean result = list.remove(element);
        // then
        assertTrue(result, "remove should return true when the backing list reports removal");
        verify(mockBackingList, times(1)).remove(element);
    }

    @Test
    void remove_DelegatesToBackingList_WhenNotSealed_AndElementDoesNotExist() {
        // given
        Integer element = 99;
        when(mockBackingList.remove(element)).thenReturn(false);
        // when
        boolean result = list.remove(element);
        // then
        assertFalse(result, "remove should return false when the backing list reports no removal");
        verify(mockBackingList, times(1)).remove(element);
    }

    @Test
    void remove_ThrowsUnsupportedOperationException_WhenSealed() {
        // given
        // seal the list
        sealedState = true;
        Integer element = 10;
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> list.remove(element), "remove should throw when the list is sealed");
        // ensure the backing list was never touched
        verify(mockBackingList, never()).remove(any());
    }

    @Test
    void remove_NullElement_DelegatesCorrectly_WhenNotSealed() {
        // given
        when(mockBackingList.remove(null)).thenReturn(true);
        // when
        boolean result = list.remove(null);
        // then
        assertTrue(result, "remove should correctly handle null elements");
        verify(mockBackingList, times(1)).remove(null);
    }

    @Test
    void remove_ThrowsWhenSealed_EvenForNullElement() {
        // given
        // seal the list
        sealedState = true;
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> list.remove(null), "remove should still throw when sealed, even for a null argument");
        verify(mockBackingList, never()).remove(any());
    }

    /**
     * Duplicate element handling – the underlying list contains the same value twice.
     * The contract of {@link List#remove(Object)} is to remove the first occurrence.
     */
    @Test
    void remove_DuplicateElement_RemovesFirstOccurrence() {
        // Use a real mutable list to observe size changes.
        List<String> backing = new ArrayList<>(Arrays.asList("dup", "dup", "other"));
        SealableList<String> dupList = new SealableList<>(backing, sealedSupplier);
        boolean removed = dupList.remove("dup");
        assertTrue(removed, "remove should return true when a duplicate element is present");
        assertEquals(2, backing.size(), "list size should decrease by one");
        assertEquals("dup", backing.get(0), "the first occurrence should be removed, second stays");
    }

    /**
     * Null element present – the backing list holds a {@code null} value.
     */
    @Test
    void remove_NullElementPresent_RemovesIt() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", null, "b"));
        SealableList<String> nullList = new SealableList<>(backing, sealedSupplier);
        boolean removed = nullList.remove(null);
        assertTrue(removed, "remove(null) should return true when null is present");
        assertEquals(2, backing.size(), "size should decrease after removing null");
        assertFalse(backing.contains(null), "null should no longer be in the list");
    }

    /**
     * Empty list – any remove operation must return {@code false} and not throw.
     */
    @ParameterizedTest(name = "remove on empty list with element = {0}")
    @CsvSource({ "'anything', false", "'', false", "null, false" })
    void remove_EmptyList_ReturnsFalse(Object element, boolean expected) {
        List<String> emptyBacking = new ArrayList<>();
        SealableList<String> emptyList = new SealableList<>(emptyBacking, sealedSupplier);
        boolean result = emptyList.remove(element);
        assertEquals(expected, result, "remove on empty list should return false");
    }

    /**
     * Type mismatch – the list holds {@link String}s but we try to remove an {@link Object}
     * that is not equal to any element.
     */
    @Test
    void remove_TypeMismatch_ReturnsFalse() {
        List<String> backing = new ArrayList<>(Arrays.asList("one", "two"));
        SealableList<String> typeList = new SealableList<>(backing, sealedSupplier);
        Object foreign = new Object();
        boolean result = typeList.remove(foreign);
        assertFalse(result, "remove with a different type should return false");
        assertEquals(2, backing.size(), "list size must stay unchanged");
    }

    /**
     * Underlying list is immutable – e.g., created via {@link Collections#unmodifiableList}.
     * The {@code remove} call should propagate the {@link UnsupportedOperationException}
     * thrown by the delegate.
     */
    @Test
    void remove_UnderlyingImmutableList_PropagatesException() {
        List<String> mutable = new ArrayList<>(Arrays.asList("any"));
        List<String> unmodifiable = Collections.unmodifiableList(mutable);
        SealableList<String> immutableWrapper = new SealableList<>(unmodifiable, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, () -> immutableWrapper.remove("any"), "remove should propagate UnsupportedOperationException from an immutable delegate");
    }

    /**
     * Sealed after some modifications – the list is sealed **after** a successful add.
     * Subsequent remove must still throw.
     */
    @Test
    void remove_SealedAfterModifications_Throws() {
        List<String> backing = new ArrayList<>(Arrays.asList("first"));
        SealableList<String> modifiable = new SealableList<>(backing, sealedSupplier);
        // perform a legitimate modification before sealing
        assertTrue(modifiable.add("later"));
        assertEquals(2, backing.size());
        // now seal the list
        sealedState = true;
        // attempt removal – should fail with UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> modifiable.remove("later"), "remove must respect sealed state even after prior modifications");
    }

    /**
     * Underlying list is {@code null}. The constructor is allowed to receive {@code null}
     * (or we force it via reflection). Any call to {@code remove} should result in a
     * {@link NullPointerException} when the method tries to delegate to the internal list.
     */
    @Test
    void remove_UnderlyingListNull_ThrowsNullPointerException() throws Exception {
        // Construct with a non‑null list first, then replace the private field via reflection.
        SealableList<String> withNull = new SealableList<>(new ArrayList<>(), sealedSupplier);
        Field field = SealableList.class.getDeclaredField("list");
        field.setAccessible(true);
        // force the internal delegate to be null
        field.set(withNull, null);
        assertThrows(NullPointerException.class, () -> withNull.remove("any"), "remove should throw NPE when the internal delegate list is null");
    }

    /**
     * Additional sanity check: when the list is not sealed and the element is absent,
     * the method returns {@code false}. This mirrors one of the JSON cases but uses a real list.
     */
    @Test
    void remove_ElementAbsent_ReturnsFalse() {
        List<String> backing = new ArrayList<>(Arrays.asList("present"));
        SealableList<String> realList = new SealableList<>(backing, sealedSupplier);
        boolean result = realList.remove("absent");
        assertFalse(result, "remove should return false when element is not present");
        assertEquals(1, backing.size(), "size must stay unchanged");
    }
}
