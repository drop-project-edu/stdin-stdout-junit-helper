# Stdin/Stdout Helper for JUnit

Useful for testing java/kotlin programs interaction with the command line. 

## How to install

Include the following dependency on your pom file:

    <dependency>
	    <groupId>pt.ulusofona.deisi</groupId>
	    <artifactId>stdin-stdout-junit-helper</artifactId>
	    <version>0.2.0</version>
	</dependency>
	
## How to use

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