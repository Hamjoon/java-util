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
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_hashCode_Test {

    @Mock
    Supplier<Boolean> // not used by hashCode, but required by ctor
    sealedSupplier;

    @Mock
    NavigableMap<String, Integer> // mock to verify delegation
    mockNavMap;

    SealableNavigableMap<String, Integer> sealableMap;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks (MockitoExtension already does this, but explicit init is harmless)
        MockitoAnnotations.openMocks(this);
        // By default the map is considered unsealed
        when(sealedSupplier.get()).thenReturn(false);
        // Build the SealableNavigableMap that wraps the mocked NavigableMap
        sealableMap = new SealableNavigableMap<>(mockNavMap, sealedSupplier);
    }

    private static Stream<Arguments> delegationScenarioProvider() {
        // empty map
        NavigableMap<String, Integer> empty = new TreeMap<>();
        // map with multiple entries
        NavigableMap<String, Integer> many = new TreeMap<>();
        many.put("one", 1);
        many.put("two", 2);
        many.put("three", 3);
        many.put("four", 4);
        // map containing null values
        NavigableMap<String, Integer> withNulls = new TreeMap<>();
        withNulls.put("a", null);
        withNulls.put("b", 2);
        withNulls.put("c", null);
        // map after modifications (start with some, then mutate)
        NavigableMap<String, Integer> mutable = new TreeMap<>();
        mutable.put("x", 10);
        mutable.put("y", 20);
        // mutate after creation of SealableNavigableMap in the test method
        // (the test will add/remove entries before calling hashCode)
        // We'll return the map as‑is; the test method will perform modifications.
        // To keep the provider simple, we just pass the map and let the test method handle changes.
        // The test method will add an entry, compute hashCode, then remove it and compute again.
        // We'll use a wrapper to indicate this special case via description.
        return Stream.of(Arguments.of("Empty map", empty), Arguments.of("Map with multiple entries", many), Arguments.of("Map containing null values", withNulls), Arguments.of("Map after modifications (handled in test)", mutable));
    }

    @Test
    void hashCode_delegatesToUnderlyingMap() {
        // Arrange
        when(mockNavMap.hashCode()).thenReturn(42_424);
        // Act
        int result = sealableMap.hashCode();
        // Assert
        assertEquals(42_424, result, "hashCode should return the value from the wrapped map");
        verify(mockNavMap, times(1)).hashCode();
    }

    @Test
    void hashCode_reflectsChangesInUnderlyingMap() {
        // First call returns 100
        when(mockNavMap.hashCode()).thenReturn(100);
        int first = sealableMap.hashCode();
        // Second call returns 200 (simulating a change in the underlying map)
        when(mockNavMap.hashCode()).thenReturn(200);
        int second = sealableMap.hashCode();
        assertNotEquals(first, second, "hashCode should change when the underlying map's hashCode changes");
    }

    @Test
    void hashCode_worksWhenMapIsSealed() {
        // Simulate a sealed state
        when(sealedSupplier.get()).thenReturn(true);
        when(mockNavMap.hashCode()).thenReturn(7_777);
        int result = sealableMap.hashCode();
        assertEquals(7_777, result, "hashCode must still delegate correctly even when the map is sealed");
        verify(mockNavMap, times(1)).hashCode();
    }

    @Test
    void hashCode_matchesRealUnderlyingMap() {
        // Use a real NavigableMap (TreeMap) to ensure the delegation produces the same value as the map itself
        NavigableMap<String, Integer> realMap = new TreeMap<>();
        realMap.put("alpha", 1);
        realMap.put("beta", 2);
        realMap.put("gamma", 3);
        // The real map is unsealed for this test
        Supplier<Boolean> realSupplier = () -> false;
        SealableNavigableMap<String, Integer> realSealable = new SealableNavigableMap<>(realMap, realSupplier);
        assertEquals(realMap.hashCode(), realSealable.hashCode(), "hashCode of SealableNavigableMap should equal hashCode of the wrapped real map");
    }

    @Test
    void hashCode_ofEmptyMap_isZero() {
        // Empty underlying map
        NavigableMap<String, Integer> emptyMap = new TreeMap<>();
        Supplier<Boolean> supplier = () -> false;
        SealableNavigableMap<String, Integer> emptySealable = new SealableNavigableMap<>(emptyMap, supplier);
        assertEquals(emptyMap.hashCode(), emptySealable.hashCode(), "hashCode of an empty SealableNavigableMap should match the empty underlying map");
    }

    /**
     * Parameterized test covering several delegation scenarios:
     *   - empty map
     *   - map with multiple entries
     *   - map containing null values (TreeMap allows null values but not null keys)
     *   - map after modifications
     *   - repeated calls without mutation (consistency)
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("delegationScenarioProvider")
    void hashCode_delegationScenarios(String description, NavigableMap<String, Integer> underlying) {
        Supplier<Boolean> supplier = () -> false;
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(underlying, supplier);
        int first = sut.hashCode();
        // second call without mutation
        int second = sut.hashCode();
        assertEquals(underlying.hashCode(), first, "hashCode should match the underlying map's hashCode for scenario: " + description);
        assertEquals(first, second, "Repeated hashCode calls should be consistent for scenario: " + description);
    }

    @Test
    void hashCode_reflectsModificationsAfterCreation() {
        // Create a map, wrap it, then modify the underlying map.
        NavigableMap<String, Integer> base = new TreeMap<>();
        base.put("k1", 1);
        base.put("k2", 2);
        Supplier<Boolean> supplier = () -> false;
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(base, supplier);
        int before = sut.hashCode();
        // Mutate underlying map
        base.put("k3", 3);
        base.remove("k1");
        int after = sut.hashCode();
        assertNotEquals(before, after, "hashCode should reflect modifications made to the underlying map after the wrapper was created");
        assertEquals(base.hashCode(), after, "hashCode after modification must equal the underlying map's hashCode");
    }

    @Test
    void hashCode_consistentAcrossMultipleCallsWithoutMutation() {
        NavigableMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Supplier<Boolean> supplier = () -> false;
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(map, supplier);
        int first = sut.hashCode();
        int second = sut.hashCode();
        int third = sut.hashCode();
        assertEquals(first, second, "hashCode should be stable across calls");
        assertEquals(second, third, "hashCode should remain stable across multiple calls");
    }

    @Test
    void hashCode_withCustomComparator() {
        // TreeMap with reverse order comparator
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableMap<String, Integer> reverseMap = new TreeMap<>(reverse);
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        reverseMap.put("c", 3);
        Supplier<Boolean> supplier = () -> false;
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(reverseMap, supplier);
        assertEquals(reverseMap.hashCode(), sut.hashCode(), "hashCode should be identical to the underlying map's hashCode even when a custom comparator is used");
    }

    @Test
    void hashCode_nullDelegate_throwsNPE() {
        Supplier<Boolean> supplier = () -> false;
        // Construct with a null underlying map – the constructor of SealableNavigableMap does not guard against null,
        // so calling hashCode() should result in a NullPointerException.
        SealableNavigableMap<String, Integer> sut = new SealableNavigableMap<>(null, supplier);
        assertThrows(NullPointerException.class, sut::hashCode, "Calling hashCode on a SealableNavigableMap with a null delegate should throw NullPointerException");
    }
}
