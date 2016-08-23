package com.gene42.test.util;

import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by sebastian on 23/08/16.
 */
public class BoxTest
{
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
    /**
     * Class set up.
     *
     * @throws  Exception  on error
     */
    @BeforeClass
    public static void setUpClass() throws Exception {

    }

    /**
     * Class tear down.
     *
     * @throws  Exception  on error
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test set up.
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * Test tear down.
     */
    @After
    public void tearDown() {
    }

    @Test
    public void basic() throws Exception
    {
        Box box = new Box("box", 1.0d, 0.5d);
        box.getArea();

        box.getHeight();
        box.getName();
    }
}
