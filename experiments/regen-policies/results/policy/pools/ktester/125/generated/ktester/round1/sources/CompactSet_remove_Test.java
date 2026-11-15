package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CompactSet#remove(Object)}.
 *
 * <p>
 * The tests verify the contract of {@code remove(Object)}:
 * <ul>
 *   <li>returns {@code true} when the element existed and was removed;</li>
 *   <li>returns {@code false} when the element did not exist;</li>
 *   <li>properly forwards the call to the underlying {@link CompactMap}.</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CompactSet_remove_Test{

    /** Mocked backing map used by the {@link CompactSet} under test. */
    @Mock
    CompactMap<String, Object> mockMap;

    /** Instance of {@link CompactSet} that uses the mocked {@link CompactMap}. */
    CompactSet<String> setUnderTest;

    /** Simple concrete subclass that exposes the protected constructor. */
    private static final class TestCompactSet<E> extends CompactSet<E> {
        TestCompactSet(CompactMap<E, Object> map) {
            super(map);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a CompactSet that delegates to the mocked CompactMap.
        setUnderTest = new TestCompactSet<>(mockMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test contamination.
        reset(mockMap);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void remove_existingElement_returnsTrueAndDelegatesToMap() {
        // GIVEN
        String element = "foo";
        // Simulate that the map contained the key and therefore returns a non‑null value.
        when(mockMap.remove(element)).thenReturn(new Object());

        // WHEN
        boolean result = setUnderTest.remove(element);

        // THEN
        assertTrue(result, "remove should return true when the element existed");
        verify(mockMap, times(1)).remove(element);
    }

    @Test
    void remove_nonExistingElement_returnsFalseAndDelegatesToMap() {
        // GIVEN
        String element = "bar";
        // Simulate that the key was not present in the map.
        when(mockMap.remove(element)).thenReturn(null);

        // WHEN
        boolean result = setUnderTest.remove(element);

        // THEN
        assertFalse(result, "remove should return false when the element did not exist");
        verify(mockMap, times(1)).remove(element);
    }

    @Test
    void remove_nullElement_returnsFalseAndDelegatesToMap() {
        // GIVEN
        // Simulate map.remove(null) returning null (i.e., null was not a key).
        when(mockMap.remove(null)).thenReturn(null);

        // WHEN
        boolean result = setUnderTest.remove(null);

        // THEN
        assertFalse(result, "remove should return false for a null element that is not present");
        verify(mockMap, times(1)).remove(null);
    }
}