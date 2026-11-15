package com.cedarsoftware.util;

// import org.junit.*;
// import static org.junit.Assert.*;
class CompactSet_toArray_Test {

    private CompactSet<String> set;

    //     @BeforeEach
    void setupBeforeEach() {
        set = new CompactSet<>();
        set.add("alpha");
        set.add("beta");
        set.add("gamma");
    }

    //     @AfterEach
    void teardownAfterEach() {
        set.clear();
    }

    /**
     * Demonstrates that {@link CompactSet#toArray()} returns all elements.
     */
//     void testToArrayContainsAllElements() {
//         Object[] array = set.toArray();
//         assert array.length == set.size() : "Array length should equal set size";
//         Collection<Object> asCollection = Arrays.asList(array);
//         assert asCollection.containsAll(Arrays.asList("alpha", "beta", "gamma")) : "Array should contain all set elements";
//     }

    /**
     * Demonstrates {@link CompactSet#toArray(Object[])} with an exact‑size array.
     */
//     void testToArrayExactSizeArray() {
//         String[] target = new String[3];
//         String[] result = set.toArray(target);
//         assert result == target : "Result should be the same array instance when size matches";
//         assert Arrays.asList(result).containsAll(Arrays.asList("alpha", "beta", "gamma")) : "Returned array should contain all set elements";
//     }

    /**
     * Demonstrates {@link CompactSet#toArray(Object[])} with a larger array.
     */
//     void testToArrayLargerArray() {
//         String[] target = new String[5];
//         String[] result = set.toArray(target);
//         assert result == target : "Result should be the same array instance when larger array is supplied";
//         assert Arrays.asList(result[0], result[1], result[2]).containsAll(Arrays.asList("alpha", "beta", "gamma")) : "First three slots must contain all set elements";
//         assert result[3] == null : "Element after the last populated slot must be null";
//         assert result[4] == null : "Remaining slots must be null";
//     }

    /**
     * Demonstrates {@link CompactSet#toArray(Object[])} with a smaller array.
     */
//     void testToArraySmallerArray() {
//         String[] target = new String[1];
//         String[] result = set.toArray(target);
//         assert result != target : "A new array should be allocated when supplied array is too small";
//         assert result.length == set.size() : "New array length must match set size";
//         assert Arrays.asList(result).containsAll(Arrays.asList("alpha", "beta", "gamma")) : "Returned array should contain all set elements";
//     }

    /**
     * Verifies that modifying the returned array does not affect the set.
     */
    void testArrayModificationDoesNotAffectSet() {
        Object[] array = set.toArray();
        Object original = array[0];
        array[0] = "modified";
        assert set.contains("alpha") || set.contains("beta") || set.contains("gamma") : "At least one original element must still be present";
        assert !set.contains("modified") : "Modified value should not appear in the set";
        assert set.contains(original) : "Original element must still be present after array modification";
    }

    /**
     * Checks that an empty set yields an empty array.
     */
    void testToArrayOnEmptySet() {
        set.clear();
        Object[] array = set.toArray();
        assert array.length == 0 : "Empty set should produce an empty array";
    }

    // -----------------------------------------------------------------------
//     public static void main(String[] args) {
//         CompactSetToArrayDemo demo = new CompactSetToArrayDemo();
//         demo.setupBeforeEach();
//         demo.testToArrayContainsAllElements();
//         demo.testToArrayExactSizeArray();
//         demo.testToArrayLargerArray();
//         demo.testToArraySmallerArray();
//         demo.testArrayModificationDoesNotAffectSet();
//         demo.testToArrayOnEmptySet();
//         demo.teardownAfterEach();
//         System.out.println("All demo checks passed.");
//     }
}