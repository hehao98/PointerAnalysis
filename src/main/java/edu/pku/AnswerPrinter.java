package edu.pku;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class AnswerPrinter {
	
	static void printAnswer(String answer) {
		try {
			PrintStream ps = new PrintStream(
				new FileOutputStream(new File("result.txt")));
			ps.print(answer);
			ps.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
