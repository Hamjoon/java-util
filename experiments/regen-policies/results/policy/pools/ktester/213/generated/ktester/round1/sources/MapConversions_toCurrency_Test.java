package com.cedarsoftware.util.convert;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class MapConversions_toCurrency_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    /**
     * Minimal subclass of {@link Converter} that allows us to override {@code convert}
     * without pulling in any heavy mocking framework.
     */
    private static class SimpleConverter extends Converter {

        SimpleConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object src, Class<T> target) {
            // Default implementation – subclasses override as needed.
            return super.convert(src, target);
        }
    }

    @Nested
    @DisplayName("Exception‑throwing scenarios")
    class ExceptionScenarios {

        @Test
        @DisplayName("Null source results in NullPointerException")
        void toCurrency_givenNull_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> MapConversions.toCurrency(null, converter));
        }

        @Test
        @DisplayName("Invalid currency code string throws IllegalArgumentException")
        void toCurrency_givenInvalidString_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency("NOT_A_CURRENCY", converter));
        }

        @Test
        @DisplayName("Map without any VALUE_KEYS throws IllegalArgumentException")
        void toCurrency_givenMapMissingKeys_throwsIllegalArgumentException() {
            Map<String, Object> map = new HashMap<>();
            map.put("unknown", "XYZ");
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(map, converter));
        }

        @Test
        @DisplayName("Lower‑case ISO code is rejected")
        void toCurrency_givenLowerCaseString_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency("usd", converter));
        }

        @Test
        @DisplayName("Empty string throws IllegalArgumentException")
        void toCurrency_givenEmptyString_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency("", converter));
        }

        @Test
        @DisplayName("String containing only whitespace throws IllegalArgumentException")
        void toCurrency_givenWhitespaceString_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency("   ", converter));
        }

        @Test
        @DisplayName("Map with key \"value\" holding empty string throws IllegalArgumentException")
        void toCurrency_givenMapWithEmptyString_throwsIllegalArgumentException() {
            Map<String, Object> map = new HashMap<>();
            map.put("value", "");
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(map, converter));
        }

        @Test
        @DisplayName("Map with key \"value\" holding whitespace only throws IllegalArgumentException")
        void toCurrency_givenMapWithWhitespaceString_throwsIllegalArgumentException() {
            Map<String, Object> map = new HashMap<>();
            map.put("value", "   ");
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(map, converter));
        }

        @Test
        @DisplayName("Map with key \"value\" holding null throws IllegalArgumentException")
        void toCurrency_givenMapWithNullValue_throwsIllegalArgumentException() {
            Map<String, Object> map = new HashMap<>();
            map.put("value", null);
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(map, converter));
        }

        @Test
        @DisplayName("Map with numeric value without custom Converter throws IllegalArgumentException")
        void toCurrency_givenMapWithNumericValue_throwsIllegalArgumentException() {
            Map<String, Object> map = new HashMap<>();
            map.put("value", 123);
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(map, converter));
        }

        @Test
        @DisplayName("Object with public Integer field \"value\" throws IllegalArgumentException")
        void toCurrency_givenObjectWithIntegerField_throwsIllegalArgumentException() {
            Object src = new Object() {

                public Integer value = 123;
            };
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(src, converter));
        }

        @Test
        @DisplayName("Object with getter returning Integer throws IllegalArgumentException")
        void toCurrency_givenObjectWithIntegerGetter_throwsIllegalArgumentException() {
            Object src = new Object() {

                public Integer getValue() {
                    return 123;
                }
            };
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(src, converter));
        }

        @Test
        @DisplayName("Object with public field \"value\" set to null throws IllegalArgumentException")
        void toCurrency_givenObjectWithNullField_throwsIllegalArgumentException() {
            Object src = new Object() {

                public String value = null;
            };
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(src, converter));
        }

        @Test
        @DisplayName("Object with private field and no getter throws IllegalArgumentException")
        void toCurrency_givenObjectWithPrivateFieldOnly_throwsIllegalArgumentException() {
            Object src = new Object() {

                private String value = "CHF";
            };
            assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(src, converter));
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Create a mock Converter that always returns null, forcing MapConversions
        // to use its internal conversion logic.
        converter = mock(Converter.class);
        when(converter.convert(any(), eq(Currency.class))).thenReturn(null);
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    @Test
    void toCurrency_givenCurrencyInstance_returnsSameInstance() {
        Currency usd = Currency.getInstance("USD");
        Currency result = MapConversions.toCurrency(usd, converter);
        assertSame(usd, result, "When a Currency instance is supplied it should be returned unchanged.");
    }

    @Test
    void toCurrency_givenStringCode_returnsCorrespondingCurrency() {
        Currency eur = Currency.getInstance("EUR");
        Currency result = MapConversions.toCurrency("EUR", converter);
        assertEquals(eur, result, "String ISO‑code should be converted to the matching Currency.");
    }

    @Test
    void toCurrency_givenMapWithValueKey_returnsCurrency() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "GBP");
        Currency result = MapConversions.toCurrency(map, converter);
        assertEquals(Currency.getInstance("GBP"), result);
    }

    @Test
    void toCurrency_givenMapWithVKey_returnsCurrency() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "JPY");
        Currency result = MapConversions.toCurrency(map, converter);
        assertEquals(Currency.getInstance("JPY"), result);
    }

    @Test
    void toCurrency_givenNull_returnsNull() {
        Currency result = MapConversions.toCurrency(null, converter);
        assertNull(result, "Null source should result in a null Currency.");
    }

    @Test
    void toCurrency_givenInvalidString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency("NOT_A_CURRENCY", converter), "An unknown currency code should cause IllegalArgumentException.");
    }

    @Test
    void toCurrency_givenMapWithUnsupportedValue_throwsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 12345);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toCurrency(map, converter), "A map containing a non‑convertible value should cause IllegalArgumentException.");
    }

    @Test
    @DisplayName("Map value already a Currency returns that instance")
    void toCurrency_givenMapWithCurrencyValue_returnsSameCurrency() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Currency.getInstance("GBP"));
        Currency result = MapConversions.toCurrency(map, converter);
        assertEquals(Currency.getInstance("GBP"), result);
    }

    @Test
    @DisplayName("String with surrounding whitespace is trimmed before conversion")
    void toCurrency_givenStringWithWhitespace_isTrimmed() {
        Currency gbr = Currency.getInstance("GBP");
        Currency result = MapConversions.toCurrency("  GBP  ", converter);
        assertEquals(gbr, result);
    }

    @Test
    @DisplayName("When both VALUE_KEYS are present, \"value\" takes precedence")
    void toCurrency_givenMapWithBothKeys_valueWins() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "EUR");
        map.put("_v", "JPY");
        Currency result = MapConversions.toCurrency(map, converter);
        assertEquals(Currency.getInstance("EUR"), result);
    }

    @Test
    @DisplayName("Public field \"value\" on a plain object is used")
    void toCurrency_givenObjectWithPublicField_isUsed() {
        Object src = new Object() {

            public String value = "CHF";
        };
        Currency result = MapConversions.toCurrency(src, converter);
        assertEquals(Currency.getInstance("CHF"), result);
    }

    @Test
    @DisplayName("Getter method getValue() is used when no public field is present")
    void toCurrency_givenObjectWithGetter_isUsed() {
        Object src = new Object() {

            public String getValue() {
                return "CAD";
            }
        };
        Currency result = MapConversions.toCurrency(src, converter);
        assertEquals(Currency.getInstance("CAD"), result);
    }

    @Test
    @DisplayName("Public getter overrides private field")
    void toCurrency_givenObjectWithPrivateFieldAndPublicGetter_usesGetter() {
        Object src = new Object() {

            private String value = "CHF";

            public String getValue() {
                return "CAD";
            }
        };
        Currency result = MapConversions.toCurrency(src, converter);
        assertEquals(Currency.getInstance("CAD"), result);
    }

    @Test
    @DisplayName("Public field wins over getter when both are present")
    void toCurrency_givenObjectWithFieldAndGetter_usesField() {
        Object src = new Object() {

            public String value = "AUD";

            public String getValue() {
                return "NZD";
            }
        };
        Currency result = MapConversions.toCurrency(src, converter);
        assertEquals(Currency.getInstance("AUD"), result);
    }

    @Test
    @DisplayName("Custom Converter can handle Integer source")
    void toCurrency_givenIntegerWithCustomConverter_isConverted() {
        // Create a mock Converter that knows how to turn Integer 123 into USD.
        Converter custom = mock(Converter.class);
        when(custom.convert(eq(123), eq(Currency.class))).thenReturn(Currency.getInstance("USD"));
        Currency result = MapConversions.toCurrency(123, custom);
        assertEquals(Currency.getInstance("USD"), result);
    }

    @Test
    @DisplayName("Custom Converter invoked for Map value that is Integer")
    void toCurrency_givenMapWithIntegerAndCustomConverter_isConverted() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 123);
        Converter custom = mock(Converter.class);
        when(custom.convert(eq(123), eq(Currency.class))).thenReturn(Currency.getInstance("USD"));
        Currency result = MapConversions.toCurrency(map, custom);
        assertEquals(Currency.getInstance("USD"), result);
    }

    @Test
    @DisplayName("When custom Converter returns null, default logic still works")
    void toCurrency_givenStringAndNullReturningCustomConverter_usesDefaultLogic() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), eq(Currency.class))).thenReturn(null);
        Currency result = MapConversions.toCurrency("USD", custom);
        assertEquals(Currency.getInstance("USD"), result);
    }
}
