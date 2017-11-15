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

import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.EndingPreProcessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

public class jaccard {
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
		
		/*
		 * Step 2: Calculate Jaccard score for each DOC - TUT
		 * 
		 */
		for (Entry<String, List<String>> entry : training_map.entrySet()){
//			Map<String,Double> simi_map ;
////			Map<String,Double> simi_map  = average(entry.getValue(),vec);
//			
//			result_map.put(entry.getKey(), simi_map);
		}
		// *****************************************************************************
		

	}
	public static List<String> tokenizer(String line){
		//remove stop words
		line = line.replaceAll("[.,?:;!//(//)//[//]]", " ");
		TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
		EndingPreProcessor end = new EndingPreProcessor();
		line = end.preProcess(line); // Gets rid of endings: ed,ing, ly, s, .
		Tokenizer tokenizer = tokenizerFactory.create(line);

		//get the whole list of tokens
		List<String> tokens = tokenizer.getTokens();

		//iterate over the tokens
		while(tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			tokens.add(token);
		}
		return tokens;
	}

	public double term_count(List<String> doc, String term) {
		double result = 0.0;
		for (String word : doc) {
			if (term.equalsIgnoreCase(word))
				result++;
		}
		return result;
	}

	public static Integer interference(Map<String,Double> Q, Map<String,Double> C){
		int num_element = 0;
		for (Entry<String,Double> entry : Q.entrySet()){
			String key = entry.getKey();
			if (C.containsKey(key)){
				if (entry.getValue() < C.get(key))
					num_element += entry.getValue();
				else
					num_element += C.get(key);
			}
		}
		return num_element;
	}

	public static Integer union(Map<String,Double> Q, Map<String,Double> C){
		return length(Q) + length(C) - interference(Q, C);
	}

	public static Integer length(Map<String,Double> m){
		int length = 0;
		for (Entry<String,Double> entry : m.entrySet()){
			length += entry.getValue();
		}
		return length;
	}

	public static double jaccard_compute(Map<String,Double> Q, Map<String,Double> C){
		double jaccard = 0;
		jaccard = (double)interference(Q, C) / union(Q, C);
		return jaccard;

	}
}
