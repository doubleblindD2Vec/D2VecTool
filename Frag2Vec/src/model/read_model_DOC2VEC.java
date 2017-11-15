package model;

import java.io.File;
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

public class read_model_DOC2VEC {

	public static Map<String,Boolean> oracle_map = new HashMap<>();
	public static Map<String,Boolean> FRAPT_map = new HashMap<>();
	public static Map<String,Map<String,Double>> result_map = new HashMap<>();
	public static Map<String,Map<String,Double>> baseline_map = new HashMap<>();
	public static Map<String,Map<String,Double>> DOC2VEC_map = new HashMap<>();


	//public final static int num_relevant_pair 	= 42; 
	public final static int k_top = 10;
	public final static String library = "jodatime";
	public final static String model_name = library + "_concatinate_DOC2VEC_NEW";
	public static String secondModel = library + "_concatinate_baseline";
	public final static String oracle_name = "resources/input/tutorial_"+ library+ "_oracle";
	public final static String frapt_file = "resources/input/FRAPT_result_"+ library+ ".txt";

	public static void main(String args[]) throws IOException
	{
		readDoc2VecModel();
		readOracleResult();
		readBaselineModel();
		readFrapt();
		resultDoc2Vec();
//		resultFraptIn();
//		onTopOfFrapt();
//		vsBaseline();
//		vsFrapt();
//		vsFrapt2();
		vsBaseline2();
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
	
	public static void readDoc2VecModel() throws IOException
	{
		/*
		 * Step 1: Read Model DOC2VEC file and Re-build API vector 
		 * Name		= result_map
		 * Key		= API name e.g. java.util.collections
		 * Value	= Map < String Key, Double Value>
		 * 			Key = Tutorial ID e.g. DOC_0, DOC_1
		 * 			Value = cosine sim btw API and tutorial
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
			result_map.put(key, value);
			sc_in.close();
		}
		sc.close();
	}
	
	public static void readBaselineModel() throws IOException
	{
		/*
		 * Step 1: Read Model DOC2VEC file and Re-build API vector 
		 * Name		= result_map
		 * Key		= API name e.g. java.util.collections
		 * Value	= Map < String Key, Double Value>
		 * 			Key = Tutorial ID e.g. DOC_0, DOC_1
		 * 			Value = cosine sim btw API and tutorial
		 */

		File file = new File ("resources/output/model/"+secondModel);

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
			baseline_map.put(key, value);
			sc_in.close();
		}
		sc.close();
	}
	
	public static void readOracleResult() throws IOException
	{
		/*
		 * Step 2:	Read Oracle file and build a Map
		 * Name		= oracle_map
		 * Key		= 0_Collections -> relation btw DOC_0 and class Collections 
		 * Value	= False (not relevant) or True (relevant)
		 */
		File file_oracle = new File (oracle_name);
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
	}
	
	/*
	 * Read FRAPT result
	 */
	public static void readFrapt() throws IOException
	{
		File FRAPT_result = new File(frapt_file);
		Scanner sc = new Scanner(FRAPT_result);
		while(sc.hasNextLine()){
			String s = sc.nextLine();
			s = s.replace(":", "");
			Scanner sc_in = new Scanner(s);
			while (sc_in.hasNext()){
				String DOC_ID = sc_in.next();
				String class_name = sc_in.next();
				int result = sc_in.nextInt();
				String key = DOC_ID + "_" + class_name;
				boolean value	= (result == 1) ? true : false;
				FRAPT_map.put(key, value);
			}
			sc_in.close();
		}
		sc.close();
	}
	
	/*
	 * Result of Doc2Vec
	 * Precision & Recall & F1 with Threshold
	 */
	public static void resultDoc2Vec() throws IOException
	{
		File file_output	= new File ("resources/output/result/"+model_name+"_Fscore");
		FileWriter fWriter	= new FileWriter (file_output);
		PrintWriter writer	= new PrintWriter (fWriter);
		File file	= new File ("resources/output/result/"+model_name+"_vsFrapt");
		FileWriter fw	= new FileWriter (file);
		PrintWriter pw	= new PrintWriter (fw);


		System.out.println("T\tPrecision\tRecall\tF\tTP");
		for (int j = 0; j <= 100; j++){
			int TP = 0, FP = 0, FN = 0; 		int numOut = 0;
			double T = j * 0.01;
			int theyRight = 0, weRight = 0, bothRight = 0;
			//int num_retrieve = 0;
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
			
			for( Entry<String, Boolean> entry: oracle_map.entrySet() )
			{
				int index = entry.getKey().indexOf("_");
				String fragNo = entry.getKey().substring(0,index), className = entry.getKey().substring(index+1);

				if ( result_map.containsKey(className) )
				{
					double result = result_map.get(className).get("DOC_" + fragNo);
					if ( result >= T)
					{
						//We say 1, oracle say 1
						if( entry.getValue() ) 
						{
							TP++;
							if ( FRAPT_map.containsKey(entry.getKey()) ) //they also say 1
							{
								if ( FRAPT_map.get(entry.getKey()) )
								bothRight++;
							}
							else
							{
								weRight++;
							}
						}
						else //We say 1, oracle say 0
						{
							FP++;
						}
					}
					else
					{
						if( entry.getValue() ) //We say 0, oracle say 1
						{
							FN++;
							if( FRAPT_map.containsKey(entry.getKey()) )
							{
								if ( FRAPT_map.get(entry.getKey()) ) theyRight++;
							}
						}
					}
				}

				//Out of vocabulary
				else
				{
//					System.out.println(className);
//					FN++;
//					numOut++;
				}
				
			}
			//			double precision    = 100 * (double)TP / num_retrieve;
			double precision 	= 100 * (double)TP / (TP+FP);
			//			double recall 		= 100 * (double)TP / num_relevant_pair;
			double recall    	= 100 * (double)TP / (TP+FN);
			double F1			= 2 * precision * recall / (precision + recall);
			

			System.out.println(T+"\t"+precision+"\t"+recall + "\t" + F1 + "\t"+ TP);

			pw.println(theyRight + "\t" + weRight + "\t" + bothRight);
			writer.println(T+"\t"+precision+"\t"+recall+"\t"+F1);

		}
		// *****************************************************************************
		writer.close();
		pw.close();

	}
	
