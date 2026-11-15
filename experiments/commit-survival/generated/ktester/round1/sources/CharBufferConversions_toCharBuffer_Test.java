package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toCharBuffer_Test {

    @Mock
    private Converter // Converter is not used by the method but required for the signature
    mockConverter;

    /**
     * Dummy converter implementation – used only to verify that the argument is ignored.
     */
    private static class DummyConverter implements Converter {
        // No implementation needed for the tests
    }

    /* ---------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final CharBuffer from;

        final Converter converter;

        final String description;

        ArgumentsHolder(CharBuffer from, Converter converter, String description) {
            this.from = from;
            this.converter = converter;
            this.description = description;
        }
    }

    /**
     * Supplies normal‑behavior test cases (valid CharBuffer inputs).
     */
    private static Stream<ArgumentsHolder> normalBehaviorProvider() {
        return Stream.of(// case 1 – simple mutable buffer, null converter
        new ArgumentsHolder(CharBuffer.wrap(new char[] { 'a', 'b', 'c' }), null, "CharBuffer that is read‑only, contains ['a','b','c'], and has the same position (0) and limit (3) as the original buffer"), // case 2 – pre‑filled, flipped buffer, non‑null converter
        new ArgumentsHolder(CharBuffer.allocate(5).put("hello".toCharArray()).flip(), new DummyConverter(), "Read‑only CharBuffer with content \"hello\", position 0, limit 5"));
    }

    /**
     * Supplies error‑handling test cases.
     */
    private static Stream<ArgumentsHolder> errorHandlingProvider() {
        return Stream.of(// null "from"
        new ArgumentsHolder(null, null, "NullPointerException"), // non‑CharBuffer String
        new ArgumentsHolder("plain string", null, "ClassCastException"), // non‑CharBuffer ByteBuffer
        new ArgumentsHolder(java.nio.ByteBuffer.allocate(4), new DummyConverter(), "ClassCastException"));
    }

    /**
     * Supplies behavior‑specific test cases.
     */
    private static Stream<ArgumentsHolder> behaviorProvider() {
        return Stream.of(// already read‑only buffer – should return same instance
        new ArgumentsHolder(CharBuffer.wrap(new char[] { 'x', 'y' }).asReadOnlyBuffer(), null, "sameInstance"), // mutation of original buffer reflected in read‑only view
        new ArgumentsHolder(CharBuffer.allocate(4).put(new char[] { 'a', 'b', 'c', 'd' }).flip(), null, "mutationReflected"), // original buffer position/limit unchanged after conversion
        new ArgumentsHolder(CharBuffer.allocate(10).position(3).limit(8), null, "positionUnchanged"), // converter argument ignored
        new ArgumentsHolder(CharBuffer.wrap(new char[] { 'm', 'n', 'o' }), new DummyConverter(), "converterIgnored"));
    }

    @Test
    void toCharBuffer_returnsReadOnlyBufferWithSameContent() {
        // GIVEN
        CharBuffer original = CharBuffer.allocate(10);
        original.put("abcdefghij");
        // position = 0, limit = 10
        original.flip();
        // move position to 3, remaining = 7
        original.position(3);
        // WHEN
        CharBuffer result = CharBufferConversions.toCharBuffer(original, mockConverter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertTrue(result.isReadOnly(), "Resulting CharBuffer must be read‑only");
        assertEquals(original.position(), 3, "Original buffer's position must remain unchanged");
        assertEquals(original.limit(), result.limit(), "Limit of the read‑only buffer should match original");
        assertEquals(original.remaining(), result.remaining(), "Remaining characters should be identical");
        // Verify content equality
        // duplicate shares content but not read‑only flag
        CharBuffer copyOfOriginal = original.duplicate();
        while (copyOfOriginal.hasRemaining()) {
            assertEquals(copyOfOriginal.get(), result.get(), "Characters in the buffers must be equal");
        }
    }

    @Test
    void toCharBuffer_readOnlyBufferThrowsOnWrite() {
        // GIVEN
        CharBuffer original = CharBuffer.wrap("test".toCharArray());
        // WHEN
        CharBuffer result = CharBufferConversions.toCharBuffer(original, mockConverter);
        // THEN
        assertTrue(result.isReadOnly(), "Resulting CharBuffer must be read‑only");
        assertThrows(ReadOnlyBufferException.class, () -> result.put('X'), "Attempting to modify a read‑only CharBuffer should throw ReadOnlyBufferException");
    }

    @Test
    void toCharBuffer_withNonCharBufferThrowsClassCastException() {
        // GIVEN
        Object notACharBuffer = "I am a String, not a CharBuffer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharBufferConversions.toCharBuffer(notACharBuffer, mockConverter), "Passing a non‑CharBuffer object should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] Normal behavior: {2}")
    @MethodSource("normalBehaviorProvider")
    void toCharBuffer_normalBehavior(CharBuffer source, Converter converter, String description) {
        // GIVEN – source buffer already prepared in the provider
        // WHEN
        CharBuffer result = CharBufferConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertTrue(result.isReadOnly(), "Result must be a read‑only buffer");
        // The read‑only view shares the same underlying data, so content must match
        // duplicate keeps mutable flag
        CharBuffer dup = source.duplicate();
        while (dup.hasRemaining()) {
            assertEquals(dup.get(), result.get(), "Content of the read‑only view must be identical to the original");
        }
        // Verify that the original buffer's position and limit are unchanged
        assertEquals(source.position(), source.position(), "Original buffer's position must remain unchanged");
        assertEquals(source.limit(), result.limit(), "Resulting buffer limit must match original limit");
    }

    @ParameterizedTest(name = "[{index}] Error handling: expect {2}")
    @MethodSource("errorHandlingProvider")
    void toCharBuffer_errorHandling(Object source, Converter converter, String expectedException) {
        // WHEN / THEN
        if ("NullPointerException".equals(expectedException)) {
            assertThrows(NullPointerException.class, () -> CharBufferConversions.toCharBuffer(source, converter), "Null source should trigger NullPointerException");
        } else if ("ClassCastException".equals(expectedException)) {
            assertThrows(ClassCastException.class, () -> CharBufferConversions.toCharBuffer(source, converter), "Non‑CharBuffer source should trigger ClassCastException");
        } else {
            fail("Unexpected expectedException value: " + expectedException);
        }
    }

    @ParameterizedTest(name = "[{index}] Behavior: {2}")
    @MethodSource("behaviorProvider")
    void toCharBuffer_behavior(CharBuffer source, Converter converter, String scenario) {
        switch(scenario) {
            case "sameInstance" ->
                {
                    // GIVEN a read‑only buffer
                    CharBuffer result = CharBufferConversions.toCharBuffer(source, converter);
                    assertSame(source, result, "When the source is already read‑only, the same instance should be returned");
                }
            case "mutationReflected" ->
                {
                    // GIVEN a mutable buffer
                    CharBuffer result = CharBufferConversions.toCharBuffer(source, converter);
                    assertTrue(result.isReadOnly(), "Result must be read‑only");
                    // Mutate original buffer at position 0
                    source.position(0);
                    source.put('z');
                    // The read‑only view should see the mutation
                    result.position(0);
                    assertEquals('z', result.get(), "Mutation of the original buffer must be visible in the read‑only view");
                }
            case "positionUnchanged" ->
                {
                    // Set up explicit position/limit
                    source.position(3);
                    source.limit(8);
                    CharBuffer result = CharBufferConversions.toCharBuffer(source, converter);
                    // Original buffer state must stay the same
                    assertEquals(3, source.position(), "Original position must stay 3");
                    assertEquals(8, source.limit(), "Original limit must stay 8");
                    // Resulting buffer starts at position 0 and its limit equals (limit - position) of original
                    assertEquals(0, result.position(), "Result buffer should start at position 0");
                    assertEquals(5, result.limit(), "Result limit should be original limit - original position (8-3 = 5)");
                }
            case "converterIgnored" ->
                {
                    CharBuffer result = CharBufferConversions.toCharBuffer(source, converter);
                    assertTrue(result.isReadOnly(), "Result must be read‑only");
                    // Verify content matches source
                    CharBuffer dup = source.duplicate();
                    while (dup.hasRemaining()) {
                        assertEquals(dup.get(), result.get(), "Content must be identical regardless of converter presence");
                    }
                }
            default ->
                fail("Unknown scenario: " + scenario);
        }
    }
}
