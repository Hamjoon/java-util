package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.util.*;

public class EnumConversionsTest {

    /** Simple enum used for all conversion tests. */
    private enum Color {
        RED, GREEN, BLUE
    }

    /*--------------------------------------------------------------------
     *  Tests for EnumConversions.toMap(...)
     *--------------------------------------------------------------------*/

    @Test
    public void testToMapReturnsName() {
        Map<String, Object> map = EnumConversions.toMap(Color.RED, null);
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey("name"));
        Assert.assertEquals("RED", map.get("name"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullSourceThrowsNPE() {
        EnumConversions.toMap(null, null);
    }

    /*--------------------------------------------------------------------
     *  Tests for EnumConversions.toEnumSet(...)
     *--------------------------------------------------------------------*/

    @Test
    public void testToEnumSetFromEnumCollection() {
        List<Color> source = Arrays.asList(Color.RED, Color.BLUE);
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.BLUE), result);
    }

    @Test
    public void testToEnumSetFromStringCollection() {
        List<String> source = Arrays.asList("RED", "GREEN");
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.GREEN), result);
    }

    @Test
    public void testToEnumSetFromNumberCollection() {
        List<Integer> source = Arrays.asList(0, 2); // RED (0) and BLUE (2)
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.BLUE), result);
    }

    @Test
    public void testToEnumSetMixedCollection() {
        List<Object> source = Arrays.asList(Color.GREEN, "RED", 2);
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.GREEN, Color.BLUE), result);
    }

    @Test
    public void testToEnumSetWithNullElements() {
        List<Object> source = Arrays.asList(null, "RED", null);
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED), result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetInvalidStringThrowsException() {
        List<String> source = Collections.singletonList("PURPLE");
        EnumConversions.toEnumSet(source, Color.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetInvalidOrdinalThrowsException() {
        List<Integer> source = Collections.singletonList(5); // out of range
        EnumConversions.toEnumSet(source, Color.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetTargetNotEnumThrowsException() {
        List<String> source = Collections.singletonList("RED");
        EnumConversions.toEnumSet(source, String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetSourceNotCollectionOrArrayThrowsException() {
        EnumConversions.toEnumSet("RED", Color.class);
    }

    @Test
    public void testToEnumSetFromEnumArray() {
        Color[] source = new Color[]{Color.BLUE, Color.RED};
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.BLUE), result);
    }

    @Test
    public void testToEnumSetFromStringArray() {
        String[] source = new String[]{"GREEN", "BLUE"};
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.GREEN, Color.BLUE), result);
    }

    @Test
    public void testToEnumSetFromNumberArray() {
        Integer[] source = new Integer[]{1, 0}; // GREEN (1) and RED (0)
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.GREEN), result);
    }

    @Test
    public void testToEnumSetFromPrimitiveIntArray() {
        int[] source = new int[]{2, 0}; // BLUE (2) and RED (0)
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertEquals(EnumSet.of(Color.RED, Color.BLUE), result);
    }

    @Test
    public void testToEnumSetEmptyCollectionReturnsEmptySet() {
        List<Color> source = Collections.emptyList();
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testToEnumSetEmptyArrayReturnsEmptySet() {
        Color[] source = new Color[0];
        EnumSet<Color> result = EnumConversions.toEnumSet(source, Color.class);
        Assert.assertTrue(result.isEmpty());
    }
}
