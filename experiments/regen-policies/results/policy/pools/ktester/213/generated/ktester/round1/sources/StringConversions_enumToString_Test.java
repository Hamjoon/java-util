package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringConversions_enumToString_Test {

    /**
     * Mocked Converter – not used by the method but required by the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Outer class containing a nested enum, also used in the JSON test data.
     */
    private static class OuterClass {

        enum InnerEnum {

            NESTED
        }
    }

    /**
     * Minimal stub implementation of {@link Converter} – the method under test ignores it.
     */
    private static class StubConverter implements Converter {
        // No methods required for the current tests; this class merely satisfies the type.
    }

    @Test
    void enumToString_returnsEnumName() {
        // GIVEN
        SampleEnum value = SampleEnum.FIRST;
        // WHEN
        String result = StringConversions.enumToString(value, mockConverter);
        // THEN
        assertEquals("FIRST", result);
    }

    @Test
    void enumToString_nullSource_throwsNullPointerException() {
        // GIVEN a null source
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.enumToString(nullSource, mockConverter));
    }

    @Test
    void enumToString_nonEnumSource_throwsClassCastException() {
        // GIVEN a non‑enum object
        Object notAnEnum = "I am not an enum";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.enumToString(notAnEnum, mockConverter));
    }

    @Test
    void enumToString_regularEnumConstant() {
        // GIVEN
        TestEnum value = TestEnum.FIRST;
        // WHEN
        String result = StringConversions.enumToString(value, mockConverter);
        // THEN
        assertEquals("FIRST", result);
    }

    @Test
    void enumToString_enumConstantWithUnderscore() {
        // GIVEN
        TestEnum value = TestEnum.SECOND_VALUE;
        // WHEN
        String result = StringConversions.enumToString(value, mockConverter);
        // THEN
        assertEquals("SECOND_VALUE", result);
    }

    @Test
    void enumToString_nestedEnumConstant() {
        // GIVEN
        OuterClass.InnerEnum value = OuterClass.InnerEnum.NESTED;
        // WHEN
        String result = StringConversions.enumToString(value, mockConverter);
        // THEN
        assertEquals("NESTED", result);
    }

    @Test
    void enumToString_unicodeEnumConstant() {
        // GIVEN
        UnicodeEnum value = UnicodeEnum.ÜNÍCÓDE;
        // WHEN
        String result = StringConversions.enumToString(value, mockConverter);
        // THEN
        assertEquals("ÜNÍCÓDE", result);
    }

    @Test
    void enumToString_converterArgumentIsIgnored() {
        // GIVEN
        TestEnum value = TestEnum.FIRST;
        // non‑null, but should be ignored
        Converter realConverter = new StubConverter();
        // WHEN
        String result = StringConversions.enumToString(value, realConverter);
        // THEN
        assertEquals("FIRST", result);
    }

    @Test
    void enumToString_nullSource_throwsExceptionWithCorrectType() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.enumToString(nullSource, mockConverter));
    }

    @Test
    void enumToString_nonEnumSource_throwsExceptionWithCorrectType() {
        // GIVEN
        Object notAnEnum = "Just a String";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.enumToString(notAnEnum, mockConverter));
    }
}
