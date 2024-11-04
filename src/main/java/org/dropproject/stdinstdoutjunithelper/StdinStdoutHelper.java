/*-
 * ========================LICENSE_START=================================
 * Stdin/Stdout Helper for JUnit
 * %%
 * Copyright (C) 2019 - 2020 Pedro Alves
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

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

enum Channel {
    STDIN, STDOUT
}

class Command {

    private String text;
    private Channel channel;
    protected ContextMessageBuilder msgBuilder;

    public Command(String text, Channel channel, ContextMessageBuilder msgBuilder) {
        this.text = text;
        this.channel = channel;
        this.msgBuilder = msgBuilder;
    }

    @Override
    public boolean equals(Object o) {
        return o.toString().equals(this.getText());
    }

    @Override
    public String toString() {
        return this.getText();
    }

    public String getText() {
        return text;
    }

    public Channel getChannel() {
        return channel;
    }

    public void validateAgainst(String realOutput, boolean showDetailedErrors) {
        if (!Channel.STDOUT.equals(channel)) {
            throw new IllegalStateException("This operation can only be performed on Stdout");
        }

        if (showDetailedErrors) {
            assertEquals(getText(), realOutput, msgBuilder.buildContextMessage());
        } else {
            if (!getText().equals(realOutput)) {
                fail("Incorrect output: " + realOutput + ".\n" + msgBuilder.buildContextMessage());
            }
        }

        assertEquals(getText(), realOutput);
    }
}

class SupplierCommand extends Command {

    private Supplier<String> supplier;

    public SupplierCommand(Supplier<String> supplier, ContextMessageBuilder msgBuilder) {
        super(null, Channel.STDIN, msgBuilder);
        this.supplier = supplier;
    }

    @Override
    public String getText() {
        return supplier.get();
    }
}

class PredicateCommand extends Command {

    private Predicate<String> predicate;

    public PredicateCommand(Predicate<String> predicate, ContextMessageBuilder msgBuilder) {
        super(null, Channel.STDOUT, msgBuilder);
        this.predicate = predicate;
    }

    @Override
    public void validateAgainst(String realOutput, boolean showDetailedErrors) {
        boolean isValid = false;
        try {
            isValid = predicate.test(realOutput);
        } catch (AssertionFailedError e) {
            if (showDetailedErrors) {
                fail(e.getMessage() + ". " + msgBuilder.buildContextMessage());
            } else {
                fail(e.getMessage());
            }
        }
        if (!isValid) {
            if (showDetailedErrors) {
                fail("Output different from expected. Actual output: " + realOutput + ". " + msgBuilder.buildContextMessage());
            } else {
                fail("Output different from expected. Actual output: " + realOutput);
            }
        }
    }
}

class FunctionCommand extends Command {

    private Function<String,String> function;

    public FunctionCommand(Function<String,String> function, ContextMessageBuilder msgBuilder) {
        super(null, Channel.STDOUT, msgBuilder);
        this.function = function;
    }

    @Override
    public void validateAgainst(String realOutput, boolean showDetailedErrors) {
        String result = function.apply(realOutput);
        if (showDetailedErrors) {
            assertEquals(realOutput, result, msgBuilder.buildContextMessage());
        } else {
            assertEquals(realOutput, result);
        }
    }
}

class ExpectLineCommand extends Command {

    public ExpectLineCommand(ContextMessageBuilder msgBuilder) {
        super("", Channel.STDOUT, msgBuilder);
    }

    @Override
    public boolean equals(Object o) {
        return true;
    }

    @Override
    public void validateAgainst(String realOutput, boolean showDetailedErrors) {
        // do nothing. just consume the line
    }
}

public class StdinStdoutHelper implements ContextMessageBuilder {

    public static final int DEFAULT_OUTPUT_BUFFER_SIZE = 10;

    private List<Command> commands;

    private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut = System.out;
    private InputStream originalIn = System.in;

    private boolean showDetailedErrors = true;
    private boolean writeLog = false;

    private int currentCommandIdx = 0;
    private boolean active = false;

    private CircularFifoQueue<String> outputBuffer;

    public StdinStdoutHelper() {
        this(false);
    }

    public StdinStdoutHelper(boolean showDetailedErrors) {
        this.commands = new ArrayList<Command>();
        this.showDetailedErrors = showDetailedErrors;
        this.outputBuffer = new CircularFifoQueue<>(DEFAULT_OUTPUT_BUFFER_SIZE);
    }

    public StdinStdoutHelper(boolean showDetailedErrors, int outputBufferSize) {
        this(showDetailedErrors);
        this.outputBuffer = new CircularFifoQueue<>(outputBufferSize);
    }

    public StdinStdoutHelper setWriteLog(boolean writeLog) {
        this.writeLog = writeLog;
        return this;
    }

    /**
     * Force Line Separatgor LF Unix & MAC (\n)
     * On Windows SO environment will delete '\r'
     */
    private String forceLineSeparatorUnix(String source) {
        return source != null && SystemUtil.isWindows() ? source.replaceAll("\r", "") : source;
    }

    public StdinStdoutHelper simulateInput(String textToReadFromKeyboard) {
        commands.add(new Command(textToReadFromKeyboard, Channel.STDIN, this));
        return this;
    }

    public StdinStdoutHelper simulateInput(Supplier<String> function) {
        commands.add(new SupplierCommand(function, this));
        return this;
    }

    public StdinStdoutHelper expectOutput(String expectedTextWrittenToConsole) {
        commands.add(new Command(expectedTextWrittenToConsole, Channel.STDOUT, this));
        return this;
    }

    public StdinStdoutHelper expectOutput(Function<String, String> function) {
        commands.add(new FunctionCommand(function, this));
        return this;
    }

    public StdinStdoutHelper matchOutput(Predicate<String> predicate) {
        commands.add(new PredicateCommand(predicate, this));
        return this;
    }

    public StdinStdoutHelper expectNNumberOfLines(int expectedNumberOfLines) {
        for (int i = 0; i < expectedNumberOfLines; i++) {
            commands.add(new ExpectLineCommand(this));
        }
        return this;
    }

    public StdinStdoutHelper expectMultiLineOutput(String expectedTextWrittenToConsole) {
        List<String> strList = new ArrayList<String>(
                Arrays.asList(expectedTextWrittenToConsole.split("\n")));
        if (expectedTextWrittenToConsole.endsWith("\n")) {
            strList.add("");
        }

        StdinStdoutHelper result = this;
        for (String expectedLine : strList) {
            result = this.expectOutput(expectedLine);
        }

        return result;
    }

    public void start() {
        System.setOut(new PrintStream(outContent) {

            int startLineIdx = 0, endLineIdx = -1, pos = 0;

            @Override
            public void write(int b) {

                super.write(b);
                if (b == '\n') {
                    outputBuffer.add("[OUT]: (enter)");
                    if (writeLog) System.err.println("[OUT]:(enter)");
                    endLineIdx = pos;
                    String line = new String(Arrays.copyOfRange(outContent.toByteArray(), startLineIdx, endLineIdx + 1));
                    checkLine(line);
                    startLineIdx = pos + 1;
                } else {
                    if (writeLog) System.err.println("[OUT]:" + (char) b);
                }
                pos++;
            }

            @Override
            public void write(byte[] buf, int off, int len) {
                super.write(buf, off, len);
                String content = getLine(buf, off, len);
                outputBuffer.addAll(Arrays.stream(content.split("\\n"))
                        .map((s) -> { if (s.equals("(enter)")) return "[OUT]: " +s; else return "[OUT]: " + s + "(enter)"; })
                        .collect(Collectors.toCollection(ArrayList::new)));
                if (writeLog) {
                    System.err.println("[OUT]:" + content);
                }
                int i = off;
                while (i < len) {
                    if (buf[i] == '\n') {
                        endLineIdx = pos;
                        String line = new String(Arrays.copyOfRange(outContent.toByteArray(), startLineIdx, endLineIdx)); // without '\n'
                        checkLine(SystemUtil.isWindows() ? forceLineSeparatorUnix(line) : line);
                        startLineIdx = pos + 1;
                    }
                    i++;
                    pos++;
                }
            }


            private void checkLine(String line) {

                if (currentCommandIdx < commands.size()) {
                    Command currentCommand = commands.get(currentCommandIdx);
                    if (currentCommand.getChannel() == Channel.STDOUT) {
                        currentCommandIdx++;
                        currentCommand.validateAgainst(line, showDetailedErrors);
                    } else {
                        assertEquals("[IN] " + currentCommand.getText(), "[OUT] " + line, buildContextMessage());
                    }
                }
            }
        });


        System.setIn(new InputStream() {

            StringBuffer currentBuffer = new StringBuffer();
            int currentPositionWithinCommand = 0;

            @Override
            public int read() throws IOException {

                if (currentCommandIdx < commands.size()) {
                    Command currentCommand = commands.get(currentCommandIdx);
                    if (currentCommand.getChannel() == Channel.STDIN) {
                        String currentCommandStr = commands.get(currentCommandIdx).getText();

                        if (currentPositionWithinCommand == currentCommandStr.length()) {
                            currentCommandIdx++;
                            currentPositionWithinCommand = 0;
                            outputBuffer.add("[IN]: " + currentBuffer + "(enter)");
                            currentBuffer.setLength(0);  // reset
                            if (writeLog) System.err.println("[IN]:(enter)");
                            return '\n';
                        } else {
                            char ch = currentCommandStr.charAt(currentPositionWithinCommand++);
                            currentBuffer.append(ch);
                            if (writeLog) System.err.println("[IN]:" + ch);
                            return ch;
                        }

                    } else {
                        assertEquals("[OUT] " + currentCommand.getText(), "[IN] ...", buildContextMessage());
                    }

                } else {
                    // the real program was expecting more stdin than what was defined in the test
                    fail("You have an extra [IN] that is not needed. " + buildContextMessage());
                }

                return -1;
            }
        });

        active = true;
    }

    public void stop() {

        System.setOut(originalOut);
        System.setIn(originalIn);
        active = false;

        if (currentCommandIdx < commands.size()) {
            fail("Program finished too early. It should have printed '" +
                    commands.get(currentCommandIdx)
                    + "'. " + buildContextMessage());
        }

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (active) {
            stop();
        }
    }

    private static String getLine(byte[] buffer, int offset, int length) {

        String result = "";
        String decodedString = new String(buffer, offset, length, StandardCharsets.UTF_8);
        for (int i = 0; i < decodedString.length(); i++) {
            char ch = decodedString.charAt(i);
            if (ch != '\n') {
                result += ch;
            } else {
                result += "(enter)\n";
            }
        }

        return result;
    }

    public String buildContextMessage() {

        if (outputBuffer.isEmpty()) {
            return "";
        }

        String result = "Last " + outputBuffer.size() + " lines were:\n";
        for (int i = 0; i < outputBuffer.size(); i++) {
            result += outputBuffer.get(i) + "\n";
        }

        return result;
    }
}