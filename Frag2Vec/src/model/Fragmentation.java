package model;

import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.plot.BarnesHutTsne;
import org.deeplearning4j.text.documentiterator.LabelsSource;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.deeplearning4j.util.ModelSerializer;
import org.jfree.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Fragmentation {

	// Sort Map by Value
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey, 
						Map.Entry::getValue, 
						(e1, e2) -> e1, 
						LinkedHashMap::new
						));
	}

	// Print an Array in format "A, B, C, D..."
	private static void printArray(int[] anArray) {
		for (int i = 0; i < anArray.length; i++) {
			if (i > 0) {
				System.out.print(", ");
			}
			System.out.print(anArray[i]);
		}
	}

	private static final Logger log = LoggerFactory.getLogger(Fragmentation.class);
	
	public final static int num_FragTutorial 	= 57; 
	public final static int num_class 			= 31; 
	public final static int num_relevant_pair 	= 56; 
	public static Map<String,List<String>> training_map = new HashMap<>();
	public static Map<String,Boolean> oracle_map = new HashMap<>();
	public static Map<String,Map<String,Double>> result_map = new HashMap<>();
	
	public static void main(String[] args) throws Exception {
		// Load training corpus

//		String path = "/tutorial_javadoc_col_concatinate.txt";
//				String path = "/collections_single.txt";
				String path = "/input/col_concatinate.txt";
		//		String path = "/description+1.txt";
		//		String path = "/description+2.txt";
		//    	String path = "/tutorial_javadoc_col_full.txt";
		//    	String path = "/tutorial_javadoc_col_full_short.txt";
		//    	String path = "/raw_sentences.txt";

		
		// Configure training Para2Vec
		ClassPathResource resource = new ClassPathResource(path);
		File file_training_corpus = resource.getFile();
		SentenceIterator iter = new BasicLineIterator(file_training_corpus);

		AbstractCache<VocabWord> cache = new AbstractCache<>();

		TokenizerFactory t = new DefaultTokenizerFactory();
		t.setTokenPreProcessor(new CommonPreprocessor());

		LabelsSource source = new LabelsSource("DOC_");


		// Train Doc2Vec 
		ParagraphVectors vec = new ParagraphVectors.Builder()
				.minWordFrequency(1) //change from 1 to 5
				.iterations(5)	// change from 5 to 10
				.epochs(1)		//Change from 20 to 1
				.layerSize(100) // change from 100 to 200
				.learningRate(0.005) // change from 0.025 to 0.01
				.labelsSource(source)
				.windowSize(5) 	// change from 5 to 3
				.iterate(iter)
				.trainWordVectors(true) // change from false to true
				.vocabCache(cache)
				.tokenizerFactory(t)
				.sampling(0)
				.build();

		vec.fit();	// Start the training

		// *****************************************************************************

		/*		Initialize Log file
				Store info of training */

		File file_output = new File ("log/output.txt");
		FileWriter fWriter = new FileWriter (file_output);
		PrintWriter writer = new PrintWriter (fWriter);
//		writer.println (new Date());
		
		// *****************************************************************************
		
		/*	
		 * 	Step 1: Read Training Corpus and build a Map
		 *  Key		= API name
		 *  Value	= List / Array of its DOC_ID
		*/

//		Map<String,List<String>> training_map = new HashMap<>();
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
				
//				System.out.print(first_token +" "+i);
				if (!first_token.equals(last_token)){
//					System.out.println("\nClear DOC ID");
					DOC_ID = new ArrayList<String>();
				}
				DOC_ID.add("DOC_"+i);
//				System.out.println(DOC_ID.toString());
				if (training_map.containsKey(first_token)){
					
					training_map.replace(first_token, DOC_ID);
//					System.out.println(" REPLACE "+first_token);
//					System.out.println(training_map.get(first_token).size());
				}else{
					training_map.put(first_token, DOC_ID);
//					System.out.println(" PUT "+first_token);
//					System.out.println(training_map.size());
					
				}
				
				last_token = first_token;	
			}
			i++;
		}
		sc_training_corpus.close();
		
//		for (Entry<String,List<String>> entry : training_map.entrySet()){
//			System.out.println(entry.getKey() + ": " + entry.getValue().toString()); // Test -> Wroong
//		}
		
//		System.out.println(training_map.get("java.util.PriorityQueue").toString());
//		System.out.println(training_map.get("java.util.concurrent.CopyOnWriteArraySet").toString());
		
		// *****************************************************************************
		
		/*
		 * Step 2:	Read Oracle file and build a Map
		 * Key		= 0_Collections -> relation btw DOC_0 and class Collections 
		 * Value	= False (not relevant) or True (relevant)
		*/
