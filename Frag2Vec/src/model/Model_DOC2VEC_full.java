package model;

import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.models.embeddings.learning.SequenceLearningAlgorithm;
import org.deeplearning4j.models.embeddings.learning.impl.sequence.DM;
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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Model_DOC2VEC_full {

	
	public static int num_FragTutorial; 

	
	public static Map<String,List<String>> training_map = new HashMap<>();
	public static Map<String,Map<String,Double>> result_map = new HashMap<>();
	
	// Load training corpus
	public static String input_name = "jodatime_concatinate";
	public static String model_name = input_name+"_DOC2VEC_NEW";
	public static String path = "/"+ input_name + ".txt";
	
	public static void main(String[] args) throws Exception 
	{
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter number of fragment tutorial: ");
		num_FragTutorial = scanner.nextInt();
		scanner.close();
		/*
		 *  Configure and Train Para2Vec
		 */
		//ClassPathResource resource = new ClassPathResource("C:\\Research\\Tutorial Project\\Frag2Vec\\Frag2Vec\\resources\\input"+path);
		//File file_training_corpus = resource.getFile();
//		File file_training_corpus = new File ("C:\\Research\\Tutorial Project\\Frag2Vec\\Frag2Vec\\resources\\input" + path);
		File file_training_corpus = new File ("F:\\Study\\Research\\Tutorial Project\\Frag2Vec\\Frag2Vec\\resources\\input" + path);
		SentenceIterator iter = new BasicLineIterator(file_training_corpus);

		AbstractCache<VocabWord> cache = new AbstractCache<>();

		TokenizerFactory t = new DefaultTokenizerFactory();
		t.setTokenPreProcessor(new CommonPreprocessor());

		LabelsSource source = new LabelsSource("DOC_");


		// Train Doc2Vec 
		ParagraphVectors vec = new ParagraphVectors.Builder()
				.minWordFrequency(1) //change from 1 to 5
				.iterations(5)	// change from 5 to 10
				.epochs(20)		//Change from 20 to 1
				.layerSize(200) // change from 100 to 200
				.learningRate(0.005) // change from 0.025 to 0.01
				.labelsSource(source)
				.windowSize(5) 	// change from 5 to 3
				.iterate(iter)
				.trainWordVectors(true) // change from false to true
				.vocabCache(cache)
				.tokenizerFactory(t)
				.sampling(0)
				.sequenceLearningAlgorithm(new DM<>())
				.build();
				

		vec.fit();	// Start the training

		// *****************************************************************************

		/*		Initialize Log file
				Store info of training */

		
		File file_output1 = new File ("F:\\Study\\Research\\Tutorial Project\\Frag2Vec\\Frag2Vec\\resources\\output\\model\\"+model_name);
		FileWriter fWriter1 = new FileWriter (file_output1);
		PrintWriter writer1 = new PrintWriter (fWriter1);	
		
		// *****************************************************************************
		
		/*	
		 * 	Step 1: Read Training Corpus and build a Map training_map
		 *  Name	= training_map
		 *  Key		= API name e.g. java.util.collections
		 *  Value	= List / Array of its DOC_ID [DOC_57,DOC_58,DOC_59] -> Which DOC_ID in Javadoc belongs to which API 
		*/
		
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
		 * Step 2: Build API vector using MAX / AVERAGE function
		 * Name		= result_map
		 * Key		= API name e.g. java.util.collections
		 * Value	= Map < String Key, Double Value>
		 * 			Key = Tutorial ID e.g. DOC_0, DOC_1
		 * 			Value = cosine sim btw API and tutorial
		 */

		for (Entry<String, List<String>> entry : training_map.entrySet()){
			Map<String,Double> simi_map  = max(entry.getValue(),vec);			
			simi_map	= sortByValue(simi_map);
			result_map.put(entry.getKey(), simi_map);
		}
		// *****************************************************************************
		
//		globalNorm(result_map); //Call if global normalization
		/*
		 * Step 3: Export model
		 */
		for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
			System.out.println(entry.getKey());
			writer1.println(entry.getKey());
			
			entry.setValue(map_range(entry.getValue()));  //Call if local normalization
			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
				System.out.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
				writer1.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
			}
			System.out.println();
			writer1.println();

		}
		writer1.close();
		System.out.println("Export Model: "+ file_output1.getAbsolutePath());
		
		// *****************************************************************************
	}


	private static void globalNorm(Map<String, Map<String, Double>> result_map) 
	{
		double min = 1;
		double max = 0;
		for ( String api : result_map.keySet() )
		{
			for ( String doc : result_map.get(api).keySet() ) 
			{
				double value = result_map.get(api).get(doc);
				if (value >= max)
					max = value;
				if (value <= min)
					min = value;
			}
		}
		
		for ( String api : result_map.keySet() )
		{
			for ( String doc : result_map.get(api).keySet() ) 
			{
				double value = result_map.get(api).get(doc);
				double newValue = ( value - min ) / ( max - min );
				result_map.get(api).put(doc, newValue);
			}
		}


	}


	//For Local normalization
	public static Map<String,Double> map_range(Map<String,Double> map){
		double min = 1;
		double max = -1;
		for (Entry<String,Double> entry : map.entrySet()){
			double value = entry.getValue();
			if (max < value)
				max = value;
			if (min > value)
				min = value;
		}	
		
		for (Entry<String,Double> entry : map.entrySet()){
			double new_simi = ( entry.getValue() - min ) / (max - min);
			entry.setValue(new_simi);
		}
		return map;
	}
	
	public static Map<String,Double> max(List<String> list,ParagraphVectors vec){
		/*
		 * Key 	 = DOC ID
		 * Value = Cosine Similarity
		 */
		Map<String,Double> simi_map  = new TreeMap<>();
		
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
		for (Entry<String,Double>entry : simi_map.entrySet()){
			double value = entry.getValue() / list.size();
			entry.setValue(value);
		}
		
		return simi_map;
	}
	
	// Sort Map by Value
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		Map<K, V> m = map.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey, 
						Map.Entry::getValue, 
						(e1, e2) -> e1, 
						LinkedHashMap::new
						));
		return m;
	}
	
	
}