	public static void resultFraptIn() throws IOException
	{
		int TPf = 0, FPf = 0, FNf = 0; //For FRAPT accuracy
		for( Entry<String, Boolean> entry: oracle_map.entrySet() )
		{
			String className = entry.getKey().substring(entry.getKey().indexOf("_")+1);
//			if ( result_map.containsKey(className) )
//			{
//			System.out.println(entry.getKey());
				boolean fraptResult = FRAPT_map.get(entry.getKey());
				if ( entry.getValue() ) //Oracle say 1
				{
					if (fraptResult) TPf++; //Frapt say 1
					else FPf++; 			//Frapt say 0
				}
				else						//Oracle say 0
				{
					if (fraptResult) FNf++; //Frapt say 1
				}
//			}
		}
		double precisionF 	= 100 * (double)TPf / (TPf+FPf);
		double recallF    	= 100 * (double)TPf / (TPf+FNf);
		double F1F			= 2 * precisionF * recallF / (precisionF + recallF);
		System.out.println("FRAPT result originally ------------------------");
		System.out.println(precisionF+"\t"+recallF+"\t"+F1F);

	}
	
	public static void onTopOfFrapt() throws IOException
	{
		System.out.println("FRAPT with Para2Vec");
		for (int j = 0; j <= 100; j++)
		{
			int TPf = 0, FPf = 0, FNf = 0; //For FRAPT accuracy
			double T = j * 0.01;
			for( String key: oracle_map.keySet() )
			{
				int index = key.indexOf("_");
				String fragNo = key.substring(0,index), className = key.substring(index+1);
				if ( FRAPT_map.containsKey(key) ) {
					boolean fraptResult = FRAPT_map.get(key);
					boolean oracleResult =  oracle_map.get(key);
					if (fraptResult) 		//frapt say 1
					{
						if ( oracleResult) 	//oracle say 1
						{
							TPf++;
						}
						else  				//oracle say 0 
						{
							FPf++;
						}
					}
					else                  	//frapt say 0 take our result instead
					{
						if ( result_map.containsKey(className) )  
						{
							double result = result_map.get(className).get("DOC_" + fragNo);
							if ( result >= T) //we say 1
							{
								if ( oracleResult) //oracle say 1
								{
									TPf++;
								}
								else  //oracle say 0 
								{
									FPf++;
								}
							}
							else //we say 0
							{
								if ( oracleResult )
								{
									FNf++;
								}
							}
						}
						else
						{
							if ( oracleResult )
							{
								FNf++;
							}
						}
					}
					
				}
			}
			//Calculate Increment Precision and Recall
			double precisionF 	= 100 * (double)TPf / (TPf+FPf);
			double recallF    	= 100 * (double)TPf / (TPf+FNf);
			double F1F			= 2 * precisionF * recallF / (precisionF + recallF);
			System.out.println(j+"\t"+precisionF+"\t"+recallF + "\t" + F1F);
		}
	}
	
