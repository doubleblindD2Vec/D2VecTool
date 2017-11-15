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

public class Model_DOC2VEC {


	public final static int num_FragTutorial 	= 42; 
	public static double MAX = -1;
	public static double MIN = 1;
	
	
	// Load training corpus
//	public final static String input_name = "col_concatinate";
//	public final static String input_name = "col_concatinate_clean_edit";
	public final static String input_name = "jcol_concatinate";
//	public final static String input_name = "col_single";
//	public final static String input_name = "col_combine2";
	public final static String model_name = input_name+"_DOC2VEC";
	public static String path = "/"+ input_name + ".txt";
	
	// Build constructor for Model_DOC2VEC
//	public Model_DOC2VEC(String training_corpus, ){
//		
//	}
	
	public static void main(String[] args) throws Exception {
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
				.layerSize(200) // change from 100 to 200
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
//		System.out.println(vec.similarity("DOC_0", "DOC_1"));
		// *****************************************************************************

		/*		Initialize Log file
				Store info of training */
		
		File file_output = new File ("resources/output/model/"+model_name);
		FileWriter fWriter = new FileWriter (file_output);
		PrintWriter writer = new PrintWriter (fWriter);	
		
		// *****************************************************************************
		
		/*	
		 * 	Step 1: Read Training Corpus and build a Map
		 *  Key		= DOC ID
		 *  Value	= MAP <TUT ID, Cosine Sim>
		*/
		Map<String,Map<String,Double>> DOC2VEC_map = new HashMap<>();		
		
		Scanner sc_training_corpus = new Scanner(file_training_corpus);
		int i = 0;
		while (sc_training_corpus.hasNextLine()){
			sc_training_corpus.nextLine();
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
		/*
		 * Export model
		 */
		for (Entry<String,Map<String,Double>> entry : DOC2VEC_map.entrySet()){
			writer.println(entry.getKey());
			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
				writer.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
			}
			writer.println();
		}
		writer.close();
		
		// *****************************************************************************
	
	}
	
}