//		Map<String,Boolean> oracle_map = new HashMap<>();
		File file_oracle = new File ("resources/input/tutorial_col_oracle");
		Scanner sc_oracle = new Scanner (file_oracle);
		i = 0;
		while(sc_oracle.hasNextLine()){
			Scanner line = new Scanner(sc_oracle.nextLine());
			while (line.hasNext()){
				String token 	= line.next();
				String key		= i + "_" + token;
				boolean value	= (line.nextInt() == 1) ? true : false;
				oracle_map.put(key, value);
				
//				System.out.println("key: "+key);
//				System.out.println("value: "+value);
			}				
			i++;
			line.close();
		}
		sc_oracle.close();
		// *****************************************************************************
		
		/*
		 * Step 3: Build API vector using MAX / AVERAGE function
		 * Cosine similarity
		 */
//		Map<String,Map<String,Double>> result_map = new HashMap<>();
		for (Entry<String, List<String>> entry : training_map.entrySet()){
			Map<String,Double> simi_map  = max(entry.getValue(),vec);
//			Map<String,Double> simi_map  = average(entry.getValue(),vec);
			simi_map	= sortByValue(simi_map);
			result_map.put(entry.getKey(), simi_map);
		}
		
//		Test Step 3 -> OK
//		for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
//			System.out.println(entry.getKey() + "\n" + entry.getValue().toString());
//		}
		
//		Test max function
//		Map<String,Double> simi_map  = new HashMap<>();
//		simi_map = max(training_map.get("java.util.Collections"),vec);
//		simi_map	= sortByValue(simi_map);
//		for (Entry<String,Double> entry : simi_map.entrySet()){
//			System.out.println(entry.getKey() + "\t" + entry.getValue());
//		}
		
		// *****************************************************************************
		
		/*
		 * R-Precision implementation
		 */
		Map<String,Integer> size_map = new HashMap<>();
		size_map.put("java.util.Collections", 					7);
		size_map.put("java.util.List", 							4);
		size_map.put("java.util.Comparator",					1);
		size_map.put("java.util.Collection",					5);
		size_map.put("java.util.AbstractList",					2);
		size_map.put("java.util.AbstractCollection",			1);
		size_map.put("java.util.AbstractMap",					1);
		size_map.put("java.util.Set",							3);
		size_map.put("java.util.Map",							3);
		size_map.put("java.util.Deque",							2);
		size_map.put("java.util.LinkedList",					2);
		size_map.put("java.util.ArrayDeque",					1);
		size_map.put("java.util.concurrent.LinkedBlockingDeque",1);
		size_map.put("java.util.concurrent.BlockingQueue",		2);
		size_map.put("java.util.concurrent.ConcurrentMap",		2);
		size_map.put("java.util.ArrayList",						2);
		size_map.put("java.util.concurrent.CopyOnWriteArrayList",1);
		size_map.put("java.util.Vector",						1);
		size_map.put("java.util.LinkedHashMap",					1);
		size_map.put("java.util.EnumMap",						1);
		size_map.put("java.util.WeakHashMap",					1);
		size_map.put("java.util.IdentityHashMap",				1);
		size_map.put("java.util.concurrent.ConcurrentHashMap",	1);
		size_map.put("java.util.Queue",							1);
		size_map.put("java.util.PriorityQueue",					1);
		size_map.put("java.util.HashSet",						3);
		size_map.put("java.util.TreeSet",						1);
		size_map.put("java.util.LinkedHashSet",					1);
		size_map.put("java.util.EnumSet",						1);
		size_map.put("java.util.concurrent.CopyOnWriteArraySet",1);
		size_map.put("java.util.SortedMap",						1);

		
		/*
		 * Step 4: Precision & Recall Calculation
		 */
//		int TP = 0;
//		int TN = 0;
//		int FP = 0;
//		int FN = 0;
//		int k_nearest = 3;
//		int num_retrieve = k_nearest * num_class;	
		
		
		// Threshold implementation
		
		for (double T = 0; T <= 1; T = T + 0.01){
			int TP = 0;
			int FP = 0;
			int num_retrieve = 0;
			for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
				
				String class_name = entry.getKey();
				writer.println(class_name);
				for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
					writer.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
					if (entry_in.getValue() >= T){
						String key = entry_in.getKey().replaceAll("[^0-9]", "");
						key = key + "_" + class_name;
						if (oracle_map.containsKey(key)){
							if (oracle_map.get(key))
								TP ++;
							else
								FP ++;
						}else{
							FP ++;
						}
						num_retrieve ++;
					}

				}
				writer.println();
			}
