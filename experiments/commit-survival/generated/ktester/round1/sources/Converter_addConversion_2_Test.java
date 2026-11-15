package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_addConversion_2_Test {

    /**
     * A fresh Converter instance used for each test to avoid cross‑test pollution.
     */
    private Converter freshInstance;

    /**
     * Mock conversion function used in the tests.
     */
    @Mock
    private Convert<Integer> mockConversion;

    @Mock
    private Convert<Integer> mockIntConversion;

    @Mock
    private Convert<String> mockStringConversion;

    /**
     * Creates a new {@link Converter} instance before every test.
     * The public constructor of {@code Converter} takes a {@link DefaultConverterOptions}
     * argument (the same constructor used for the static singleton).  Using reflection
     * allows us to instantiate it even though the no‑arg constructor is private.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Obtain the (public) constructor that accepts DefaultConverterOptions
        Constructor<Converter> ctor = Converter.class.getDeclaredConstructor(DefaultConverterOptions.class);
        ctor.setAccessible(true);
        freshInstance = ctor.newInstance(new DefaultConverterOptions());
        // Replace the static singleton with the fresh instance so that static helper
        // methods (e.g., Converter.convert) operate on a clean state.
        Converter.instance = freshInstance;
    }

    /**
     * Clears the static singleton after each test to avoid leaking state to other test
     * classes that might rely on the default instance.
     */
    @AfterEach
    void teardownAfterEach() {
        // Reset the singleton to a newly created default instance.
        // This mirrors the initialization performed in the class definition.
        Converter.instance = new Converter(new DefaultConverterOptions());
    }

    private static Stream<Arguments> nullArgumentProvider() {
        // a trivial conversion, never used because NPE occurs earlier
        Convert<Object> dummy = o -> o;
        return Stream.of(Arguments.of(null, Integer.class, dummy), Arguments.of(String.class, null, dummy));
    }

    private static void assertSupportedMapping(Class<?> source, Class<?> target) {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        assertNotNull(map.get(source.getName()), "Supported map must contain source " + source.getName());
        assertTrue(map.get(source.getName()).contains(target.getName()), "Supported map must list target " + target.getName() + " for source " + source.getName());
    }

    @Test
    void addConversion_registersAndIsUsedByConvert() {
        // GIVEN – a source type (String), a target type (Integer) and a conversion lambda
        // that parses the string into an Integer.
        when(mockConversion.convert(any())).thenAnswer(invocation -> {
            String s = (String) invocation.getArgument(0);
            return Integer.valueOf(s);
        });
        // WHEN – the conversion is added to the fresh Converter instance
        Convert<?> returned = freshInstance.addConversion(String.class, Integer.class, mockConversion);
        // THEN – the same Convert instance is returned
        assertSame(mockConversion, returned, "addConversion should return the supplied Convert instance");
        // AND – the conversion is reported as supported
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class), "The newly added conversion should be reported as supported");
        // AND – the supported‑conversion map contains the new entry
        Map<String, Set<String>> supported = Converter.getSupportedConversions();
        assertNotNull(supported.get("java.lang.String"), "Supported conversions map should contain an entry for the source type");
        assertTrue(supported.get("java.lang.String").contains("java.lang.Integer"), "Supported conversions map should list the target type for the source type");
        // AND – the static convert method actually uses the supplied lambda
        Integer result = Converter.convert("123", Integer.class);
        assertEquals(123, result);
        // Verify that the mock was invoked exactly once with the original argument
        verify(mockConversion, times(1)).convert("123");
    }

    @Test
    void addConversion_overwritesExistingConversion() {
        // GIVEN – an initial conversion that returns 1 for any input
        Convert<Integer> first = mock(Convert.class);
        when(first.convert(any())).thenReturn(1);
        freshInstance.addConversion(String.class, Integer.class, first);
        // AND – a second conversion that returns 2 for any input
        Convert<Integer> second = mock(Convert.class);
        when(second.convert(any())).thenReturn(2);
        // WHEN – we add the second conversion for the same source/target pair
        freshInstance.addConversion(String.class, Integer.class, second);
        // THEN – the later conversion replaces the earlier one
        Integer result = Converter.convert("any", Integer.class);
        assertEquals(2, result);
        verify(second, times(1)).convert("any");
        // The first conversion should not be called after being overwritten
        verifyNoMoreInteractions(first);
    }

    @Test
    void addConversion_nullConversionThrowsException() {
        // GIVEN – a null conversion function
        Convert<Integer> nullConversion = null;
        // WHEN / THEN – adding a null conversion should throw a NullPointerException
        assertThrows(NullPointerException.class, () -> freshInstance.addConversion(String.class, Integer.class, nullConversion), "addConversion must reject a null Convert implementation");
    }

    @ParameterizedTest(name = "{index} => source={0}, target={1}, conversion={2}")
    @MethodSource("nullArgumentProvider")
    void addConversion_nullArgumentsThrowNPE(Class<?> source, Class<?> target, Convert<?> conversion) {
        assertThrows(NullPointerException.class, () -> freshInstance.addConversion(source, target, conversion), "addConversion must reject null source or target arguments");
    }

    @Test
    void addConversion_identityConversionFirstRegistrationReturnsNull() {
        // identity conversion for String -> String
        Convert<String> identity = mock(Convert.class);
        when(identity.convert(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // first registration – no previous entry
        Convert<?> previous = freshInstance.addConversion(String.class, String.class, identity);
        assertNull(previous, "First registration of an identity conversion should return null");
        // verify that the conversion is now supported
        assertTrue(Converter.isConversionSupportedFor(String.class, String.class));
        assertTrue(Converter.getSupportedConversions().get(String.class.getName()).contains(String.class.getName()));
        // use the conversion via the static API
        String result = Converter.convert("hello", String.class);
        assertEquals("hello", result);
        verify(identity, times(1)).convert("hello");
    }

    @Test
    void addConversion_identityConversionOverwriteReturnsPrevious() {
        Convert<String> first = mock(Convert.class);
        Convert<String> second = mock(Convert.class);
        when(first.convert(any())).thenReturn("first");
        when(second.convert(any())).thenReturn("second");
        // register first
        freshInstance.addConversion(String.class, String.class, first);
        // register second – should return the first instance
        Convert<?> previous = freshInstance.addConversion(String.class, String.class, second);
        assertSame(first, previous, "Overwriting an existing identity conversion must return the previous Convert");
        // conversion now should use the second implementation
        String result = Converter.convert("x", String.class);
        assertEquals("second", result);
        verify(second, times(1)).convert("x");
        verifyNoMoreInteractions(first);
    }

    @Test
    void addConversion_primitiveSourceSupported() {
        // primitive int -> String conversion
        Convert<String> intToString = mock(Convert.class);
        when(intToString.convert(any())).thenAnswer(inv -> String.valueOf(inv.getArgument(0)));
        Convert<?> previous = freshInstance.addConversion(int.class, String.class, intToString);
        assertNull(previous, "First registration for primitive source should return null");
        assertTrue(Converter.isConversionSupportedFor(int.class, String.class));
        // static convert should invoke our mock (auto‑boxing occurs)
        String result = Converter.convert(42, String.class);
        assertEquals("42", result);
        verify(intToString, times(1)).convert(42);
    }

    @Test
    void addConversion_genericObjectToString() {
        // Object -> String conversion that calls toString()
        Convert<String> objToString = mock(Convert.class);
        when(objToString.convert(any())).thenAnswer(inv -> inv.getArgument(0).toString());
        Convert<?> previous = freshInstance.addConversion(Object.class, String.class, objToString);
        assertNull(previous, "First registration for Object->String should return null");
        assertTrue(Converter.isConversionSupportedFor(Object.class, String.class));
        Date now = new Date();
        String result = Converter.convert(now, String.class);
        assertEquals(now.toString(), result);
        verify(objToString, times(1)).convert(now);
    }

    @Test
    void addConversion_collectionConcreteToInterface() {
        // ArrayList -> List conversion (identity cast)
        Convert<List<?>> listConversion = mock(Convert.class);
        when(listConversion.convert(any())).thenAnswer(inv -> (List<?>) inv.getArgument(0));
        Convert<?> previous = freshInstance.addConversion(ArrayList.class, List.class, listConversion);
        assertNull(previous, "First registration for ArrayList->List should return null");
        assertTrue(Converter.isConversionSupportedFor(ArrayList.class, List.class));
        ArrayList<Integer> src = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<?> result = Converter.convert(src, List.class);
        assertSame(src, result);
        verify(listConversion, times(1)).convert(src);
    }

    @Test
    void addConversion_overwriteReturnsPreviousInstance() {
        // First conversion: String -> Integer (parse)
        Convert<Integer> first = mock(Convert.class);
        when(first.convert(any())).thenReturn(1);
        freshInstance.addConversion(String.class, Integer.class, first);
        // Second conversion: String -> Integer (different logic)
        Convert<Integer> second = mock(Convert.class);
        when(second.convert(any())).thenReturn(2);
        Convert<?> previous = freshInstance.addConversion(String.class, Integer.class, second);
        assertSame(first, previous, "Overwriting should return the previously stored Convert");
        // Ensure the new conversion is used
        Integer result = Converter.convert("any", Integer.class);
        assertEquals(2, result);
        verify(second, times(1)).convert("any");
        verifyNoMoreInteractions(first);
    }

    @Test
    void addConversion_mapReflectsNewEntry() {
        Convert<Integer> conversion = mock(Convert.class);
        when(conversion.convert(any())).thenReturn(123);
        freshInstance.addConversion(String.class, Integer.class, conversion);
        assertSupportedMapping(String.class, Integer.class);
    }
}
