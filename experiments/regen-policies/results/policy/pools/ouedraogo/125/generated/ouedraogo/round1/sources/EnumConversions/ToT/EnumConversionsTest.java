package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link EnumConversions}.
 *
 * The suite covers:
 * <ul>
 *   <li>Normal conversion of an {@code Enum} to a {@code Map}</li>
 *   <li>Successful creation of {@code EnumSet} from Collections and Arrays
 *       containing enum instances, names, ordinals and {@code null}s.</li>
 *   <li>All documented error paths (non‑enum target, unsupported source type,
 *       illegal element types, out‑of‑range ordinals).</li>
 * </ul>
 *
 * The {@code Converter} argument is required by the methods but is not used
 * in the current implementation, therefore a minimal instance created with a
 * {@code null} {@code ConverterOptions} is sufficient.
 */
public class EnumConversionsTest {

    /** Simple enum used throughout the tests. */
    private enum Color {
        RED, GREEN, BLUE
    }

    private Converter dummyConverter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Converter is not used by EnumConversions, null options are acceptable.
        dummyConverter = new Converter(null);
    }

    /* --------------------------------------------------------------------- *
     *                     Tests for toMap(..)                               *
     * --------------------------------------------------------------------- */

    @Test
    public void toMap_ShouldReturnMapWithNameKey() {
        // Arrange
        Color from = Color.GREEN;

        // Act
        Map<String, Object> result = EnumConversions.toMap(from, dummyConverter);

        // Assert
        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Map must contain key 'name'", result.containsKey("name"));
        assertEquals("Value of 'name' must be the enum name", "GREEN", result.get("name"));
    }

    /* --------------------------------------------------------------------- *
     *                     Tests for toEnumSet(..)                           *
     * --------------------------------------------------------------------- */

    @Test
    public void toEnumSet_FromCollectionOfEnumInstances() {
        Collection<Color> source = Arrays.asList(Color.RED, Color.BLUE);
        EnumSet<Color> result = EnumConversions.toEnumSet(source, dummyConverter, Color.class);
        assertEquals(EnumSet.of(Color.RED, Color.BLUE), result);
    }

    @Test
    public void toEnumSet_FromCollectionOfNames() {
        Collection<String> source = Arrays.asList("RED", "GREEN");
        EnumSet<Color> result = EnumConversions.toEnumSet(source, dummyConverter, Color.class);
        assertEquals(EnumSet.of(Color.RED, Color.GREEN), result);
    }

    @Test
    public void toEnumSet_FromCollectionOfOrdinals() {
        Collection<Integer> source = Arrays.asList(0, 2); // RED (0), BLUE (2)
        EnumSet<Color> result = EnumConversions.toEnumSet(source, dummyConverter, Color.class);
        assertEquals(EnumSet.of(Color.RED, Color.BLUE), result);
    }

    @Test
    public void toEnumSet_FromArray_MixedTypesIncludingNulls() {
        Object[] source = new Object[] { Color.GREEN, "RED", 2, null };
        EnumSet<Color> result = EnumConversions.toEnumSet(source, dummyConverter, Color.class);
        assertEquals(EnumSet.of(Color.RED, Color.GREEN, Color.BLUE), result);
    }

    @Test
    public void toEnumSet_ShouldIgnoreNullElements() {
        List<Object> source = Arrays.asList(null, "BLUE", null);
        EnumSet<Color> result = EnumConversions.toEnumSet(source, dummyConverter, Color.class);
        assertEquals(EnumSet.of(Color.BLUE), result);
    }

    @Test
    public void toEnumSet_TargetNotEnum_ShouldThrow() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be an Enum");

        List<String> source = Collections.singletonList("RED");
        EnumConversions.toEnumSet(source, dummyConverter, String.class);
    }

    @Test
    public void toEnumSet_SourceNotCollectionOrArray_ShouldThrow() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Source must be a Collection or Array");

        // Plain String is neither a Collection nor an array
        EnumConversions.toEnumSet("RED", dummyConverter, Color.class);
    }

    @Test
    public void toEnumSet_UnsupportedElementType_ShouldThrow() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("java.lang.Boolean");

        List<Object> source = Arrays.asList(true, "RED");
        EnumConversions.toEnumSet(source, dummyConverter, Color.class);
    }

    @Test
    public void toEnumSet_OrdinalOutOfRange_Negative_ShouldThrow() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid ordinal value -1");

        List<Integer> source = Collections.singletonList(-1);
        EnumConversions.toEnumSet(source, dummyConverter, Color.class);
    }

    @Test
    public void toEnumSet_OrdinalOutOfRange_TooLarge_ShouldThrow() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid ordinal value 3");

        List<Integer> source = Collections.singletonList(3); // enum has indices 0..2
        EnumConversions.toEnumSet(source, dummyConverter, Color.class);
    }

    /* --------------------------------------------------------------------- *
     *                     Direct tests of internal helpers (optional)       *
     * --------------------------------------------------------------------- */
    // The internal private helpers are exercised indirectly via the public
    // toEnumSet method.  No additional reflection‑based tests are required.

}
