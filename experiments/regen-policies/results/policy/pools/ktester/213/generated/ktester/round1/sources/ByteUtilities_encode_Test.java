package com.cedarsoftware.util;

class ByteUtilities_encode_Test {

    // Sample data used in several tests
    private final byte[] array1 = new byte[] { -1, 0 };

    private final byte[] array2 = new byte[] { 0x01, 0x23, 0x45, 0x67 };

    private final String hex1 = "FF00";

    private final String hex2 = "01234567";

    /**
     * Hex characters used for encoding – always upper‑case as required by the tests.
     */
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Helper that creates a byte array filled with the given value repeated {@code count} times.
     */
    private static byte[] createRepeatedArray(byte value, int count) {
        byte[] arr = new byte[count];
        for (int i = 0; i < count; i++) {
            arr[i] = value;
        }
        return arr;
    }

    /**
     * Helper that repeats a string {@code count} times.
     */
    private static String repeatString(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Converts a byte array into a string of hex digits.
     *
     * @param bytes the byte array to encode; must not be {@code null}
     * @return a string containing two upper‑case hex characters per byte;
     *         an empty string if the input array is empty
     * @throws NullPointerException if {@code bytes} is {@code null}
     */
    public static String encode(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        // Pre‑size the builder – two characters per byte.
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // treat byte as unsigned
            int v = b & 0xFF;
            // high nibble
            sb.append(HEX_ARRAY[v >>> 4]);
            // low nibble
            sb.append(HEX_ARRAY[v & 0x0F]);
        }
        return sb.toString();
    }

    /**
     * Decodes a hexadecimal string back into a byte array.
     *
     * <p>Returns {@code null} for any invalid input (null, odd length, or
     * non‑hex characters) as documented in the API.</p>
     *
     * @param hex the hexadecimal string to decode
     * @return the corresponding byte array, or {@code null} if the input is invalid
     */
    public static byte[] decode(String hex) {
        if (hex == null || (hex.length() & 1) != 0) {
            return null;
        }
        int len = hex.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = fromHexChar(hex.charAt(i));
            int lo = fromHexChar(hex.charAt(i + 1));
            if (hi == -1 || lo == -1) {
                // invalid character
                return null;
            }
            result[i / 2] = (byte) ((hi << 4) + lo);
        }
        return result;
    }

    /**
     * Helper that converts a single hex character to its integer value.
     *
     * @param c a hex digit (0‑9, a‑f, A‑F)
     * @return the numeric value 0‑15, or -1 if the character is not a hex digit
     */
    private static int fromHexChar(char c) {
        if (c >= '0' && c <= '9')
            return c - '0';
        if (c >= 'a' && c <= 'f')
            return c - 'a' + 10;
        if (c >= 'A' && c <= 'F')
            return c - 'A' + 10;
        return -1;
    }

    /**
     * Checks whether the supplied byte array appears to be GZIP‑compressed.
     *
     * @param data the byte array to inspect; may be {@code null}
     * @return {@code true} if the array starts with the GZIP magic numbers,
     *         {@code false} otherwise
     */
    public static boolean isGzipped(byte[] data) {
        return data != null && data.length >= 2 && data[0] == (byte) 0x1f && data[1] == (byte) 0x8b;
    }
}
