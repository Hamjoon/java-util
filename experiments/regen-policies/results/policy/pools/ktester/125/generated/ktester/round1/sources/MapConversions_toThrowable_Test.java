package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toThrowable_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not require any special configuration for these tests.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  Helper to build the map that will be passed to toThrowable()
     * ------------------------------------------------------------- */
    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    /* -------------------------------------------------------------
     *  Parameterised test for the remaining simple cases (optional)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> simpleCases() {
        return Stream.of(Arguments.of(map(MapConversions.MESSAGE, "default target"), RuntimeException.class, RuntimeException.class, "default target", null), Arguments.of(map(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "derived class"), RuntimeException.class, IllegalArgumentException.class, "derived class", null), Arguments.of(map(MapConversions.CLASS, Exception.class.getName(), MapConversions.MESSAGE, "less derived"), RuntimeException.class, RuntimeException.class, "less derived", null));
    }

    /**
     * Verify that when the map supplies a concrete exception class name,
     * the returned instance is of that class (even if the target class is a
     * superclass) and that the message and cause are correctly set.
     */
    @Test
    void toThrowable_usesClassFromMap_andSetsMessageAndCause() {
        // Arrange – map that describes an IllegalArgumentException with a cause
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, IllegalArgumentException.class.getName());
        map.put(MapConversions.MESSAGE, "main exception");
        map.put(MapConversions.CAUSE, NullPointerException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "root cause");
        // Act
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        // Assert
        assertTrue(result instanceof IllegalArgumentException, "Expected the concrete class from the map to be used");
        assertEquals("main exception", result.getMessage());
        assertNotNull(result.getCause());
        assertTrue(result.getCause() instanceof NullPointerException, "Cause should be a NullPointerException");
        assertEquals("root cause", result.getCause().getMessage());
        // The implementation clears the stack trace.
        assertEquals(0, result.getStackTrace().length, "Stack trace should be cleared by MapConversions");
    }

    /**
     * Verify that when the map does not contain a {@code class} entry,
     * the supplied {@code target} class is used.
     */
    @Test
    void toThrowable_noClassInMap_usesTargetClass() {
        // Arrange – map without a class entry, but with a message
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.MESSAGE, "just a message");
        // Act
        Throwable result = MapConversions.toThrowable(map, converter, IllegalStateException.class);
        // Assert
        assertTrue(result instanceof IllegalStateException, "Expected the target class to be used when no class entry is present");
        assertEquals("just a message", result.getMessage());
        assertNull(result.getCause());
    }

    /**
     * Verify that {@code detailMessage} is used when {@code message} is absent.
     */
    @Test
    void toThrowable_usesDetailMessageWhenMessageMissing() {
        // Arrange – map with only a detailMessage entry
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DETAIL_MESSAGE, "detail only");
        // Act
        Throwable result = MapConversions.toThrowable(map, converter, Exception.class);
        // Assert
        assertEquals("detail only", result.getMessage(), "detailMessage should be used as the constructor argument when message is missing");
    }

    /**
     * Verify that a cause supplied in the map but not accepted by the exception's
     * constructor is still attached via {@code initCause}.
     */
    @Test
    void toThrowable_causeNotInConstructor_isAttachedViaInitCause() {
        // Arrange – use an exception type that only has a (String) constructor (e.g., IllegalArgumentException)
        // and supply a cause that will be ignored by that constructor.
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, IllegalArgumentException.class.getName());
        map.put(MapConversions.MESSAGE, "msg");
        map.put(MapConversions.CAUSE, IllegalStateException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "cause msg");
        // Act
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        // Assert
        assertTrue(result instanceof IllegalArgumentException);
        assertEquals("msg", result.getMessage());
        assertNotNull(result.getCause());
        assertTrue(result.getCause() instanceof IllegalStateException);
        assertEquals("cause msg", result.getCause().getMessage());
    }

    /**
     * Verify that an illegal map (e.g., missing required keys) results in an
     * {@link IllegalArgumentException} as defined by the implementation.
     */
    @Test
    void toThrowable_invalidMap_throwsIllegalArgumentException() {
        // Arrange – map that cannot be cast to Map<String,Object> (null or wrong type)
        Object notAMap = "I am not a map";
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(notAMap, converter, RuntimeException.class));
        assertTrue(ex.getMessage().contains("Unable to reconstruct exception instance from map"));
    }

    /* -------------------------------------------------------------
     *  1. Class selection tests
     * ------------------------------------------------------------- */
    @Test
    void classSelection_defaultTargetWhenNoClassEntry() {
        Map<String, Object> map = map(MapConversions.MESSAGE, "default target");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(t instanceof RuntimeException);
        assertEquals("default target", t.getMessage());
        assertNull(t.getCause());
        assertEquals(0, t.getStackTrace().length);
    }

    @Test
    void classSelection_mapClassMoreDerivedThanTarget() {
        Map<String, Object> map = map(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "derived class");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("derived class", t.getMessage());
        assertEquals(0, t.getStackTrace().length);
    }

    @Test
    void classSelection_mapClassLessDerivedThanTarget() {
        Map<String, Object> map = map(MapConversions.CLASS, Exception.class.getName(), MapConversions.MESSAGE, "less derived");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        // target (RuntimeException) is more specific than Exception
        assertTrue(t instanceof RuntimeException);
        assertEquals("less derived", t.getMessage());
    }

    @Test
    void classSelection_unknownClassNameFallsBackToTarget() {
        Map<String, Object> map = map(MapConversions.CLASS, "non.existent.FooBar", MapConversions.MESSAGE, "unknown class");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(t instanceof RuntimeException);
        assertEquals("unknown class", t.getMessage());
    }

    @Test
    void classSelection_classEntryMatchesTargetExactly() {
        Map<String, Object> map = map(MapConversions.CLASS, RuntimeException.class.getName(), MapConversions.MESSAGE, "exact match");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(t instanceof RuntimeException);
        assertEquals("exact match", t.getMessage());
    }

    /* -------------------------------------------------------------
     *  2. Message handling tests
     * ------------------------------------------------------------- */
    @Test
    void messageHandling_detailMessageUsedWhenMessageMissing() {
        Map<String, Object> map = map(MapConversions.DETAIL_MESSAGE, "detail only");
        Throwable t = MapConversions.toThrowable(map, converter, Exception.class);
        assertEquals("detail only", t.getMessage());
    }

    @Test
    void messageHandling_messageTakesPrecedenceOverDetailMessage() {
        Map<String, Object> map = map(MapConversions.MESSAGE, "primary", MapConversions.DETAIL_MESSAGE, "secondary");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertEquals("primary", t.getMessage());
    }

    @Test
    void messageHandling_noMessageKeysResultsInNullMessage() {
        Map<String, Object> map = new HashMap<>();
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertNull(t.getMessage());
    }

    @Test
    void messageHandling_messageNullUsesDetailMessage() {
        Map<String, Object> map = map(MapConversions.MESSAGE, null, MapConversions.DETAIL_MESSAGE, "fallback detail");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertEquals("fallback detail", t.getMessage());
    }

    /* -------------------------------------------------------------
     *  3. Cause handling tests
     * ------------------------------------------------------------- */
    @Test
    void causeHandling_constructorAcceptsStringAndThrowable() {
        Map<String, Object> map = map(MapConversions.MESSAGE, "main exception", MapConversions.CAUSE, NullPointerException.class.getName(), MapConversions.CAUSE_MESSAGE, "null pointer");
        Throwable t = MapConversions.toThrowable(map, converter, IllegalArgumentException.class);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("main exception", t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof NullPointerException);
        assertEquals("null pointer", t.getCause().getMessage());
    }

    @Test
    void causeHandling_initCauseFallbackWhenConstructorDoesNotAcceptCause() {
        // IllegalArgumentException only has (String) and (String, Throwable) constructors.
        // We'll supply a cause that will be created but not passed to the main ctor.
        Map<String, Object> map = map(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "msg", MapConversions.CAUSE, IllegalStateException.class.getName(), MapConversions.CAUSE_MESSAGE, "cause msg");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("msg", t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof IllegalStateException);
        assertEquals("cause msg", t.getCause().getMessage());
    }

    @Test
    void causeHandling_missingCauseMessageCreatesCauseWithNullMessage() {
        Map<String, Object> map = map(MapConversions.MESSAGE, "main exception", MapConversions.CAUSE, IllegalStateException.class.getName());
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertEquals("main exception", t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof IllegalStateException);
        assertNull(t.getCause().getMessage());
    }

    @Test
    void causeHandling_unknownCauseClassNameResultsInNoCause() {
        Map<String, Object> map = map(MapConversions.MESSAGE, "main exception", MapConversions.CAUSE, "non.existent.Cause", MapConversions.CAUSE_MESSAGE, "ignored");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertEquals("main exception", t.getMessage());
        assertNull(t.getCause());
    }

    @Test
    void causeHandling_onlyCauseNoMessage() {
        Map<String, Object> map = map(MapConversions.CAUSE, NullPointerException.class.getName(), MapConversions.CAUSE_MESSAGE, "null pointer");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        // RuntimeException has a (Throwable) constructor, so the cause becomes the sole argument.
        assertNull(t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof NullPointerException);
        assertEquals("null pointer", t.getCause().getMessage());
    }

    @Test
    void causeHandling_causeInstantiationFailurePropagatesAsIllegalArgumentException() {
        // Simulate a class with a private constructor – we use java.lang.System (no public ctor)
        Map<String, Object> map = map(MapConversions.MESSAGE, "main", MapConversions.CAUSE, System.class.getName(), MapConversions.CAUSE_MESSAGE, "msg");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, converter, RuntimeException.class));
        assertTrue(ex.getMessage().contains("Unable to reconstruct exception instance from map"));
    }

    /* -------------------------------------------------------------
     *  4. Stack‑trace clearing test
     * ------------------------------------------------------------- */
    @Test
    void stackTraceIsCleared() {
        Map<String, Object> map = map(MapConversions.MESSAGE, "stack cleared");
        Throwable t = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertEquals(0, t.getStackTrace().length);
    }

    /* -------------------------------------------------------------
     *  5. Error handling – illegal map content
     * ------------------------------------------------------------- */
    @Test
    void errorHandling_illegalMapContentThrowsIllegalArgumentException() {
        Object notAMap = "I am not a map";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(notAMap, converter, RuntimeException.class));
        assertTrue(ex.getMessage().contains("Unable to reconstruct exception instance from map"));
    }

    /* -------------------------------------------------------------
     *  6. populateFields failure – unknown field
     * ------------------------------------------------------------- */
    @Test
    void populateFields_unknownFieldTriggersIllegalArgumentException() {
        Map<String, Object> map = map(MapConversions.CLASS, RuntimeException.class.getName(), MapConversions.MESSAGE, "field error", "nonExistentField", "some value");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, converter, RuntimeException.class));
        assertTrue(ex.getMessage().contains("Unable to reconstruct exception instance from map"));
    }

    @ParameterizedTest
    @MethodSource("simpleCases")
    void parameterisedSimpleCases(Map<String, Object> map, Class<?> target, Class<?> expectedClass, String expectedMessage, Throwable expectedCause) {
        Throwable t = MapConversions.toThrowable(map, converter, target);
        assertEquals(expectedClass, t.getClass());
        assertEquals(expectedMessage, t.getMessage());
        assertEquals(expectedCause, t.getCause());
    }
}
