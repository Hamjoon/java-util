package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Executor}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public method signatures.</li>
 *   <li>Normal successful execution using a harmless command that is available on every JVM
 *       ( {@code java -version} ).</li>
 *   <li>Edge‑case / error handling – null arguments, empty strings / arrays, and invalid
 *       directories – all of which should cause the {@code exec(...)} methods to return {@code -1}
 *       without throwing an exception.</li>
 * </ul>
 *
 * NOTE: The test deliberately avoids any mocking frameworks as required by the prompt.
 * It relies on the real {@code Runtime.exec(...)} implementation.
 */
public class ExecutorTest
{
    private Executor executor;

    @Before
    public void setUp()
    {
        executor = new Executor();
    }

    /* -------------------------------------------------
     *  Public method signatures – basic “happy path”
     * ------------------------------------------------- */

    @Test
    public void testExecString() throws Exception
    {
        // java -version writes its output to STDERR and exits with 0
        int rc = executor.exec("java -version");
        assertEquals("exec(String) should return the process exit code", 0, rc);
        assertNotNull("Error stream should be captured", executor.getError());
        assertTrue("Error stream should contain version information",
                executor.getError().toLowerCase().contains("version"));
        // Stdout for java -version is usually empty
        assertNotNull("Stdout should never be null", executor.getOut());
    }

    @Test
    public void testExecStringArray() throws Exception
    {
        String[] cmd = {"java", "-version"};
        int rc = executor.exec(cmd);
        assertEquals(0, rc);
        assertNotNull(executor.getError());
        assertTrue(executor.getError().toLowerCase().contains("version"));
        assertNotNull(executor.getOut());
    }

    @Test
    public void testExecStringWithEnv() throws Exception
    {
        // Passing null envp should behave the same as the no‑env overload
        int rc = executor.exec("java -version", null);
        assertEquals(0, rc);
        assertNotNull(executor.getError());
        assertTrue(executor.getError().toLowerCase().contains("version"));
    }

    @Test
    public void testExecStringArrayWithEnv() throws Exception
    {
        String[] cmd = {"java", "-version"};
        int rc = executor.exec(cmd, null);
        assertEquals(0, rc);
        assertNotNull(executor.getError());
        assertTrue(executor.getError().toLowerCase().contains("version"));
    }

    @Test
    public void testExecStringEnvDir() throws Exception
    {
        // Use the current working directory as the working dir argument
        File cwd = new File(System.getProperty("user.dir"));
        int rc = executor.exec("java -version", null, cwd);
        assertEquals(0, rc);
        assertNotNull(executor.getError());
        assertTrue(executor.getError().toLowerCase().contains("version"));
    }

    @Test
    public void testExecStringArrayEnvDir() throws Exception
    {
        String[] cmd = {"java", "-version"};
        File cwd = new File(System.getProperty("user.dir"));
        int rc = executor.exec(cmd, null, cwd);
        assertEquals(0, rc);
        assertNotNull(executor.getError());
        assertTrue(executor.getError().toLowerCase().contains("version"));
    }

    /* -------------------------------------------------
     *  Edge‑case / error handling tests
     * ------------------------------------------------- */

    @Test
    public void testExecNullString()
    {
        int rc = executor.exec((String) null);
        assertEquals("Null command should be caught and return -1", -1, rc);
        // When an exception occurs the class does not set _error/_out – they stay null
        assertNull(executor.getError());
        assertNull(executor.getOut());
    }

    @Test
    public void testExecEmptyString()
    {
        int rc = executor.exec("");
        assertEquals("Empty command should be caught and return -1", -1, rc);
        assertNull(executor.getError());
        assertNull(executor.getOut());
    }

    @Test
    public void testExecNullStringArray()
    {
        int rc = executor.exec((String[]) null);
        assertEquals("Null String[] should be caught and return -1", -1, rc);
        assertNull(executor.getError());
        assertNull(executor.getOut());
    }

    @Test
    public void testExecEmptyStringArray()
    {
        int rc = executor.exec(new String[0]);
        assertEquals("Empty String[] should be caught and return -1", -1, rc);
        assertNull(executor.getError());
        assertNull(executor.getOut());
    }

    @Test
    public void testExecStringArrayWithNullElement()
    {
        String[] cmd = {"java", null};
        int rc = executor.exec(cmd);
        assertEquals("Array containing null should cause an exception and return -1", -1, rc);
        assertNull(executor.getError());
        assertNull(executor.getOut());
    }

    @Test
    public void testExecWithInvalidDirectory()
    {
        // Create a File that does not exist
        File bogusDir = new File("this-directory-should-not-exist-12345");
        int rc = executor.exec("java -version", null, bogusDir);
        assertEquals("Invalid working directory should be caught and return -1", -1, rc);
        assertNull(executor.getError());
        assertNull(executor.getOut());
    }

    @Test
    public void testExecStringArrayEnvDirWithNullDir()
    {
        String[] cmd = {"java", "-version"};
        int rc = executor.exec(cmd, null, null);
        // Passing null for dir is allowed – it behaves like the overload without dir
        assertEquals(0, rc);
        assertNotNull(executor.getError());
        assertTrue(executor.getError().toLowerCase().contains("version"));
    }

    /* -------------------------------------------------
     *  Accessor tests – ensure getters simply return stored values
     * ------------------------------------------------- */

    @Test
    public void testGettersAfterFailedExec()
    {
        executor.exec((String) null); // will fail and leave fields untouched
        assertNull("getError() should be null after failed exec", executor.getError());
        assertNull("getOut()   should be null after failed exec", executor.getOut());
    }

    @Test
    public void testGettersAfterSuccessfulExec()
    {
        executor.exec("java -version");
        assertNotNull("getError() should contain something after success", executor.getError());
        assertNotNull("getOut()   should be non‑null after success", executor.getOut());
    }
}
