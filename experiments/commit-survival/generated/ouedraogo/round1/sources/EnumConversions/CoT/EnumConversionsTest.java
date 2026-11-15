package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Map;

/**
 * Unit tests for {@link EnumConversions}.
 */
public class EnumConversionsTest {

    /** Simple enum used for testing. */
    private enum SampleEnum {
        ONE,
        TWO
    }

    /**
     * Verify that a valid enum instance is converted to a map containing the enum name.
     */
    @Test
    public void testToMapWithValidEnum() {
        // Arrange
        SampleEnum enumValue = SampleEnum.ONE;
        Converter dummyConverter = null; // converter is not used by the method

        // Act
        Map result = EnumConversions.toMap(enumValue, dummyConverter);

        // Assert
        assertNotNull("Resulting map should not be null", result);
        // The implementation uses CompactLinkedMap, verify the concrete type
        assertTrue("Resulting map should be an instance of CompactLinkedMap",
                result instanceof CompactLinkedMap);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain the key 'name'", result.containsKey("name"));
        assertEquals("The value associated with 'name' should be the enum constant name",
                enumValue.name(), result.get("name"));
    }

    /**
     * Verify that passing {@code null} as the source object results in a {@link NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapWithNullFromThrowsNullPointerException() {
        // Arrange
        Object nullObject = null;
        Converter dummyConverter = null;

        // Act
        EnumConversions.toMap(nullObject, dummyConverter);

        // No Assert – the test passes if NullPointerException is thrown
    }

    /**
     * Verify that passing a non‑enum object results in a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapWithNonEnumThrowsClassCastException() {
        // Arrange
        String notAnEnum = "I am not an enum";
        Converter dummyConverter = null;

        // Act
        EnumConversions.toMap(notAnEnum, dummyConverter);

        // No Assert – the test passes if ClassCastException is thrown
    }

    /**
     * Verify that the method does not depend on the {@link Converter} argument.
     * Supplying a real Converter instance should not affect the result.
     */
    @Test
    public void testToMapIgnoresConverterParameter() {
        // Arrange
        SampleEnum enumValue = SampleEnum.TWO;
        // Create a real Converter (its internal behavior is irrelevant for this test)
        ConverterOptions options = new ConverterOptions();
        Converter realConverter = new Converter(options);

        // Act
        Map result = EnumConversions.toMap(enumValue, realConverter);

        // Assert
        assertNotNull("Resulting map should not be null", result);
        assertEquals("The map should contain the enum name 'TWO'",
                "TWO", result.get("name"));
    }
}
