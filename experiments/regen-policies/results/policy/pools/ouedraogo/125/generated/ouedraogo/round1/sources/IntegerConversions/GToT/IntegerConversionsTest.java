package com.cedarsoftware.util.convert;

import java.lang.reflect.Modifier;
import java.time.LocalTime;
import org.junit.Test;
import static org.junit.Assert.*;

public class IntegerConversionsTest {

    /*** BASIC FUNCTIONALITY ***/
    @Test
    public void basic_toLocalTime_convertsIntegerMillis() {
        Integer ms = 1_234;                     // 1 234 ms = 00:00:01.234
        Converter converter = new Converter(new ConverterOptions());

        LocalTime result = IntegerConversions.toLocalTime(ms, converter);
        LocalTime expected = LongConversions.toLocalTime(ms.longValue(), converter);

        assertEquals(expected, result);
    }

    /*** EDGE CASES & EXCEPTIONS ***/

    // Null source object – should raise NPE from the cast
    @Test(expected = NullPointerException.class)
    public void toLocalTime_nullFrom_throwsNPE() {
        Converter converter = new Converter(new ConverterOptions());
        IntegerConversions.toLocalTime(null, converter);
    }

    // Source object of wrong type – should raise ClassCastException
    @Test(expected = ClassCastException.class)
    public void toLocalTime_nonIntegerFrom_throwsClassCastException() {
        Converter converter = new Converter(new ConverterOptions());
        IntegerConversions.toLocalTime("not an integer", converter);
    }

    // Negative millisecond value – conversion must match LongConversions behavior
    @Test
    public void toLocalTime_negativeMillis() {
        Integer ms = -500;                      // –0.5 seconds
        Converter converter = new Converter(new ConverterOptions());

        LocalTime result = IntegerConversions.toLocalTime(ms, converter);
        LocalTime expected = LongConversions.toLocalTime(ms.longValue(), converter);

        assertEquals(expected, result);
    }

    // Maximum int value – verify no overflow when widened to long
    @Test
    public void toLocalTime_maxIntMillis() {
        Integer ms = Integer.MAX_VALUE;         // 2 147 483 647 ms
        Converter converter = new Converter(new ConverterOptions());

        LocalTime result = IntegerConversions.toLocalTime(ms, converter);
        LocalTime expected = LongConversions.toLocalTime(ms.longValue(), converter);

        assertEquals(expected, result);
    }

    // The class has a private constructor; ensure it is indeed private and can be invoked via reflection.
    @Test
    public void privateConstructor_isNotAccessible() throws Exception {
        java.lang.reflect.Constructor<IntegerConversions> ctor =
                IntegerConversions.class.getDeclaredConstructor();

        // Verify the constructor is declared private
        assertTrue(Modifier.isPrivate(ctor.getModifiers()));

        // Make it accessible and instantiate – should succeed without side‑effects
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
