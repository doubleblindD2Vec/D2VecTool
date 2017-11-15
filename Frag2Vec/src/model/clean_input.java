package model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.EndingPreProcessor;

public class clean_input {
	
	public final static String input_name = "col_concatinate";
	public static String path = "/"+ input_name + ".txt";
	
	public static void main(String args[]) throws IOException{
		
		File file_output		= new File ("resources/input/"+input_name+"_clean.txt");
		FileWriter fWriter		= new FileWriter (file_output);
		PrintWriter writer		= new PrintWriter (fWriter);
		
		File file_training_corpus = new File("resources/input"+path);
		Scanner sc = new Scanner(file_training_corpus);
		while (sc.hasNextLine()){
			
			String line = sc.nextLine();
			
			//remove stop words
			line = line.replaceAll("[.,?:;!//(//)//[//]]", " ");
			
			EndingPreProcessor end = new EndingPreProcessor();
			line = end.preProcess(line); // Gets rid of endings: ed,ing, ly, s, .
			writer.println(line);
	
		}
		sc.close();
		writer.close();
	}
}
