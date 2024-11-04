# Stdin/Stdout Helper for JUnit

Useful for testing java/kotlin programs interaction with the command line. 

## How to install

Include the following dependency on your pom file:

    <dependency>
	    <groupId>org.dropproject</groupId>
	    <artifactId>stdin-stdout-junit-helper</artifactId>
	    <version>0.4.1</version>
	</dependency>
	
## How to use

```java
public class Main {
    public static void main(String args[]) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter your name");
        String name = scanner.nextLine();
        System.out.println("Your name is " + name);
        
        scanner.close();
    }
}


class TestStdinStdout {

    @Test
    public void testAskName() {
        StdinStdoutHelper helper = new StdinStdoutHelper()
                .expectOutput("Enter your name")
                .simulateInput("Pedro")
                .expectOutput("Your name is Pedro");

        helper.start();
        main(null);
        helper.stop();
    }
}
```

### Contextual information on failure

In highly interactive programs, it can be difficult to understand what happened on certain
test failures. You can pass a `showDetailedErrors` and a `òutputBufferSize` to the
`StdinStdoutHelper` constructor, as in this example:

```java
StdinStdoutHelper helper = new StdinStdoutHelper(true, 20)
                .expectOutput("Enter your name")
                .simulateInput("Pedro")
                .expectOutput("Enter your age")
                .simulateInput("45")
                .expectOutput("Pedro, you're 45 years old!");
```                    
This will show the last 20 (or less) lines of the interaction before the failure.

### Ignoring output lines

If the program writes several lines to the stdout that you don't want to test, you can use the function `expectNNumberOfLines`:
```java
StdinStdoutHelper helper = new StdinStdoutHelper()
                .expectOutput("Enter the file name")
                .simulateInput("data.txt")
                .expectOutput("The data contained in the file:")
                .expectNNumberOfLines(57)  // suppose the file has 57 lines
                .expectOutput("Read another file? [Y|N]")
                .simulateInput("N");
```

### Dynamic behaviour

Suppose you have a program that prints a random number to the stdout, and you want to test that the printed number is 
within a certain range. You can use the `matchOutput` which receives a predicate function. This function receives the
line that was printed to the screen and returns true if it matches the expected value. This is evaluated during the
program's execution.

#### Example in Kotlin
```kotlin
val helper = StdinStdoutHelper()
    .expectOutput("Press enter to generate a random number between 1 and 100")
    .simulateInput("")  // press enter
    .matchOutput { it in 1..100 }
```
