import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


public class LuceneProject {
	
	private static IndexReader reader;
	private static HashMap<String, LinkedList<Integer>> invertedIndex;
	private static PrintWriter outputFile;
	private static Scanner inputFile;
	
	public static void getIndexFromDir (String indexDirectoryPath){
		try{
			FileSystem fs = FileSystems.getDefault();
			Directory indexDirectory = FSDirectory.open(fs.getPath(indexDirectoryPath, new String[0]));
			reader = DirectoryReader.open(indexDirectory);
//			System.out.println(reader.document(32213).getField("text_de").stringValue());
//			System.exit(0);
			createInvertedIndex();
		}
		catch(Exception exception){
			exception.printStackTrace();
		}
	}
	
	public static void createInvertedIndex (){
		try {
			invertedIndex =  new HashMap<String, LinkedList<Integer>>();
			System.out.println("Number of Documents in Reader:\t" + reader.numDocs());
			Fields fields = MultiFields.getFields(reader);
	        for (String field : fields) {
	        	if (!field.equals("_version_") && !field.equals("id")){
	        		System.out.print(field + "\t");
		            Terms terms = fields.terms(field);
		            TermsEnum termsEnum = terms.iterator();
		            int count = 0;
		            BytesRef text;
		            while ((text = termsEnum.next()) != null) {
		                count++;
		                String stringedText = text.utf8ToString();
//		                if (field.equals("text_de")){
//			                System.out.println("'"+stringedText+"'");
//		                }
		                PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader,
		                        field, text, PostingsEnum.FREQS);
		                LinkedList<Integer> termLinkedList = new LinkedList<Integer>();
		                int i;
		                while ((i = postingsEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
//		                    Document doc = reader.document(i); // The document
//		                    termLinkedList.add(Integer.parseInt(doc.getField("id").stringValue()));
		                    termLinkedList.add(i);
		                }
		                invertedIndex.put(stringedText, termLinkedList);
//		                System.out.println(invertedIndex.get("adspurg"));
//		                System.exit(0);
		            }
		            System.out.print(count+"\n");
	        	}
	        }
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	public static void getOutputs (String outputFilePath) {
		try {
			outputFile = new PrintWriter(new File(outputFilePath));
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	public static void getInputs (String inputFilePath) {
		try{
			inputFile = new Scanner(new FileReader(inputFilePath));
			while (inputFile.hasNextLine()) {
                String line = inputFile.nextLine();
                GetPostings(line);
                TaatAnd(line);
                TaatOr(line);
                DaatAnd(line);
                DaatOr(line);
            }
            inputFile.close();
            outputFile.close();
		}
		catch(Exception exception){
			exception.printStackTrace();
		}
	}
	
	public static void GetPostings (String termString){
		try {
			String[] terms = termString.split(" ");
			for (String term : terms){
				outputFile.println("GetPostings");
				outputFile.println(term);
				outputFile.print("Postings list: ");
				LinkedList<Integer> termLinkedList = invertedIndex.get(term);
				if (termLinkedList != null){
					for (int i = 0; i < termLinkedList.size(); i++) {
						outputFile.print(termLinkedList.get(i));
						if (i != (termLinkedList.size() -1)){
							outputFile.print(" ");
						}
					}
				}	
				outputFile.println();
			}
			
		} catch (Exception exception) {
			exception.printStackTrace();
			System.out.println(exception.getMessage());
		}
	}
	
	public static String[] GetSortedTermsList(String[] terms) {
		for (int i=0;i<terms.length;i++){
			for (int j=0;j<terms.length;j++){
				Integer counti = invertedIndex.get(terms[i]) != null ? invertedIndex.get(terms[i]).size():0;
				Integer countj = invertedIndex.get(terms[j]) != null ? invertedIndex.get(terms[j]).size():0;
				String temp;
				if (counti < countj);
					temp = terms[i];
					terms[i] = terms[j];
					terms[j] = temp;
			}
		}
		return terms;
	}
	
	public static void TaatAnd (String termString) {
		try {
			String[] terms = termString.split(" ");
			Integer numComparisons = 0;
			outputFile.println("TaatAnd");
			outputFile.println(termString);
			outputFile.print("Results: ");

//			System.out.println();
			terms = GetSortedTermsList(terms);
			LinkedList<Integer>[] termsPostingsArray = new LinkedList[terms.length];
			for(int i=0; i<terms.length;i++){
				termsPostingsArray[i] = invertedIndex.get(terms[i]) != null ? invertedIndex.get(terms[i]): new LinkedList<Integer>();
			}
			Integer i = terms.length-1;
			while (i > 0){
				Integer pointer1 = 0;
				Integer pointer2 = 0;
				LinkedList<Integer> tempLinkedList = new LinkedList<Integer>();
				while (pointer1 < termsPostingsArray[i].size() && pointer2 < termsPostingsArray[i-1].size()){
//					System.out.println(terms[i]+": "+termsPostingsArray[i].get(pointer1)+" "+terms[i-1]+": "+termsPostingsArray[i-1].get(pointer2)+" "+(termsPostingsArray[i-1].get(pointer2).equals(termsPostingsArray[i].get(pointer1))));
					if(termsPostingsArray[i].get(pointer1).equals(termsPostingsArray[i-1].get(pointer2))){
						tempLinkedList.add(termsPostingsArray[i].get(pointer1));
						pointer1++;
						pointer2++;
					}
					else if(termsPostingsArray[i].get(pointer1) < termsPostingsArray[i-1].get(pointer2)){
						pointer1++;
					}
					else{
						pointer2++;
					}
					numComparisons++;
				}
				if (tempLinkedList.size() == 0){
					outputFile.print("empty");
					outputFile.println("\nNumber of documents in results: 0");
					outputFile.println("Number of comparisons: " + numComparisons);
					return;
				}
				termsPostingsArray[i-1] = tempLinkedList;
				i --;
			}
			for(i=0;i<termsPostingsArray[0].size();i++){
				outputFile.print(termsPostingsArray[0].get(i));
				if(i != termsPostingsArray[0].size()-1){
					outputFile.print(" ");
				}
			}
			outputFile.println("\nNumber of documents in results: " + termsPostingsArray[0].size());
			outputFile.println("Number of comparisons: " + numComparisons);
			
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	public static void TaatOr (String termString) {
		try {
			String[] terms = termString.split(" ");
			Integer numComparisons = 0;
			outputFile.println("TaatOr");
			outputFile.println(termString);
			outputFile.print("Results: ");

//			System.out.println();
			terms = GetSortedTermsList(terms);
			LinkedList<Integer>[] termsPostingsArray = new LinkedList[terms.length];
			for(int i=0; i<terms.length;i++){
				termsPostingsArray[i] = invertedIndex.get(terms[i]) != null ? invertedIndex.get(terms[i]): new LinkedList<Integer>();
			}
			Integer i = terms.length-1;
			while (i > 0){
				Integer pointer1 = 0;
				Integer pointer2 = 0;
				LinkedList<Integer> tempLinkedList = new LinkedList<Integer>();
				while (pointer1 < termsPostingsArray[i].size() && pointer2 < termsPostingsArray[i-1].size()){
//					System.out.println(terms[i]+": "+termsPostingsArray[i].get(pointer1)+" "+terms[i-1]+": "+termsPostingsArray[i-1].get(pointer2)+" "+(termsPostingsArray[i-1].get(pointer2).equals(termsPostingsArray[i].get(pointer1))));
					if(termsPostingsArray[i].get(pointer1).equals(termsPostingsArray[i-1].get(pointer2))){
						tempLinkedList.add(termsPostingsArray[i].get(pointer1));
						pointer1++;
						pointer2++;
					}
					else if(termsPostingsArray[i].get(pointer1) < termsPostingsArray[i-1].get(pointer2)){
						tempLinkedList.add(termsPostingsArray[i].get(pointer1));
						pointer1++;
					}
					else{
						tempLinkedList.add(termsPostingsArray[i-1].get(pointer2));
						pointer2++;
					}
					numComparisons++;
				}
				while(pointer1 < termsPostingsArray[i].size()){
					tempLinkedList.add(termsPostingsArray[i].get(pointer1));
					pointer1++;
				}
				while(pointer2 < termsPostingsArray[i-1].size()){
					tempLinkedList.add(termsPostingsArray[i-1].get(pointer2));
					pointer2++;
				}
				termsPostingsArray[i-1] = tempLinkedList;
				i --;
			}
			for(i=0;i<termsPostingsArray[0].size();i++){
				outputFile.print(termsPostingsArray[0].get(i));
				if(i != termsPostingsArray[0].size()-1){
					outputFile.print(" ");
				}
			}
			if(termsPostingsArray[0].size() == 0){
				outputFile.print("empty");
			}
			outputFile.println("\nNumber of documents in results: " + termsPostingsArray[0].size());
			outputFile.println("Number of comparisons: " + numComparisons);

		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	public static void DaatAnd (String termString) {
		try {
			System.out.println("DaatAnd " + termString);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	public static void DaatOr (String termString) {
		try {
			System.out.println("DaatOr " + termString);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	public static void main (String[] args) throws IOException, ParseException{
		final long startTime = System.currentTimeMillis();
		getIndexFromDir(args[0]);
		final long endTime = System.currentTimeMillis();
		System.out.println("Total execution time: " + (endTime - startTime)/1000 );
		getOutputs(args[1]);
		getInputs(args[2]);
		
//		for (int i=0; i<reader.maxDoc(); i++) {
//			Document doc = reader.document(i);
//		    List<IndexableField> fields1 = doc.getFields();
//		    Integer docId = Integer.parseInt(fields1.get(1).stringValue());
//		    String fieldValue = fields1.get(0).stringValue();
//		    Set<String> keySet = invertedIndex.keySet();
//		    for (String term : keySet){
//		    	if (fieldValue.contains(term)){
//		    		LinkedList<Integer> termLinkedList = invertedIndex.get(term);
//		    		System.out.println(termLinkedList);
//		    		termLinkedList.add(docId);
//		    	}
//		    }
//		}
	}
}