//			System.out.println("Threshold:\t"+T);
//			System.out.println("TP:\t"+TP);
//			System.out.println("FP:\t"+FP);
//			System.out.println("Retrieved result:\t"+num_retrieve);
//			System.out.println("Precision:\t"+ (100 * (double)TP / num_retrieve));
//			System.out.println("Recall:\t"+ (100 * (double)TP / num_relevant_pair));
//			System.out.println("**************************************************");
			double precision = 100 * (double)TP / num_retrieve;
			double recall    = 100 * (double)TP / num_relevant_pair;
			System.out.println(T+" "+precision+" "+recall);
			
			
		}
		
		writer.close();

//		int num_retrieve = 0;
//		for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
//			String class_name = entry.getKey();
//			int k = size_map.get(class_name);
//			int j = 0;
//			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
//				if (j < k){
//					String key = entry_in.getKey().replaceAll("[^0-9]", "");
//					key = key + "_" + class_name;
//					if (oracle_map.containsKey(key)){
//						if (oracle_map.get(key))
//							TP ++;
//						else
//							FP ++;
//					}else{
//						FP ++;
//					}
//					num_retrieve++;	
//				}
//
//				j++;
//			}
//			
//		}
		
		
		// Test Collections
//		String name = "java.util.Collections";
//		Map<String,Double> test = result_map.get(name);
//		int num_revelant_test = 7; // collections
//
//		int m = 1;
//		for (Entry<String,Double> entry : test.entrySet()){
//			if (m <= 5){
//				String key = entry.getKey().replaceAll("[^0-9]", "");
//				key = key + "_" + name;
//				if (oracle_map.containsKey(key)){
//					if (oracle_map.get(key))
//						TP ++;
//					else
//						FP ++;
//				}else
//					FP ++;
//				
//				System.out.println("Top k:\t"+m);
//				System.out.println("TP:\t"+TP);
//				System.out.println("FP:\t"+FP);
//				System.out.println("Precision:\t"+ (100 * TP / m));
//				System.out.println("Recall:\t\t"+ (100 * TP / num_revelant_test));
//				System.out.println("*****************************************");
//			}
//				
//			m++;
//		}
	
		
//		System.out.println("TP:\t"+TP);
//		System.out.println("FP:\t"+FP);
//		System.out.println("Retrieved result:\t"+num_retrieve);
//		System.out.println("Precision:\t"+ (100 * (double)TP / num_retrieve));
//		System.out.println("Recall:\t"+ (100 * TP / num_relevant_pair));
		
	}
	
	public static Map<String,Double> max(List<String> list,ParagraphVectors vec){
//		double[] cosine_simi = new double[num_FragTutorial];
		/*
		 * Key 	 = DOC ID
		 * Value = Cosine Similarity
		 */
		Map<String,Double> simi_map  = new HashMap<>();
		for (String ID : list){
			for (int i = 0; i < num_FragTutorial; i++){
				double new_simi	= vec.similarity(ID, "DOC_"+i);
				double current_simi	= simi_map.getOrDefault("DOC_"+i, 2.0);
        		if (current_simi != 2.0 && current_simi < new_simi){
        			simi_map.replace("DOC_"+i, new_simi);
        		}else if(current_simi == 2.0){
        			simi_map.put("DOC_"+i, new_simi); // Put DOC_ID and Similarity 
        		}
			}
		}
		
		return simi_map;
	}
	
	public static Map<String,Double> average(List<String> list,ParagraphVectors vec){
		Map<String,Double> simi_map  = new HashMap<>();
		for (String ID : list){
			for (int i = 0; i < num_FragTutorial; i++){
				String TUT_ID = "DOC_"+i;
				double new_simi	= vec.similarity(ID, TUT_ID);
				double current_simi	= simi_map.getOrDefault(TUT_ID, 2.0);
        		if (current_simi != 2.0){
        			new_simi = new_simi + simi_map.get(TUT_ID);
        			simi_map.replace(TUT_ID, new_simi);
        		}else if(current_simi == 2.0){
        			simi_map.put(TUT_ID, new_simi); // Put DOC_ID and Similarity 
        		}				
			}
		}
//		System.out.println("Size: "+list.size());
		for (Entry<String,Double>entry : simi_map.entrySet()){
//			System.out.println(entry.getValue());
			double value = entry.getValue() / list.size();
//			training_map.get(key)
			entry.setValue(value);
//			System.out.println(entry.getValue());
		}
		
		return simi_map;
	}
	
}