	public static void topK() throws IOException
	{
		/*
		 * K-Accuracy
		 */
		/*		
		 * Initialize Log file
		 */
		File file_output1	= new File ("resources/output/result/"+model_name+"_Top_"+k_top+"_Accuracy");
		FileWriter fWriter1	= new FileWriter (file_output1);
		PrintWriter writer1	= new PrintWriter (fWriter1);
		double[] count_k = new double[k_top];	// Numerator Array
		for (Entry<String, Map<String, Double>> entry : result_map.entrySet()){
			//			System.out.println(entry.getKey());
			//			System.out.println(entry.getValue().toString());
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
		writer1.println("Top " + k_top + " Accuracy");
		for (int m = 0; m < count_k.length; m++){
			count_k[m] = count_k[m] * 100 / (31);
			//	    	  System.out.println("Top "+(num.length - j)+" accuracy:\n "+num[j]);
			System.out.println(count_k[m]);
			writer1.println(count_k[m]);
		}
		writer1.close();
		// *****************************************************************************
	}
	
	//They Wrong - We Right vs baseline
	public static void vsBaseline()
	{
		System.out.println("Vs Baseline -----------------------------------------------");
		System.out.println("They wrong \t Total TP");
		for (int j = 0; j <= 100; j++){
			int TP = 0, FP = 0, FN = 0;
			int theyWrong = 0;
			double T = j * 0.01;
			for( Entry<String, Boolean> entry: oracle_map.entrySet() )
			{
				int index = entry.getKey().indexOf("_");
				String fragNo = entry.getKey().substring(0,index), 
						className = entry.getKey().substring(index+1);

				if ( result_map.containsKey(className) )
				{
					double result = result_map.get(className).get("DOC_" + fragNo);
					if ( result >= T)
					{
						//We say 1, oracle say 1
						if( entry.getValue() ) 
						{
							TP++;
							if ( baseline_map.containsKey(className) )
							{
								double baselineResult = baseline_map.get(className).get("DOC_" + fragNo);
								if ( baselineResult < T)
								{
									theyWrong++;
								}
							}
						}
						
					}
				}

			}
			System.out.println(theyWrong+"\t"+TP);
		}
	}
	
	//They Wrong - We Right vs Frapt
	public static void vsFrapt()
	{
		System.out.println("Vs Frapt -----------------------------------------------");
		System.out.println("They wrong \t Total TP");
		for (int j = 0; j <= 100; j++){
			int TP = 0, FP = 0, FN = 0;
			int theyWrong = 0;
			double T = j * 0.01;
			for( Entry<String, Boolean> entry: oracle_map.entrySet() )
			{
				int index = entry.getKey().indexOf("_");
				String fragNo = entry.getKey().substring(0,index), 
						className = entry.getKey().substring(index+1);

				if ( result_map.containsKey(className) )
				{
					double result = result_map.get(className).get("DOC_" + fragNo);
					if ( result >= T)
					{
						//We say 1, oracle say 1
						if( entry.getValue() ) 
						{
							TP++;
							if ( !FRAPT_map.get(entry.getKey()) )
							{
								theyWrong++;
							}
						}
						
					}
				}

			}
			System.out.println(theyWrong+"\t"+TP);
		}
	}
	
	//They Right - We Right - Both Right vs Frapt
		public static void vsFrapt2()
		{
			System.out.println("Vs Frapt 2-----------------------------------------------");
			System.out.println("They Right \t We right \t Both right");
			for (int j = 0; j <= 100; j++)
			{
				int theyRight = 0;
				int weRight = 0;
				int bothRight = 0;
				double T = j * 0.01;
				for( Entry<String, Boolean> entry: oracle_map.entrySet() )
				{
					int index = entry.getKey().indexOf("_");
					String fragNo = entry.getKey().substring(0,index), 
							className = entry.getKey().substring(index+1);

					if ( result_map.containsKey(className) )
					{
						if (entry.getValue() ) //Oracle say 1
						{
							double result = result_map.get(className).get("DOC_" + fragNo);
							if(  result >= T && !FRAPT_map.get(entry.getKey()) ) 
							{
								weRight++;
							}
							if (  result < T && FRAPT_map.get(entry.getKey()) ) 
							{
								theyRight++;
							}
							if (  result >= T && FRAPT_map.get(entry.getKey()) )
							{
								bothRight++;
							}
							
						}
					}

				}
				System.out.println(theyRight + " \t" +  weRight + " \t" +  bothRight);
			}
		}
		
		public static void vsBaseline2()
		{
			System.out.println("Vs Frapt 2-----------------------------------------------");
			System.out.println("They Right \t We right \t Both right");
			for (int j = 0; j <= 100; j++)
			{
				int theyRight = 0;
				int weRight = 0;
				int bothRight = 0;
				double T = j * 0.01;
				for( Entry<String, Boolean> entry: oracle_map.entrySet() )
				{
					int index = entry.getKey().indexOf("_");
					String fragNo = entry.getKey().substring(0,index), 
							className = entry.getKey().substring(index+1);

					if ( result_map.containsKey(className) )
					{
						if (entry.getValue() ) //Oracle say 1
						{
							double result = result_map.get(className).get("DOC_" + fragNo);
							double baseline = baseline_map.get(className).get("DOC_" + fragNo);
							if(  result >= T && baseline < T ) 
							{
								weRight++;
							}
							if (  result < T && baseline >= T ) 
							{
								theyRight++;
							}
							if (  result >= T && baseline >= T )
							{
								bothRight++;
							}
							
						}
					}

				}
				System.out.println(theyRight + " \t" +  weRight + " \t" +  bothRight);
			}
		}
}
