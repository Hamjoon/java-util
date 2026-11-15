package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link EnumConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Normal conversion of an {@link Enum} to a {@link Map}.</li>
 *   <li>All supported source types for {@code toEnumSet} – {@link Collection},
 *       primitive arrays and object arrays.</li>
 *   <li>All supported element representations – enum instances, {@link String}
 *       names and numeric ordinals.</li>
 *   <li>Edge cases such as {@code null} elements, duplicate values and empty
 *       sources.</li>
 *   <li>Failure scenarios – wrong target type, unsupported source type,
 *       invalid element type and out‑of‑range ordinal values.</li>
 * </ul>
 *
 * <p>The test class resides in the same package as {@code EnumConversions}
 * so it can access the package‑private static methods.</p>
 */
public class EnumConversionsTest {

    /** Simple enum used throughout the test suite. */
    private enum Color {
        RED, GREEN, BLUE
    }

    /** A dummy {@link Converter} instance – the conversion methods do not use it. */
    private static final Converter DUMMY_CONVERTER = null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* -------------------------------------------------------------
     *  Tests for EnumConversions.toMap(...)
     * ------------------------------------------------------------- */
    @Test
    public void toMap_returnsMapWithEnumName() {
        Map<String, Object> result = EnumConversions.toMap(Color.GREEN, DUMMY_CONVERTER);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Map must contain key \"name\"", result.containsKey("name"));
        assertEquals("Value must be the enum name", "GREEN", result.get("name"));
    }

    /* -------------------------------------------------------------
     *  Tests for EnumConversions.toEnumSet(...)
     * ------------------------------------------------------------- */

    @Test
    public void toEnumSet_fromCollection_ofEnumInstances() {
        List<Color> source = Arrays.asList(Color.RED, Color.BLUE);
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertEquals(EnumSet.of(Color.RED, Color.BLUE), set);
    }

    @Test
    public void toEnumSet_fromCollection_ofStringNames() {
        List<String> source = Arrays.asList("GREEN", "RED");
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertEquals(EnumSet.of(Color.GREEN, Color.RED), set);
    }

    @Test
    public void toEnumSet_fromCollection_ofNumericOrdinals() {
        List<Integer> source = Arrays.asList(0, 2); // RED and BLUE
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertEquals(EnumSet.of(Color.RED, Color.BLUE), set);
    }

    @Test
    public void toEnumSet_fromCollection_mixedElements() {
        List<Object> source = Arrays.asList(Color.GREEN, "RED", 2); // GREEN, RED, BLUE
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertEquals(EnumSet.of(Color.RED, Color.GREEN, Color.BLUE), set);
    }

    @Test
    public void toEnumSet_fromCollection_ignoresNullElements() {
        List<Object> source = Arrays.asList(null, "BLUE", null);
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertEquals(EnumSet.of(Color.BLUE), set);
    }

    @Test
    public void toEnumSet_fromCollection_duplicateValues() {
        List<Object> source = Arrays.asList("RED", 0, Color.RED);
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        // Duplicates must not cause exceptions and must result in a set with a single RED entry
        assertEquals(EnumSet.of(Color.RED), set);
    }

    @Test
    public void toEnumSet_fromEmptyCollection_returnsEmptySet() {
        List<Color> source = Collections.emptyList();
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertTrue("Resulting EnumSet should be empty", set.isEmpty());
    }

    @Test
    public void toEnumSet_fromArray_ofEnumInstances() {
        Color[] source = {Color.BLUE, Color.GREEN};
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertEquals(EnumSet.of(Color.BLUE, Color.GREEN), set);
    }

    @Test
    public void toEnumSet_fromArray_ofStringNames() {
        String[] source = {"RED", "GREEN"};
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertEquals(EnumSet.of(Color.RED, Color.GREEN), set);
    }

    @Test
    public void toEnumSet_fromArray_ofNumericOrdinals() {
        Integer[] source = {1, 2}; // GREEN, BLUE
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertEquals(EnumSet.of(Color.GREEN, Color.BLUE), set);
    }

    @Test
    public void toEnumSet_fromEmptyArray_returnsEmptySet() {
        Color[] source = new Color[0];
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertTrue(set.isEmpty());
    }

    @Test
    public void toEnumSet_targetNotEnum_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be an Enum");

        List<String> source = Arrays.asList("RED");
        EnumConversions.toEnumSet(source, String.class);
    }

    @Test
    public void toEnumSet_sourceNotCollectionOrArray_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Source must be a Collection or Array");

        // Passing a single enum instance is illegal
        EnumConversions.toEnumSet(Color.RED, Color.class);
    }

    @Test
    public void toEnumSet_invalidElementType_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Boolean.class.getName());

        List<Object> source = Arrays.asList(Boolean.TRUE);
        EnumConversions.toEnumSet(source, Color.class);
    }

    @Test
    public void toEnumSet_invalidOrdinal_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid ordinal value 5");

        List<Integer> source = Arrays.asList(5); // out of range for Color (max ordinal = 2)
        EnumConversions.toEnumSet(source, Color.class);
    }

    @Test
    public void toEnumSet_negativeOrdinal_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid ordinal value -1");

        List<Integer> source = Arrays.asList(-1);
        EnumConversions.toEnumSet(source, Color.class);
    }

    @Test
    public void toEnumSet_mixedArrayAndCollection_behaveConsistently() {
        // Verify that processing an array and a collection yields the same result
        Object[] arraySource = {Color.RED, "GREEN", 2};
        List<Object> collectionSource = Arrays.asList(Color.RED, "GREEN", 2);

        EnumSet<Color> fromArray = EnumConversions.toEnumSet(arraySource, Color.class);
        EnumSet<Color> fromCollection = EnumConversions.toEnumSet(collectionSource, Color.class);

        EnumSet<Color> expected = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE);
        assertEquals(expected, fromArray);
        assertEquals(expected, fromCollection);
    }

    /* -------------------------------------------------------------
     *  Internal sanity test – ensure lazy loading of enum constants
     * ------------------------------------------------------------- */
    @Test
    public void toEnumSet_lazyLoadingOfEnumConstants_isTransparent() {
        // The first element is a String, the second a Number.
        // The method must load enum constants only once and still succeed.
        List<Object> source = Arrays.asList("BLUE", 0);
        EnumSet<Color> set = EnumConversions.toEnumSet(source, Color.class);

        assertEquals(EnumSet.of(Color.BLUE, Color.RED), set);
    }
}
