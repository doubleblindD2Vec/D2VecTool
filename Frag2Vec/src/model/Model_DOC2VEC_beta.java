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
import java.io.FileNotFoundException;
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
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Model_DOC2VEC_beta {


	private final static int num_FragTutorial 	= 57; 
	private static double MAX = -1;
	private static double MIN = 1;
	private final static int num_relevant_pair 	= 56; 
	private final static int k_top = 10;
	
	private String input_name;
	private String model_name;

	private String path;
	
	private Map<String,List<String>> training_map 	= new HashMap<>();
	private Map<String,Map<String,Double>> result_map = new HashMap<>();
	private Map<String,Map<String,Double>> DOC2VEC_map = new HashMap<>();	
	private Map<String,Boolean> oracle_map = new HashMap<>();
	
	public Model_DOC2VEC_beta(String training_corpus){
		input_name 	= training_corpus;
		model_name 	= input_name+"_DOC2VEC";
		path		= "/"+ input_name + ".txt";
	}
	
	public void train() throws FileNotFoundException{
		
		// Configure training Para2Vec
		ClassPathResource resource = new ClassPathResource("input"+path);
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
				.epochs(20)		//Change from 20 to 1
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
		
		/*	
		 * 	Step 1: Read Training Corpus and build a Map
		 *	DOC2VEC_map:
		 *  Key		= DOC ID
		 *  Value	= MAP <TUT ID, Cosine Sim>
		 *  training_map:
		 *  Key		= API name
		 *  Value	= List / Array of its DOC_ID
		 */
				
		Scanner sc_training_corpus = new Scanner(file_training_corpus);
		
		String first_token 	= "";
		String last_token	= "";
		List<String> DOC_ID = new ArrayList<String>();
		
		int i = 0;
		while (sc_training_corpus.hasNextLine()){
			String s = sc_training_corpus.nextLine();
			if (i >= num_FragTutorial){	
				String DOC = "DOC_"+i;
				Map<String,Double> m = new HashMap<>();	
				for (int j = 0; j < num_FragTutorial; j++){
					String TUT = "DOC_"+j;					
					double cosine_sim = vec.similarity(TUT, DOC);
					m.put(TUT, cosine_sim);
					
					if (cosine_sim < MIN)
						MIN = cosine_sim;
					if (cosine_sim > MAX)
						MAX = cosine_sim;
				}
				DOC2VEC_map.put(DOC, m);
				
				
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
		 * Step 2: Normalized vector
		 */
		for (Entry<String,Map<String,Double>> entry : DOC2VEC_map.entrySet()){
			for(Entry<String,Double> entry_in : entry.getValue().entrySet()){
				double value = entry_in.getValue();
				value = (value - MIN) / (MAX - MIN) ;
				entry_in.setValue(value);
			}
		}
		
		// *****************************************************************************
		
	}
	
	public void export(){
		/*
		 * Export model
		 */
//		for (Entry<String,Map<String,Double>> entry : DOC2VEC_map.entrySet()){
//			writer.println(entry.getKey());
//			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
//				writer.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
//			}
//			writer.println();
//		}
//		writer.close();
	}
	
	public void build_API_vector(){
		for (Entry<String, List<String>> entry : training_map.entrySet()){
			Map<String,Double> simi_map  = max(entry.getValue());			
			simi_map	= sortByValue(simi_map);
			simi_map	= convert_range(simi_map);
			result_map.put(entry.getKey(), simi_map);
			
		}
		
		for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
			System.out.println(entry.getKey());
			System.out.println(entry.getValue().toString());
		}
	}
	
	public Map<String,Double> convert_range(Map<String,Double> map){
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
			double new_simi = entry.getValue();
			new_simi		= (new_simi - min) / (max - min) ;
			entry.setValue(new_simi);
		}
		return map;
	}
	
	public Map<String,Double> max(List<String> list){
		/*
		 * Key 	 = DOC ID
		 * Value = a x VSM + (1-a) x DOC2VEC
		 * 
		 */
		Map<String,Double> simi_map  = new TreeMap<>();
		
		for (String DOC : list){
			for (int i = 0; i < num_FragTutorial; i++){
				String TUT = "DOC_"+i;

				double DOC2VEC_score 	= DOC2VEC_map.get(DOC).get(TUT);
				
				double new_score = DOC2VEC_score;
				
				double current_score	= simi_map.getOrDefault(TUT, 2.0);
				
        		if (current_score != 2.0 && current_score < new_score){
        			simi_map.replace(TUT, new_score);

        		}else if(current_score == 2.0){
        			simi_map.put(TUT, new_score); // Put DOC_ID and Similarity 
        		} 			
			}
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
	
	public Model_DOC2VEC_beta measure() throws FileNotFoundException{
		/*
		 * Step 2:	Read Oracle file and build a Map
		 * Key		= 0_Collections -> relation btw DOC_0 and class Collections 
		 * Value	= False (not relevant) or True (relevant)
		*/
		File file_oracle = new File ("resources/input/tutorial_col_oracle");
		Scanner sc_oracle = new Scanner (file_oracle);
		int i = 0;
		while(sc_oracle.hasNextLine()){
			Scanner line = new Scanner(sc_oracle.nextLine());
			while (line.hasNext()){
				String token 	= line.next();
				String key		= i + "_" + token;
				boolean value	= (line.nextInt() == 1) ? true : false;
				oracle_map.put(key, value);
			}				
			i++;
			line.close();
		}
		sc_oracle.close();
		// *****************************************************************************
		return this;

	}
	public void F1(){
		/*
		 * Step 3: Precision & Recall & F1 with Threshold
		 */
		
		for (int j = 0; j <= 100; j++){
			int TP = 0;
			int FP = 0;
			int num_retrieve = 0;
			double T = j * 0.01;
			for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
				String class_name = entry.getKey();
				for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
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
			}
			double precision 	= 100 * (double)TP / num_retrieve;
			double recall    	= 100 * (double)TP / num_relevant_pair;
			double F1			= 2 * precision * recall / (precision + recall);
//			System.out.println("TP: "+TP+" FP: "+FP + " number retrieved: "+num_retrieve);
			System.out.println(T+" "+precision+" "+recall + " " + F1 + " "+ TP);
//			writer.println(T+" "+precision+" "+recall+" "+F1);
		}
		// *****************************************************************************
//		writer.close();
	}
	
	public void K_accuracy (){
		double[] count_k = new double[k_top];	// Numerator Array
		for (Entry<String, Map<String, Double>> entry : result_map.entrySet()){

			int j = 0;
			String class_name = entry.getKey();
			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
				if (j < k_top){
					String key = entry_in.getKey().replaceAll("[^0-9]", "");
					key = key + "_" + class_name;
					if (oracle_map.containsKey(key)){
						if (oracle_map.get(key)){
							for (int k = j; k < count_k.length; k++){
								count_k[k] ++;
							}
							break;
						}					
					}
				}else{
					break;
				}

				j++;
			}

		}
		System.out.println("Top " + k_top + " Accuracy");
//		writer1.println("Top " + k_top + " Accuracy");
		for (int m = 0; m < count_k.length; m++){
			count_k[m] = count_k[m] * 100 / (31);
			//	    	  System.out.println("Top "+(num.length - j)+" accuracy:\n "+num[j]);
			System.out.println(count_k[m]);
//			writer1.println(count_k[m]);
		}
//		writer1.close();
		// *****************************************************************************
	}
	
	public static void main(String[] args) throws Exception {
		Model_DOC2VEC_beta model = new Model_DOC2VEC_beta("col_single");
		model.train();
		model.build_API_vector();
		model.measure();
		


		

		// *****************************************************************************

		/*		Initialize Log file
				Store info of training */
		
//		File file_output = new File ("resources/output/model/"+model_name);
//		FileWriter fWriter = new FileWriter (file_output);
//		PrintWriter writer = new PrintWriter (fWriter);	
		
		// *****************************************************************************
		

	
	}
	
}
