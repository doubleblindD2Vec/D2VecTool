package utils;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

public class Utils {
	public static String EXEC_DOT = "D:/Program Files (x86)/Graphviz2.36/bin/dot.exe"; // Windows
	
	public static void toGraphics(String file, String type) {
		Runtime rt = Runtime.getRuntime();

		String[] args = { EXEC_DOT, "-T" + type, file + ".dot", "-o",
				file + "." + type };
		try {
			Process p = rt.exec(args);
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String readStringFromFile(String inputFile) {
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
			byte[] bytes = new byte[(int) new File(inputFile).length()];
			in.read(bytes);
			in.close();
			return new String(bytes);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void writeStringToFile(String string, String outputFile) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(string);
			writer.flush();
			writer.close();
		}
		catch (Exception e) {
			/*e.printStackTrace();
			System.exit(0);*/
			System.err.println(e.getMessage());
		}
	}
}
