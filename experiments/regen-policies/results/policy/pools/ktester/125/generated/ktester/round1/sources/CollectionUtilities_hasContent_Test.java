package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for {@link CollectionUtilities#hasContent(Collection)}.
 *
 * The tests verify the three expected behaviours:
 * <ul>
 *   <li>returns {@code false} when the collection reference is {@code null}</li>
 *   <li>returns {@code false} when the collection is empty</li>
 *   <li>returns {@code true} when the collection contains at least one element</li>
 * </ul>
 *
 * Different concrete {@link java.util.Collection} implementations are exercised
 * (e.g., {@link ArrayList} and {@link LinkedHashSet}) to ensure the utility method
 * works regardless of the runtime type.
 */
@ExtendWith(MockitoExtension.class)
class CollectionUtilities_hasContent_Test{

    /** Sample mutable list used in the tests. */
    private List<String> mutableList;

    /** Sample mutable set used in the tests. */
    private Set<Integer> mutableSet;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for this static utility class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh collections for each test to guarantee isolation.
        mutableList = new ArrayList<>();
        mutableSet = new LinkedHashSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly necessary but keeps the pattern consistent.
        mutableList.clear();
        mutableSet.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    @DisplayName("hasContent returns false for a null collection")
    void hasContent_NullCollection_ReturnsFalse() {
        // Given
        List<String> nullList = null;

        // When
        boolean result = CollectionUtilities.hasContent(nullList);

        // Then
        assertFalse(result, "hasContent should return false when the collection is null");
    }

    @Test
    @DisplayName("hasContent returns false for an empty collection")
    void hasContent_EmptyCollection_ReturnsFalse() {
        // Given
        // mutableList and mutableSet are already empty after setupBeforeEach

        // When
        boolean listResult = CollectionUtilities.hasContent(mutableList);
        boolean setResult = CollectionUtilities.hasContent(mutableSet);

        // Then
        assertFalse(listResult, "hasContent should return false for an empty List");
        assertFalse(setResult, "hasContent should return false for an empty Set");
    }

    @Test
    @DisplayName("hasContent returns true for a collection with at least one element")
    void hasContent_NonEmptyCollection_ReturnsTrue() {
        // Given
        mutableList.add("element");
        mutableSet.add(42);

        // When
        boolean listResult = CollectionUtilities.hasContent(mutableList);
        boolean setResult = CollectionUtilities.hasContent(mutableSet);

        // Then
        assertTrue(listResult, "hasContent should return true for a non‑empty List");
        assertTrue(setResult, "hasContent should return true for a non‑empty Set");
    }

    @Test
    @DisplayName("hasContent works with immutable collections")
    void hasContent_ImmutableCollection_ReturnsCorrectResult() {
        // Given
        List<String> immutableEmpty = Collections.emptyList();
        Set<String> immutableNonEmpty = Collections.singleton("only");

        // When
        boolean emptyResult = CollectionUtilities.hasContent(immutableEmpty);
        boolean nonEmptyResult = CollectionUtilities.hasContent(immutableNonEmpty);

        // Then
        assertFalse(emptyResult, "hasContent should return false for an immutable empty collection");
        assertTrue(nonEmptyResult, "hasContent should return true for an immutable non‑empty collection");
    }
}