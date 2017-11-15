package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.EndingPreProcessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

public class Model_VSM {
	public final static int num_FragTutorial 	= 57; 
	public static double MAX_1 = -1;
	public static double MIN_1 = 1;
	
	public static double MAX_2 = -1;
	public static double MIN_2 = 1;

//	public final static String input_name = "col_concatinate";
//	public final static String input_name = "col_concatinate_clean_edit";
//	public final static String input_name = "col_single";
//	public final static String input_name = "col_combine2";
	public final static String input_name = "col_des_1";
	public final static String model_name_VSM = input_name + "_VSM";
	public final static String model_name_JAC = input_name + "_JAC";
	public static String path = "/"+ input_name + ".txt";

	public static void main(String args[]) throws IOException{
		
		/*		Initialize Log file
		Store info of training */

		File file_output1		= new File ("resources/output/model/"+model_name_VSM);
		FileWriter fWriter1 	= new FileWriter (file_output1);
		PrintWriter writer1		= new PrintWriter (fWriter1);
		
		File file_output2		= new File ("resources/output/model/"+model_name_JAC);
		FileWriter fWriter2 	= new FileWriter (file_output2);
		PrintWriter writer2 	= new PrintWriter (fWriter2);
		// *****************************************************************************

		/*	
		 * 	Step 1: Read Training Corpus and build a VSM map and Jaccard Map
		 *  Key		= DOC ID
		 *  Value	= MAP <TUT ID, cosine score / Jaccard score>
		*/
		
		File file_training_corpus = new File("resources/input"+path);
		Map<String,Map<String,Double>> TF_IDF_map 	= new HashMap<>();
		Map<String,Map<String,Double>> term_map		= new HashMap<>();
		
		Map<String,Map<String,Double>> VSM_map = new HashMap<>();	
		Map<String,Map<String,Double>> Jac_map = new HashMap<>();
		
		TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
		List<List<String>> docs = new ArrayList<List<String>>();
		
		System.out.println("Step 1");
		Scanner sc = new Scanner(file_training_corpus);
		while (sc.hasNextLine()){
			
			String line = sc.nextLine();
			
			//remove stop words
			line = line.replaceAll("[.,?:;!//(//)//[//]]", " ");
			
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
			docs.add(tokens);
	
		}
		sc.close();
		
		int i =0;
		sc = new Scanner(file_training_corpus);
		while (sc.hasNextLine()){
			System.out.println(i);
			String line = sc.nextLine();
			
			//remove stop words
			line = line.replaceAll("[.,?:;!//(//)//[//]]", " ");
			
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
//			docs.add(tokens);
			
			Map<String,Double> m1 = new HashMap<>();
			Map<String,Double> m2 = new HashMap<>();
			TFIDFCalculator cal = new TFIDFCalculator();
			for (String s : tokens){
				if (!m2.containsKey(s)){
					m1.put(s, cal.tfIdf(tokens, docs, s));
//					m1.put(s, cal.tf(tokens, s));
//					List<String> doc, List<List<String>> docs, String term
					
					m2.put(s, cal.term_count(tokens, s));
				}		
			}

			TF_IDF_map.put("DOC_"+i, m1);
			term_map.put("DOC_"+i, m2);
			
			if (i >= num_FragTutorial){	
				String DOC = "DOC_"+i;

				Map<String,Double> m_tfidf = new HashMap<>();	
				Map<String,Double> m_term = new HashMap<>();	
				for (int j = 0; j < num_FragTutorial; j++){
					String TUT = "DOC_"+j;	
					double cosine_sim 	= cosine_sim(TF_IDF_map.get(TUT),TF_IDF_map.get(DOC));
					
					double jaccard		= jaccard_compute(term_map.get(TUT),term_map.get(DOC));

					m_tfidf.put(TUT, cosine_sim);
					m_term.put(TUT, jaccard);
					
					if (cosine_sim < MIN_1)
						MIN_1 = cosine_sim;
					if (cosine_sim > MAX_1)
						MAX_1 = cosine_sim;
					
					if (jaccard < MIN_2)
						MIN_2 = jaccard;
					if (jaccard > MAX_2)
						MAX_2 = jaccard;
				}
				VSM_map.put(DOC, m_tfidf);
				Jac_map.put(DOC, m_term);
				
			}
			
			i++;
		}
		
		sc.close();
		
		// *****************************************************************************

		/*
		 * Step 2: Normalized vector
		 */
		System.out.println("Step 2");
		for (Entry<String,Map<String,Double>> entry : VSM_map.entrySet()){
			for(Entry<String,Double> entry_in : entry.getValue().entrySet()){
				double value = entry_in.getValue();
				value = (value - MIN_1) / (MAX_1 - MIN_1) ;
				entry_in.setValue(value);
			}
		}
		
		for (Entry<String,Map<String,Double>> entry : Jac_map.entrySet()){
			for(Entry<String,Double> entry_in : entry.getValue().entrySet()){
				double value = entry_in.getValue();
				value = (value - MIN_2) / (MAX_2 - MIN_2) ;
				entry_in.setValue(value);
			}
		}
		
		// Display
//		for (Entry<String,Map<String,Double>> entry : Jac_map.entrySet()){
//			System.out.println(entry.getKey());
//			System.out.println(entry.getValue().toString());
//		}	
		// *****************************************************************************
		
		/*
		 * Step 1: Export model
		 */
		System.out.println("Step 3");
		for (Entry<String,Map<String,Double>> entry : VSM_map.entrySet()){
			writer1.println(entry.getKey());
			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
				writer1.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
			}
			writer1.println();
		}
		writer1.close();
		
		for (Entry<String,Map<String,Double>> entry : Jac_map.entrySet()){
			writer2.println(entry.getKey());
			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
				writer2.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
			}
			writer2.println();
		}
		writer2.close();		
		// *****************************************************************************
	}
	

	public static double cosine_sim(Map<String,Double> m1, Map<String,Double> m2){
		double n = 0;
		double cosine_sim = 0;
		for (Entry<String,Double> entry : m1.entrySet()){
			String s	= entry.getKey();
			double x1 	= entry.getValue();
			double x2	= m2.getOrDefault(s, 0.0);
			double x	= x1 * x2;
			
			n = n + x;		
			
		}
		cosine_sim = n / (get_length(m1) * get_length(m2));
		return cosine_sim;
	}
	
	public static double get_length(Map<String,Double> m){
		double length = 0.0;
		for (Entry<String,Double> entry : m.entrySet()){
			length = length + entry.getValue() * entry.getValue();
		}	
		return Math.sqrt(length);	

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
