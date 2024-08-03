/*-
 * ========================LICENSE_START=================================
 * Stdin/Stdout Helper for JUnit
 * %%
 * Copyright (C) 2020 - 2024 Pedro Alves
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
