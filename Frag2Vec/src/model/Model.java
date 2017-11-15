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


public class Model {
	
//	public final static String input_name 			= "col_concatinate";
//	public final static String input_name 			= "col_concatinate_high_layer";
	public final static String input_name 			= "col_des_1";
//	public final static String input_name 			= "col_single";
//	public final static String input_name 			= "col_combine2";
//	public final static String input_name 			= "col_concatinate_clean_edit";
	public final static String model_name_VSM 		= input_name + "_VSM";
	public final static String model_name_JAC 		= input_name + "_JAC";
	public final static String model_name_DOC2VEC 	= input_name + "_DOC2VEC";
	
	public static Map<String,Map<String,Double>> DOC2VEC_map 	= new HashMap<>();
	public static Map<String,Map<String,Double>> VSM_map 		= new HashMap<>();
	public static Map<String,Map<String,Double>> JAC_map 		= new HashMap<>();
	
	public final static int num_FragTutorial 	= 57; 
	public final static int num_class 			= 31; 
	public final static int num_relevant_pair 	= 56; 
	
	public final static double JAC_threshold 	= 0.0; 
	
	public static Map<String,List<String>> training_map 	= new HashMap<>();
	public static Map<String,Map<String,Double>> result_map = new HashMap<>();
//	public Model(){
//		
//	}
		
	public static void main(String args[]) throws IOException{
		/*		
		 * Initialize Log file
		 */

		File file_output	= new File ("resources/output/model/"+input_name+"_CombineScore_Jaccard="+JAC_threshold);
//		File file_output	= new File ("resources/output/model/col_concatinate_VSM_only");
//		File file_output	= new File ("resources/output/model/col_concatinate_");
		FileWriter fWriter	= new FileWriter (file_output);
		PrintWriter writer	= new PrintWriter (fWriter);
		/**********************************************************************************/
		/*
		 * Step 1: Read Model 
		 * DOC2VEC file and Build DOC2VEC vector
		 * VSM file and Build VSM vector
		 * JAC file and Build JACCARD vector
		 */
		
		DOC2VEC_map = read_model(model_name_DOC2VEC, DOC2VEC_map);
		JAC_map 	= read_model(model_name_JAC, JAC_map);
		VSM_map		= read_model(model_name_VSM, VSM_map);
		
		/**********************************************************************************/
		
//		for (Entry<String,Map<String,Double>> entry : JAC_map.entrySet()){
//			System.out.println(entry.getKey());
//			System.out.println(entry.getValue().toString());
//		}	
		
				
		/*	
		 * 	Step 1: Read Training Corpus and build a Map
		 *  Key		= API name
		 *  Value	= List / Array of its DOC_ID
		 */
		File file_training_corpus	= new File ("resources/input/"+input_name+".txt");
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
		/**********************************************************************************/
		
		/*
		 * Step 2: Build API vector using MAX / AVERAGE function
		 */
		for (Entry<String, List<String>> entry : training_map.entrySet()){
			Map<String,Double> simi_map  = max(entry.getValue());			
			simi_map	= sortByValue(simi_map);
			simi_map	= convert_local(simi_map);
			result_map.put(entry.getKey(), simi_map);
		}
		
		/**********************************************************************************/
		/*
		 * Step 3: Export model
		 */
		for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
			writer.println(entry.getKey());
			System.out.println(entry.getKey());
			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
				writer.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
				System.out.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
			}
			writer.println();
			System.out.println();
		}
		writer.close();
		/**********************************************************************************/
		
		
	}
	
	public static Map<String,Double> max(List<String> list){
		/*
		 * Key 	 = DOC ID
		 * Value = a x VSM + (1-a) x DOC2VEC
		 * 
		 */
		Map<String,Double> simi_map  = new TreeMap<>();
		
		for (String DOC : list){
			for (int i = 0; i < num_FragTutorial; i++){
				String TUT = "DOC_"+i;
//				
				double VSM_score 		= VSM_map.get(DOC).get(TUT);
				double DOC2VEC_score 	= DOC2VEC_map.get(DOC).get(TUT);
				double JAC_score		= JAC_map.get(DOC).get(TUT);
				int	alpha				= JAC_score <= JAC_threshold ? 0 : 1;
				
				double new_score 		= alpha * VSM_score + (1-alpha) * DOC2VEC_score;
				
//				double new_score = VSM_score;
				
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
	
	public static Map<String,Map<String,Double>> read_model(String model_name,Map<String,Map<String,Double>> model_map) throws FileNotFoundException{
		File file = new File ("resources/output/model/"+model_name);
		Scanner sc = new Scanner(file);
		while (sc.hasNextLine()){
			Map<String,Double> value = new HashMap<>();
			String DOC = sc.nextLine();
			String result = sc.nextLine();
			Scanner sc_in = new Scanner (result);
			while (sc_in.hasNext()){
				String TUT			= sc_in.next();
				double cosine_simi 	= sc_in.nextDouble();
				value.put(TUT, cosine_simi);
			}
			model_map.put(DOC, value);
			sc_in.close();
		}
		sc.close();
		return model_map;
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
		double min = 1;
		double max = -1;
		for (Entry<String,Double> entry : map.entrySet()){
			double value = entry.getValue();
			if (max < value)
				max = value;
			if (min > value)
				min = value;

		}	
		return convert_range_map(map,min,max);
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
