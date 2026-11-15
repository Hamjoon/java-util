package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteConversionsTest {

    /*** BASIC FUNCTIONALITY ***/
    @Test
    public void testToCharacterBasic() {
        Byte b = (byte)65;                     // ASCII 'A'
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf('A'), result);
    }

    /*** EDGE CASES ***/
    @Test
    public void testToCharacterMinValue() {
        Byte b = Byte.MIN_VALUE;               // -128
        Character result = ByteConversions.toCharacter(b, null);
        // (char)-128 == 0xFF80 == 65408
        assertEquals(Character.valueOf((char)-128), result);
        assertEquals(65408, result.charValue());
    }

    @Test
    public void testToCharacterMaxValue() {
        Byte b = Byte.MAX_VALUE;               // 127
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf((char)127), result);
        assertEquals(127, result.charValue());
    }

    @Test
    public void testToCharacterZero() {
        Byte b = (byte)0;
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf('\0'), result);
        assertEquals(0, result.charValue());
    }

    @Test
    public void testToCharacterNegativeOne() {
        Byte b = (byte)-1;
        Character result = ByteConversions.toCharacter(b, null);
        // (char)-1 == 0xFFFF == 65535
        assertEquals(Character.valueOf((char)-1), result);
        assertEquals(65535, result.charValue());
    }

    /*** EXCEPTION SCENARIOS ***/
    @Test(expected = NullPointerException.class)
    public void testToCharacterNullFrom() {
        ByteConversions.toCharacter(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacterWrongType() {
        Integer notAByte = Integer.valueOf(10);
        ByteConversions.toCharacter(notAByte, null);
    }
}
