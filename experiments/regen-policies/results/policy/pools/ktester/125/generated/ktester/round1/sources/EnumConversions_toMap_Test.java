package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EnumConversions_toMap_Test {

    @Mock
    Converter // Converter is not used by the method but we provide a mock
    mockConverter;

    /**
     * A dummy Converter implementation – the method never calls it.
     */
    private static class DummyConverter implements Converter {
        // No behaviour required
    }

    /**
     * A Converter that throws on any method call – used to prove the method never touches it.
     */
    private static class ExplodingConverter implements Converter {

        @Override
        public String toString() {
            throw new RuntimeException("ExplodingConverter should never be invoked");
        }
        // If Converter defines other methods, they should similarly throw.
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test does not interact with the converter
        verifyNoInteractions(mockConverter);
    }

    /**
     * Valid enum instances together with a Converter (null or a dummy).
     */
    private static Stream<Arguments> validEnumProvider() {
        return Stream.of(Arguments.of(SampleEnum.FIRST, null), Arguments.of(SampleEnum.SECOND, new DummyConverter()));
    }

    /**
     * Non‑enum inputs that should trigger a {@link ClassCastException}.
     */
    private static Stream<Arguments> nonEnumProvider() {
        return Stream.of(Arguments.of("just a string"), Arguments.of(Integer.valueOf(42)));
    }

    @Test
    void toMap_returnsMapContainingEnumName() {
        // given
        SampleEnum enumInstance = SampleEnum.ONE;
        // when
        Map<String, Object> result = EnumConversions.toMap(enumInstance, mockConverter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey("name"), "Map should contain the key \"name\"");
        assertEquals(enumInstance.name(), result.get("name"), "The value associated with \"name\" should match Enum.name()");
    }

    @Test
    void toMap_nullEnum_throwsNullPointerException() {
        // given
        Object nullEnum = null;
        // when / then
        assertThrows(NullPointerException.class, () -> EnumConversions.toMap(nullEnum, mockConverter), "Calling toMap with a null enum should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] Enum {0} with converter {1}")
    @MethodSource("validEnumProvider")
    void toMap_validEnum_returnsMapWithName(Object enumInstance, Converter converter) {
        // when
        Map<String, Object> result = EnumConversions.toMap(enumInstance, converter);
        // then
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey("name"), "Map must contain the key \"name\"");
        assertEquals(((Enum<?>) enumInstance).name(), result.get("name"), "The value for \"name\" must match Enum.name()");
    }

    @ParameterizedTest(name = "[{index}] Non‑enum input {0}")
    @MethodSource("nonEnumProvider")
    void toMap_nonEnumInput_throwsClassCastException(Object nonEnum) {
        // when / then
        assertThrows(ClassCastException.class, () -> EnumConversions.toMap(nonEnum, mockConverter), "Supplying a non‑enum argument must result in ClassCastException");
    }

    @Test
    void toMap_converterSideEffectsIgnored() {
        // given
        SampleEnum enumInstance = SampleEnum.FIRST;
        Converter exploding = new ExplodingConverter();
        // when – the method must not invoke any method on the converter
        Map<String, Object> result = EnumConversions.toMap(enumInstance, exploding);
        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("FIRST", result.get("name"));
    }

    @Test
    void toMap_returnedMapIsMutable() {
        // given
        SampleEnum enumInstance = SampleEnum.SECOND;
        // when
        Map<String, Object> result = EnumConversions.toMap(enumInstance, null);
        // mutate the map
        result.put("extra", "value");
        // then
        assertEquals(2, result.size(), "Map should contain the original and the newly added entry");
        assertEquals("SECOND", result.get("name"));
        assertEquals("value", result.get("extra"));
    }

    @Test
    void toMap_enumWithOverriddenMethods_returnsCorrectName() {
        // given
        ComplexEnum enumInstance = ComplexEnum.CUSTOM;
        // when
        Map<String, Object> result = EnumConversions.toMap(enumInstance, null);
        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CUSTOM", result.get("name"), "Even if toString() is overridden, name() must return the constant identifier");
    }
}
