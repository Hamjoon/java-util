package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toFloatZero_Test {

    @Mock
    private Converter // Converter is not used by the method, but required for the signature
    mockConverter;

    private static Stream<Arguments> provideTestCases() {
        List<Arguments> args = new ArrayList<>();
        // 1. Both arguments null (converter will be mocked, so we pass null here)
        args.add(Arguments.of(null, "Both arguments are null – method should ignore inputs and always return Float zero."));
        // 2. Integer value
        args.add(Arguments.of(123, "Integer value supplied – method must still return Float zero."));
        // 3. String representation of a number
        args.add(Arguments.of("123.45", "String representation of a number – method should not attempt conversion and return Float zero."));
        // 4. Double primitive
        args.add(Arguments.of(12.34, "Double primitive supplied – method must still return Float zero."));
        // 5. Scientific notation double
        args.add(Arguments.of(560.0, "Scientific notation double – method should ignore and return Float zero."));
        // 6. Boolean value
        args.add(Arguments.of(Boolean.TRUE, "Boolean value – method must still return Float zero."));
        // 7. BigDecimal
        args.add(Arguments.of(new BigDecimal("123.456"), "Complex object (BigDecimal) – method should not inspect it and return Float zero."));
        // 8. java.util.Date (UTC instant)
        args.add(Arguments.of(Date.from(Instant.parse("2023-01-01T00:00:00Z")), "Date object – method must still return Float zero."));
        // 9. Arbitrary custom object
        args.add(Arguments.of(new Object(), "Arbitrary custom object – method should ignore it and return Float zero."));
        // 10. Float primitive zero
        args.add(Arguments.of(0.0f, "Float primitive zero – method must still return Float zero."));
        // 11. Float NaN
        args.add(Arguments.of(Float.NaN, "Float NaN – method ignores special float values and returns Float zero."));
        // 12. Float positive infinity
        args.add(Arguments.of(Float.POSITIVE_INFINITY, "Float positive infinity – method still returns Float zero."));
        // 13. Long object
        args.add(Arguments.of(Long.valueOf(123L), "Long object – method must ignore and return Float zero."));
        // 14. Short object
        args.add(Arguments.of(Short.valueOf((short) 5), "Short object – method must ignore and return Float zero."));
        // 15. Byte object
        args.add(Arguments.of(Byte.valueOf((byte) 2), "Byte object – method must ignore and return Float zero."));
        // 16. Character object
        args.add(Arguments.of('a', "Character object – method must ignore and return Float zero."));
        // 17. int array
        args.add(Arguments.of(new int[] { 1, 2, 3 }, "Array of ints – method must ignore and return Float zero."));
        // 18. List instance
        args.add(Arguments.of(Arrays.asList(1, 2), "List instance – method must ignore and return Float zero."));
        // 19. Map instance
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        args.add(Arguments.of(map, "Map instance – method must ignore and return Float zero."));
        // 20. BigInteger object
        args.add(Arguments.of(new BigInteger("12345678901234567890"), "BigInteger object – method must ignore and return Float zero."));
        // 21. LocalDate instance
        args.add(Arguments.of(LocalDate.of(2023, 5, 1), "LocalDate instance – method must ignore and return Float zero."));
        // 22. LocalDateTime instance
        args.add(Arguments.of(LocalDateTime.of(2023, 5, 1, 12, 34, 56), "LocalDateTime instance – method must ignore and return Float zero."));
        // 23. Instant instance
        args.add(Arguments.of(Instant.parse("2023-05-01T12:34:56Z"), "Instant instance – method must ignore and return Float zero."));
        // 24. Timestamp instance
        args.add(Arguments.of(Timestamp.valueOf("2023-05-01 12:34:56.0"), "Timestamp instance – method must ignore and return Float zero."));
        // 25. AtomicInteger
        args.add(Arguments.of(new AtomicInteger(5), "AtomicInteger – method must ignore and return Float zero."));
        // 26. AtomicLong
        args.add(Arguments.of(new AtomicLong(10L), "AtomicLong – method must ignore and return Float zero."));
        // 27. AtomicBoolean
        args.add(Arguments.of(new AtomicBoolean(true), "AtomicBoolean – method must ignore and return Float zero."));
        // 28. Enum constant (define a simple local enum for the test)
        args.add(Arguments.of(TestEnum.A, "Enum constant – method must ignore and return Float zero."));
        // 29. Non‑null converter instance (the mock will be used, but we document the intent)
        args.add(Arguments.of("any string", "Non‑null converter instance – method still returns Float zero irrespective of converter."));
        return args.stream();
    }

    @Test
    void toFloatZero_returnsZero_forNullInput() {
        Float result = NumberConversions.toFloatZero(null, mockConverter);
        assertEquals(0.0f, result, "toFloatZero should return 0.0f for null input");
    }

    @Test
    void toFloatZero_returnsZero_forVariousObjectTypes() {
        Object[] testValues = { // Integer
        123, // Double
        45.67, // String
        "99.99", // BigDecimal
        new java.math.BigDecimal("123.45"), // BigInteger
        new java.math.BigInteger("9876543210"), // Boolean
        true, // Arbitrary object
        new Object() };
        for (Object input : testValues) {
            Float result = NumberConversions.toFloatZero(input, mockConverter);
            assertEquals(0.0f, result, () -> "toFloatZero should return 0.0f for input: " + input.getClass().getSimpleName());
        }
    }

    @ParameterizedTest(name = "[{index}] from={0} ({1})")
    @MethodSource("provideTestCases")
    void toFloatZero_alwaysReturnsZero(Object from, String description) {
        // Act
        Float result = NumberConversions.toFloatZero(from, mockConverter);
        // Assert – always 0.0f irrespective of input
        assertEquals(0.0f, result, () -> "Expected 0.0f for case: " + description + " (input type: " + (from == null ? "null" : from.getClass().getSimpleName()) + ")");
    }
}
