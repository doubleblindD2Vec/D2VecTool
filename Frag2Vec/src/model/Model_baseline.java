package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Scanner;

import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

public class Model_baseline {
	private String training_corpus_filename;
	
	Map<String,List<String>> training_map = new HashMap<>();
	Map<String,Map<String,Double>> result_map = new HashMap<>();
	
	List<List<String>> training_list = new ArrayList<>();
	List<String> class_list = new ArrayList<>();
	List<String> class_list_original = new ArrayList<>();
	private String output_name; 
	private final static int num_FragTutorial 	= 57; 
	
	public Model_baseline(String training_corpus){
		training_corpus_filename = "/input/"+ training_corpus + ".txt";
		output_name = training_corpus+"_WORD2VEC.txt";
		
	}
	public void train() throws IOException{
		/*
		 * Step 1: Word2Vec model
		 */
		ClassPathResource resource = new ClassPathResource(training_corpus_filename);
//	    String filePath = new ClassPathResource(training_corpus_filename).getFile().getAbsolutePath();
	    File file_training_corpus = resource.getFile();
        // Strip white space before and after for each line
        SentenceIterator iter = new BasicLineIterator(file_training_corpus);
        // Split on white spaces in the line to get words
        TokenizerFactory t = new DefaultTokenizerFactory();

        t.setTokenPreProcessor(new CommonPreprocessor());
//        t.s

        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(1)
                .iterations(1)
                .epochs(20)
                .learningRate(0.025)
                .layerSize(200)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

        vec.fit();
        
        /********************************************************************/
        read_corpus(file_training_corpus,vec);
		File file_output = new File ("resources/output/model/"+output_name);
		FileWriter fWriter = new FileWriter (file_output);
		PrintWriter writer = new PrintWriter (fWriter);
        
		/*	
		 * 	Step 2: Read Training Corpus and build a Map
		 *  Key		= API name
		 *  Value	= List / Array of its DOC_ID
		*/
		Scanner sc_training_corpus = new Scanner(file_training_corpus);
		int i = 0;
		String first_token 	= "";
		String last_token	= "";

		while (sc_training_corpus.hasNextLine()){
			String s = sc_training_corpus.nextLine();
			if (i > num_FragTutorial - 1){				
				String arr[] = s.split(" ", 2);
				first_token = arr[0].replace(":", "");	// Read the first token	
				if (!first_token.equals(last_token)){
					class_list_original.add(first_token);
					first_token = first_token.replace(".", "").toLowerCase();
					class_list.add(first_token);
//					System.out.println(first_token);
				}
				
				last_token = first_token;	
			}
			i++;
		}
		sc_training_corpus.close();

		/********************************************************************/
		
       /*
        * Step 3: calculate sim ({w}, T)
        * T: tutorial from DOC_0 to DOC_56
        */
		for (int k = 0; k < class_list.size(); k ++){
			Map<String,Double> m = new HashMap<>();
			for (int j = 0; j < num_FragTutorial; j++){
				String TUT_ID = "DOC_"+j;
				double sim = sim_docs(class_list.get(k), TUT_ID, vec);
				System.out.println(class_list.get(k) + " -> " + TUT_ID + ": " +sim);
				m.put(TUT_ID, sim);
			}
			result_map.put(class_list_original.get(k), m);
		}
		
		for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
			entry.setValue(convert_local(entry.getValue()));
			entry.setValue(sortByValue(entry.getValue()));
			System.out.println(entry.getKey());
			System.out.println(entry.getValue().toString());
	
		}
		
		/*
		 * Export model
		 */
		for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
			writer.println(entry.getKey());
			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
				writer.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
			}
			writer.println();
		}
		writer.close();

		
//        System.out.println(vec.s);
//        String api = "javautilcollections";
//        read_corpus(file_training_corpus,vec);
//        for (Entry<String,List<String>> entry : training_map.entrySet()){
//        	String DOC_ID = entry.getKey();
////        	System.out.println(DOC_ID + " " +sim_1(api,DOC_ID,vec));
//        }
        
//        String api = "javautilcollections";
//        int i = 0;
//        
//        for (Entry<String,List<String>> entry : training_map.entrySet()){
//        	double max = 0;
//        	for (int j = 0; j < entry.getValue().size(); j ++){
//        		String word = entry.getValue().get(j);
//        		double sim = vec.similarity(api, word);
//        		System.out.print(word + "\t" + sim+"\t");
//        		max = sim > max ? sim : max;
//        		System.out.println(max);
//        	}
//        	i++;
//        }
        
//        training_map.get(key)
        
        
        
	}
	
	
	private double sim_docs(String w, String DOC_ID,Word2Vec vec){
		return sim_T_w(w, DOC_ID, vec) + sim_w_T(w, DOC_ID, vec);
	}
	
//	sim (T -> {w}) = 
//	sum_{w' in T}(sim(w',w) * idf(w')) / sum_{w' in T}(idf(w')
	private double sim_T_w(String w, String DOC_ID,Word2Vec vec){
		TFIDFCalculator cal = new TFIDFCalculator();
		List<String> list = training_map.get(DOC_ID);
		double sum_sim_idf = 0;
		double sum_idf = 0;
		for (String element : list){
			double sim = vec.similarity(element, w);
			double idf = cal.idf(training_list, element);
			sum_idf += idf;
			sum_sim_idf += sim * idf;
		}
		return sum_sim_idf / sum_idf;
	}
	
	//  calculate sim ({w} -> T) = sim (w,T) = max (sim(w,w')) w' in T
	private double sim_w_T(String w, String DOC_ID,Word2Vec vec){

      double max = 0;
      List<String> list = training_map.get(DOC_ID);
      for (String element : list){
    	  double sim = vec.similarity(w, element);
    	  max = sim > max ? sim : max;
      }
      return max;
	}
	
	
	private void read_corpus(File file, Word2Vec vec) throws FileNotFoundException{
		// Token
		TokenizerFactory t = new DefaultTokenizerFactory();
		
		t.setTokenPreProcessor(new CommonPreprocessor());
		
		Scanner sc = new Scanner(file);
		int i = 0;
		while (sc.hasNextLine()){
			String s = sc.nextLine();
			List<String> list_token = new ArrayList<>();
			Tokenizer tor = t.create(s);
			
	        while (tor.hasMoreTokens()){
	        	String token = tor.nextToken();
	        	if (!token.isEmpty()){
//		        	System.out.print(token+": ");
//		        	System.out.println(vec.similarity("javautilcollections", token));
		        	list_token.add(token);
	        	}

	        }
	        training_list.add(list_token);
	        training_map.put("DOC_"+i, list_token);
	        i++;
		}
		sc.close();
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
	
	public static double convert_range(double x ,double in_min, double in_max, double out_min, double out_max){
		double new_x = (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min; 
		return new_x;
		
	}
	public static Map<String,Double> convert_range_map(Map<String,Double> map, double range_min, double range_max){
		for (Entry<String,Double> entry : map.entrySet()){
			double new_simi = convert_range(entry.getValue(),range_min,range_max,0,1);
			entry.setValue(new_simi);
		}
		return map;
	}
	
	public static Map<String,Double> convert_local(Map<String,Double> map){
		double min = 100;
		double max = -100;
		for (Entry<String,Double> entry : map.entrySet()){
			double value = entry.getValue();
			if (max < value)
				max = value;
			if (min > value)
				min = value;

		}	
		return convert_range_map(map,min,max);
	}
	
	public static void main(String args[]) throws IOException{
		Model_baseline model = new Model_baseline("col_concatinate");
//		Model_baseline model = new Model_baseline("single_sentence");
		model.train();
	}
}
