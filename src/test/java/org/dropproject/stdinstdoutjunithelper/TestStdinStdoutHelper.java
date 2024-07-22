package org.dropproject.stdinstdoutjunithelper;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestStdinStdoutHelper {

    @Test
    public void simpleOutput() {

        StdinStdoutHelper helper = new StdinStdoutHelper()
                .expectOutput("Test1")
                .expectOutput("Test2");

        helper.start();
        System.out.println("Test1");
        System.out.println("Test2");
        helper.stop();
    }

    @Test
    public void simpleOutputWrong() {

        AssertionError error = null;

        StdinStdoutHelper helper = new StdinStdoutHelper()
                .expectOutput("Test1")
                .expectOutput("Test2");

        helper.start();
        System.out.println("Test1");
        try {
            System.out.println("Test3");
        } catch (AssertionError e) {
            error = e;
        }
        helper.stop();

        if (error == null) {
            fail("Should have detected a failure since it was expecting 'Test2' but it was printed 'Test3'");
        } else {
            assertEquals("Last 5 lines were:\n" +
                    "[OUT]: Test1(enter)\n" +
                    "[OUT]: (enter)\n" +
                    "[OUT]: Test3(enter)\n" +
                    "[OUT]: (enter)\n" +
                    " ==> expected: <Test2> but was: <Test3>", error.getMessage());
        }
    }

    @Test
    public void programTerminatesTooSoon() {

        AssertionError error = null;

        StdinStdoutHelper helper = new StdinStdoutHelper()
                .expectOutput("Test1")
                .expectOutput("Test2");

        helper.start();
        System.out.println("Test1");
        try {
            helper.stop();
        } catch (AssertionError e) {
            error = e;
        }

        if (error == null) {
            fail("Should have detected a failure since it was expecting 'Test2' but it didn't print anything");
        } else {
            assertEquals("Program finished too early. It should have printed 'Test2'. Last 5 lines were:\n" +
                    "[OUT]: Test1(enter)\n" +
                    "[OUT]: (enter)\n", error.getMessage());
        }
    }
}
