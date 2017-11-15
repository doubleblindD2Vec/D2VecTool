package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Scanner;

public class read_model_FRAPT {
	
	public static Map<String,Boolean> oracle_map = new HashMap<>();
	public static Map<String,Boolean> FRAPT_map = new HashMap<>();
	
	public static Map<String,Map<String,Double>> result_map = new HashMap<>();
	
	public static Map<String,Map<String,Double>> DOC2VEC_map = new HashMap<>();
	
	public static Map<String,Map<String,Double>> VSM_map = new HashMap<>();
	
	
	public final static int num_relevant_pair 	= 56; 
	public final static int k_top = 10;
	
	public final static String model_name = "col_concatinate_DOC2VEC_FULL";
//	public final static String model_name = "col_concatinate_high_layer_DOC2VEC";
//	public final static String model_name = "col_des_1";
//	public final static String model_name = "col_single_range";
//	public final static String model_name = "col_des1_new_max_range";
//	public final static String model_name = "combine2_max_test_norange";
	public final static String Jaccard		= "0.0";
//	public final static String model_name 	= "col_des_1_CombineScore_Jaccard="+Jaccard;
//	public final static String model_name 	= "col_des_1_CombineScore_Jaccard="+Jaccard;
	
//	public final static String model_name 	= "col_concatinate_high_layer_CombineScore_Jaccard="+Jaccard;
	
//	public final static String model_name 	= "col_concatinate_VSM_only";
//	public final static String model_name 	= "col_concatinate_CombineScore_Jaccard="+Jaccard;
//	public final static String model_name 	= "col_single_CombineScore_Jaccard="+Jaccard;
//	public final static String model_name 	= "col_combine2_CombineScore_Jaccard="+Jaccard;
//	public final static String model_name 	= "col_concatinate_clean_edit_DOC2VEC_CombineScore_Jaccard="+Jaccard;
	
//	public final static String model_name 	= "col_concatinate_test";
	public static void main(String args[]) throws IOException{
		
		/*		
		 * Initialize Log file
		 */

//		File file_output1	= new File ("resources/output/result/"+model_name+"_Top_"+k_top+"_Accuracy");
//		FileWriter fWriter1	= new FileWriter (file_output1);
//		PrintWriter writer1	= new PrintWriter (fWriter1);
//		
//		File file_output	= new File ("resources/output/result/"+model_name+"_Fscore");
//		FileWriter fWriter	= new FileWriter (file_output);
//		PrintWriter writer	= new PrintWriter (fWriter);
		
		/*
		 * Step 1: Read Model DOC2VEC file and Build API vector 
		 */
		
		File file = new File ("resources/output/model/"+model_name);
		
		Scanner sc = new Scanner(file);
		while (sc.hasNextLine()){
			Map<String,Double> value = new HashMap<>();
			String key = sc.nextLine();
			String result = sc.nextLine();
			Scanner sc_in = new Scanner (result);
			while (sc_in.hasNext()){
				String DOC_ID		= sc_in.next();
				double cosine_simi 	= sc_in.nextDouble();
				value.put(DOC_ID, cosine_simi);
			}
			value = sortByValue(value);
			DOC2VEC_map.put(key, value);
			sc_in.close();
		}
		sc.close();
		
//		for (Entry<String,Map<String,Double>> entry : DOC2VEC_map.entrySet()){
//			System.out.println(entry.getKey());
//			System.out.println(entry.getValue().toString());
//		}
			
		// ***************************************************************************
		
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
		
		/*
		 * Read FRAPT result and compare
		 */
		File FRAPT_result = new File("resources/input/FRAPT_result_col.txt");
		sc = new Scanner(FRAPT_result);
		while(sc.hasNextLine()){
			String s = sc.nextLine();
			s = s.replace(":", "");
			Scanner sc_in = new Scanner(s);
			while (sc_in.hasNext()){
				String DOC_ID = sc_in.next();
				String class_name = sc_in.next();
				int result = sc_in.nextInt();
				
				
				System.out.println(DOC_ID + " " +class_name +" "+ result);

				
				String key = DOC_ID + "_" + class_name;
				boolean value	= (result == 1) ? true : false;
				FRAPT_map.put(key, value);
//				
				
				
//				String token 	= line.next();
//				String key		= i + "_" + token;
//				boolean value	= (line.nextInt() == 1) ? true : false;
//				oracle_map.put(key, value);
//				FRAPT_map.
				
			}
			sc_in.close();
		}
		sc.close();
		
		/*
		 * Step 3: Precision & Recall & F1 with Threshold
		 */
		
		for (int j = 0; j <= 100; j++){
			int TP = 0;
			int FP = 0;
			int num_retrieve = 0;
			double T = j * 0.01;
//			for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
//				String class_name = entry.getKey();
//				for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
//					if (entry_in.getValue() >= T){
//						String key = entry_in.getKey().replaceAll("[^0-9]", "");
//						key = key + "_" + class_name;
//						if (oracle_map.containsKey(key)){
//							if (oracle_map.get(key))
//								TP ++;
//							else
//								FP ++;
//						}else{
//							FP ++;
//						}
//						num_retrieve ++;
//					}
//				}
//			}
//			double precision 	= 100 * (double)TP / num_retrieve;
//			double recall    	= 100 * (double)TP / num_relevant_pair;
//			double F1			= 2 * precision * recall / (precision + recall);
//			System.out.println(T+" "+precision+" "+recall + " " + F1 + " "+ TP);
			
			int TP_F = 0;	// check model FRAPT
			int num_retrieve_F = 0; // check model FRAPT
			for (Entry<String,Boolean> entry : FRAPT_map.entrySet()){
				String s = entry.getKey();
				String[] st = s.split("_");
				String DOC_ID = "DOC_"+st[0];
				String class_name = st[1];
//				System.out.print(DOC_ID + " " + class_name);
				if (entry.getValue()){	
					if (oracle_map.containsKey(s)){
						if (oracle_map.get(s)){
							TP ++;
							TP_F ++;
						}
					}
					num_retrieve++;	
					num_retrieve_F++;
				}else{
//					System.out.print(" Re-examine");
					if (DOC2VEC_map.get(class_name) != null){
						double sim = DOC2VEC_map.get(class_name).get(DOC_ID);
//						System.out.println(sim);
						if (sim >= T){
							if (oracle_map.containsKey(s)){
								if (oracle_map.get(s))
									TP ++;
							}
//							System.out.print(" TRUE");
							num_retrieve++;
						}else{
//							System.out.print(" T FALSE");
						}
						
					}else{
//						System.out.print(" NOT FOUND FALSE");
					}
				}
//				System.out.println();
			}
//			System.out.println(num_retrieve);
//			System.out.println(TP);
			
			double precision 	= 100 * (double)TP / num_retrieve;
			double recall    	= 100 * (double)TP / num_relevant_pair;
			double F1			= 2 * precision * recall / (precision + recall);
			System.out.println(T+" "+precision+" "+recall + " " + F1 + " "+ TP);
			
//			double precision 	= 100 * (double)TP_F / num_retrieve_F;
//			double recall    	= 100 * (double)TP_F / num_relevant_pair;
//			double F1			= 2 * precision * recall / (precision + recall);
//			System.out.println(T+" "+precision+" "+recall + " " + F1 + " ");
		}
		// *****************************************************************************
//		writer.close();
		
		
		/*
		 * K-Accuracy
		 */
//		double[] count_k = new double[k_top];	// Numerator Array
//		for (Entry<String, Map<String, Double>> entry : result_map.entrySet()){
////			System.out.println(entry.getKey());
////			System.out.println(entry.getValue().toString());
//			int j = 0;
//			String class_name = entry.getKey();
//			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
//				if (j < k_top){
//					String key = entry_in.getKey().replaceAll("[^0-9]", "");
//					key = key + "_" + class_name;
//					if (oracle_map.containsKey(key)){
//						if (oracle_map.get(key)){
//							for (int k = j; k < count_k.length; k++){
//								count_k[k] ++;
//							}
//							break;
//						}					
//					}
//				}else{
//					break;
//				}
//
//				j++;
//			}
//
//		}
//		System.out.println("Top " + k_top + " Accuracy");
////		writer1.println("Top " + k_top + " Accuracy");
//		for (int m = 0; m < count_k.length; m++){
//			count_k[m] = count_k[m] * 100 / (31);
//			//	    	  System.out.println("Top "+(num.length - j)+" accuracy:\n "+num[j]);
//			System.out.println(count_k[m]);
////			writer1.println(count_k[m]);
//		}
////		writer1.close();
		// *****************************************************************************
		
		
	}
	
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
	
}
