package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_identity_Test {

    /**
     * A mock {@link Converter} that must not be used by the identity method.
     */
    @Mock
    private Converter mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not interacted with during the test.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Provides arguments for the "basic behavior" group.
     * Each {@link Arguments} contains:
     *   - a description (used only for readability in the test name)
     *   - the source object
     *   - a {@link Converter} to pass (may be {@code null})
     */
    private static Stream<Arguments> basicBehaviorProvider() {
        // non‑null converter used for many cases
        Converter realConverter = new Converter();
        return Stream.of(Arguments.of("String with non‑null converter", "Hello World", realConverter), Arguments.of("null source, non‑null converter", null, realConverter), Arguments.of("Integer with null converter", Integer.valueOf(123), null), Arguments.of("ArrayList instance", new ArrayList<>(), realConverter), Arguments.of("Date instance", new Date(0L), realConverter), Arguments.of("BigDecimal with null converter", new BigDecimal("12345.6789"), null), Arguments.of("HashMap instance", new HashMap<>(), realConverter), Arguments.of("AtomicLong instance", new AtomicLong(42L), realConverter));
    }

    /**
     * Provides arguments for the "additional cases" group.
     */
    private static Stream<Arguments> additionalCasesProvider() {
        Converter realConverter = new Converter();
        return Stream.of(Arguments.of("Both arguments null", null, null), Arguments.of("int[] array with non‑null converter", new int[] { 1, 2, 3 }, realConverter), Arguments.of("HashSet with null converter", new HashSet<>(Set.of("a", "b")), null), Arguments.of("TreeMap with non‑null converter", new TreeMap<>(Map.of(1, "one", 2, "two")), realConverter), Arguments.of("AtomicBoolean with null converter", new AtomicBoolean(true), null), Arguments.of("LocalDateTime immutable", LocalDateTime.of(2023, 5, 10, 12, 30), realConverter), Arguments.of("UUID with null converter", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), null), Arguments.of("Plain Object instance", new Object(), realConverter), Arguments.of("Converter as source", new Converter(), realConverter), Arguments.of("StringBuilder with null converter", new StringBuilder("test"), null));
    }

    @Test
    void identityReturnsSameStringInstance() {
        String original = "identity-test";
        String result = Converter.identity(original, mockConverter);
        assertSame(original, result, "The identity method should return the exact same String instance");
    }

    @Test
    void identityReturnsSameIntegerInstance() {
        Integer original = 42;
        Integer result = Converter.identity(original, mockConverter);
        assertSame(original, result, "The identity method should return the exact same Integer instance");
        assertEquals(original, result);
    }

    @Test
    void identityReturnsSameListInstance() {
        List<String> original = new ArrayList<>();
        original.add("a");
        original.add("b");
        List<String> result = Converter.identity(original, mockConverter);
        assertSame(original, result, "The identity method should return the exact same List instance");
        // Mutating the returned list should affect the original list, proving they are the same object.
        result.add("c");
        assertEquals(3, original.size());
        assertTrue(original.contains("c"));
    }

    @Test
    void identityHandlesNullFromValue() {
        Object result = Converter.identity(null, mockConverter);
        assertNull(result, "Identity of a null value should be null");
    }

    @Test
    void identityIgnoresConverterWhenConverterIsNull() {
        String original = "no-converter";
        // The method accepts a null Converter; it must still return the original value.
        String result = Converter.identity(original, null);
        assertSame(original, result, "Identity should work even when the Converter argument is null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicBehaviorProvider")
    void identityBasicBehavior(String description, Object source, Converter converter) {
        // Act
        Object result = Converter.identity(source, converter);
        // Assert – identity must return the exact same reference (or null)
        if (source == null) {
            assertNull(result, "Identity should return null when source is null");
        } else {
            assertSame(source, result, "Identity must return the original instance for: " + description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalCasesProvider")
    void identityAdditionalCases(String description, Object source, Converter converter) {
        Object result = Converter.identity(source, converter);
        if (source == null) {
            assertNull(result, "Identity should return null when source is null");
        } else {
            assertSame(source, result, "Identity must return the original instance for: " + description);
        }
    }
}
