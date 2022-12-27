package cmsc420_f22;

// YOU SHOULD NOT MODIFY THIS FILE, EXCEPT TO ALTER THE INPUT/OUTPUT SOURCES

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Testing program for Part 3. By default, this reads from tests/test01-input.txt and
 * writes the output to tests/test01-output.txt. Change inputFileName and outputFileName
 * below to change this behavior.
 */

public class Part3Tester {

	// --------------------------------------------------------------------------------------------
	// Uncomment these to read from standard input and output
	// private static final boolean USE_STD_IO = true;
	// private static String inputFileName = "";
	// private static String outputFileName = "";
	// --------------------------------------------------------------------------------------------
	// Uncomment these to read from files
	private static final boolean USE_STD_IO = false;
	private static String inputFileName = "tests/test05-input.txt";
	private static String outputFileName = "tests/test05-output.txt";
	// --------------------------------------------------------------------------------------------

	public static void main(String[] args) {

		// configure to read from file rather than standard input/output
		PrintStream saveStdOut = System.out;
		if (!USE_STD_IO) { // redirect I/O to file
			try {
				System.setIn(new FileInputStream(inputFileName));
				System.setOut(new PrintStream(outputFileName));
			} catch (FileNotFoundException e) {
				System.err.println("Error: Cannot find input or output file.");
				e.printStackTrace();
			}
		}

		try {
			Scanner scanner = new Scanner(System.in); // input scanner
			Part3CommandHandler commandHandler = new Part3CommandHandler(); // initialize command handler
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine(); // input next line
				String output = commandHandler.processCommand(line); // process this command
				System.out.print(output); // output summary
			}
			scanner.close();

			// Close window when done
			commandHandler.closeWindow();

			System.setOut(saveStdOut); // restore standard output
			System.out.println("Completed execution.");
			if (!USE_STD_IO) {
				System.out.println("-- Your output can be found in " + outputFileName);
				System.out.println("-- You may need to refresh the folder list to see it.");
			}
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}
