package parsing;

import java.util.Scanner;

import utils.FileIO;

public class ParallelCorpusParser {
	private String pathList;

	public ParallelCorpusParser(String pathList) {
		this.pathList = pathList;
	}
	
	public void generateParallelCorpus(final String outPath, final boolean recursive, final boolean testing) {
		String content = FileIO.readStringFromFile(pathList);
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			final String line = sc.nextLine();
			int index = line.indexOf(" ");
			final String name = line.substring(0, index), path = line.substring(index+1);
			new Thread(new Runnable() {
				@Override
				public void run() {
					ProjectCorpusParser pcp = new ProjectCorpusParser(path, testing);
					pcp.generateParallelCorpus(outPath + "/" + name, recursive);
				}
			}).start();
		}
		sc.close();
	}
	
	public static void main(String[] args) {
		ParallelCorpusParser pcp = new ParallelCorpusParser("list.txt");
		pcp.generateParallelCorpus("F:\\Study\\Research\\Tutorial Project\\Projects", true, true);
	}
}
