package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringBufferConversionsTest {

    /**
     * Basic functionality test: verifies that {@code toString} returns the
     * result of {@code Object#toString()} regardless of the supplied
     * {@link Converter} instance.
     */
    @Test
    public void testToString_basic() {
        // Arrange
        Object source = new Object() {
            @Override
            public String toString() {
                return "customString";
            }
        };
        Converter dummyConverter = new Converter(new ConverterOptions());

        // Act
        String result = StringBufferConversions.toString(source, dummyConverter);

        // Assert
        assertEquals("customString", result);
    }

    /**
     * Edge‑case test: the source object is a {@link StringBuilder} (a mutable
     * character sequence). The method should still return the same string
     * that {@code StringBuilder#toString()} would produce.
     */
    @Test
    public void testToString_withStringBuilder() {
        // Arrange
        StringBuilder sb = new StringBuilder();
        sb.append("builder");
        Converter converter = new Converter(new ConverterOptions());

        // Act
        String result = StringBufferConversions.toString(sb, converter);

        // Assert
        assertEquals("builder", result);
    }

    /**
     * Edge‑case test: the source object is {@code null}. The method under test
     * does not perform a null‑check, so a {@link NullPointerException} is
     * expected.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_nullSource() {
        // Arrange
        Object source = null;
        Converter converter = new Converter(new ConverterOptions());

        // Act
        StringBufferConversions.toString(source, converter);

        // No assert – expecting NPE
    }

    /**
     * Edge‑case test: the supplied {@link Converter} instance is {@code null}.
     * The implementation does not use the converter, therefore the call
     * should succeed and simply return {@code from.toString()}.
     */
    @Test
    public void testToString_nullConverter() {
        // Arrange
        Object source = new Object() {
            @Override
            public String toString() {
                return "noConverter";
            }
        };
        Converter converter = null;

        // Act
        String result = StringBufferConversions.toString(source, converter);

        // Assert
        assertEquals("noConverter", result);
    }

    /**
     * Edge‑case test: the source object's {@code toString()} returns {@code null}.
     * According to the contract of {@code Object#toString()}, this is unusual but
     * possible for user‑defined classes. The method should propagate the {@code null}
     * value without throwing.
     */
    @Test
    public void testToString_toStringReturnsNull() {
        // Arrange
        Object source = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
        Converter converter = new Converter(new ConverterOptions());

        // Act
        String result = StringBufferConversions.toString(source, converter);

        // Assert
        assertNull(result);
    }
}
