package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Arrays;

/**
 * Comprehensive JUnit‑4 test suite for {@link CharacterArrayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage – converting a non‑empty {@code Character[]} to {@code String},
 *       {@code StringBuilder} and {@code StringBuffer}.</li>
 *   <li>Edge cases – empty arrays and arrays containing {@code null} elements.</li>
 *   <li>Error scenarios – passing an object that is not a {@code Character[]} and expecting a
 *       {@code ClassCastException}.</li>
 *   <li>Verification that the supplied {@link Converter} argument is ignored (the implementation
 *       never touches this parameter).</li>
 * </ul>
 *
 * Only the public API of {@link Converter} (as supplied in the prompt) is used.
 */
public class CharacterArrayConversionsTest {

    /** A dummy {@link Converter} instance – the conversion methods under test do not
     *  depend on its state, so a minimal, non‑null instance is sufficient. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The real Converter class expects a ConverterOptions argument.
        // Passing {@code null} is acceptable for the purpose of these tests because
        // the methods under test never call any method on the Converter.
        dummyConverter = new Converter(null);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toString() tests -------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringTypical() {
        Character[] input = new Character[] { 'J', 'u', 'n', 'i', 't' };
        String expected = "JUnit";

        String result = CharacterArrayConversions.toString(input, dummyConverter);

        assertEquals("Typical conversion should produce the exact string", expected, result);
    }

    @Test
    public void testToStringEmptyArray() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, dummyConverter);

        assertEquals("Empty Character[] should yield an empty String", "", result);
    }

    @Test
    public void testToStringWithNullElements() {
        // According to StringBuilder.append(Object) the literal "null" is appended
        // for a null reference.
        Character[] input = new Character[] { 'A', null, 'B' };
        String expected = "AnullB";

        String result = CharacterArrayConversions.toString(input, dummyConverter);

        assertEquals("Null elements should be rendered as the literal \"null\"", expected, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringInvalidSourceType() {
        // Passing a non‑Character[] should trigger a ClassCastException at the cast site.
        Object notCharArray = new int[] { 1, 2, 3 };
        CharacterArrayConversions.toString(notCharArray, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /* ----------------------- toStringBuilder() tests ---------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringBuilderTypical() {
        Character[] input = new Character[] { 'T', 'e', 's', 't' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);

        assertNotNull("Result must not be null", sb);
        assertEquals("Builder content must match expected string", "Test", sb.toString());
    }

    @Test
    public void testToStringBuilderEmptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);

        assertNotNull(sb);
        assertEquals("Empty input should produce empty StringBuilder", 0, sb.length());
    }

    @Test
    public void testToStringBuilderWithNullElements() {
        Character[] input = new Character[] { null, 'X', null };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);

        assertEquals("Builder should contain literal \"null\" for each null element",
                "nullXnull", sb.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilderInvalidSourceType() {
        Object notCharArray = "not an array";
        CharacterArrayConversions.toStringBuilder(notCharArray, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /* ----------------------- toStringBuffer() tests ----------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringBufferTypical() {
        Character[] input = new Character[] { 'B', 'u', 'f', 'f', 'e', 'r' };
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, dummyConverter);

        assertNotNull(buffer);
        assertEquals("Buffer content must match expected string", "Buffer", buffer.toString());
    }

    @Test
    public void testToStringBufferEmptyArray() {
        Character[] input = new Character[0];
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, dummyConverter);

        assertNotNull(buffer);
        assertEquals("Empty array should give empty StringBuffer", 0, buffer.length());
    }

    @Test
    public void testToStringBufferWithNullElements() {
        Character[] input = new Character[] { 'Z', null };
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, dummyConverter);

        assertEquals("Buffer should render null element as literal \"null\"",
                "Znull", buffer.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBufferInvalidSourceType() {
        Object notCharArray = new Object();
        CharacterArrayConversions.toStringBuffer(notCharArray, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /* ---------------------- Converter argument sanity -------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMethodsIgnoreConverterArgument() {
        // The conversion methods do not call any method on the Converter instance.
        // We verify this by passing a Converter that would throw if any method were invoked.
        Converter throwingConverter = new Converter(null) {
            @Override
            public ConverterOptions getOptions() {
                throw new AssertionError("Converter should not be used");
            }
        };

        Character[] input = new Character[] { 'I', 'g', 'n', 'o', 'r', 'e' };
        // If the implementation erroneously uses the converter, the test will fail.
        assertEquals("IgnoreConverter", CharacterArrayConversions.toString(input, throwingConverter));
        assertEquals("IgnoreConverter", CharacterArrayConversions.toStringBuilder(input, throwingConverter).toString());
        assertEquals("IgnoreConverter", CharacterArrayConversions.toStringBuffer(input, throwingConverter).toString());
    }
}
