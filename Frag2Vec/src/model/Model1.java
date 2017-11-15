package model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

public class Model1 {
	public final static int num_FragTutorial 	= 57; 
	public static Map<String,List<String>> training_map = new HashMap<>();
	public static Map<String,Map<String,Double>> result_map = new HashMap<>();

	public final static String input_name = "col_concatinate";
	public final static String model_name = input_name + "_VSM";
	public static String path = "/"+ input_name + ".txt";
	public static void main(String args[]) throws IOException{
		/*		Initialize Log file
		Store info of training */

		File file_output	= new File ("resources/output/model/"+model_name);
		FileWriter fWriter 	= new FileWriter (file_output);
		PrintWriter writer 	= new PrintWriter (fWriter);
		
		// *****************************************************************************
		
		/*	
		 * 	Step 1: Read Training Corpus and build a Map
		 *  Key		= API name
		 *  Value	= List / Array of its DOC_ID
		 */
		File file_training_corpus	= new File ("resources/input"+path);
		Scanner sc_training_corpus = new Scanner(file_training_corpus);
		int i = 0;
		String first_token 	= "";
		String last_token	= "";
		List<String> DOC_ID = new ArrayList<String>();
		while (sc_training_corpus.hasNextLine()){
			String s = sc_training_corpus.nextLine();


			if (i > num_FragTutorial - 1){				
				String arr[] = s.split(" ", 2);
				first_token = arr[0].replace(":", "");	// Read the first token

				if (!first_token.equals(last_token)){

					DOC_ID = new ArrayList<String>();
				}
				DOC_ID.add("DOC_"+i);

				if (training_map.containsKey(first_token)){

					training_map.replace(first_token, DOC_ID);
				}else{
					training_map.put(first_token, DOC_ID);

				}

				last_token = first_token;	
			}
			i++;
		}
		sc_training_corpus.close();
		// *****************************************************************************
		Scanner sc = new Scanner(file_training_corpus);
		while (sc.hasNextLine()){
			
		}

	}
}
