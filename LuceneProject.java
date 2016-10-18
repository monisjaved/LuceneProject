import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
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

	/** Loads lucene index from directory
	 * @param indexDirectoryPath: path of index dir
	 * @return void	 */
	public static void getIndexFromDir (String indexDirectoryPath){
		try{
			FileSystem fs = FileSystems.getDefault();
			Directory indexDirectory = FSDirectory.open(fs.getPath(indexDirectoryPath, new String[0]));
			reader = DirectoryReader.open(indexDirectory);
			//create inverted index from loaded index
			createInvertedIndex();
		}
		catch(Exception exception){
			exception.printStackTrace();
		}
	}

	/** Create invertedIndex from the loaded index
	 * @param None
	 * @return void	 */
	public static void createInvertedIndex (){
		try {
			//create blank hashmap
			invertedIndex =  new HashMap<String, LinkedList<Integer>>();
			Fields fields = MultiFields.getFields(reader);
			//iterate on all fields
			for (String field : fields) {
				if (!field.equals("_version_") && !field.equals("id")){
					Terms terms = fields.terms(field);
					TermsEnum termsEnum = terms.iterator();
					Integer count = 0;
					BytesRef text;
					//iterate over all terms present in the field
					while ((text = termsEnum.next()) != null) {
						count++;
						String stringedText = text.utf8ToString();
						PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader,
								field, text, PostingsEnum.FREQS);
						LinkedList<Integer> termLinkedList = new LinkedList<Integer>();
						Integer i;
						//get all documents containing the term in the field
						while ((i = postingsEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
							//add the docs to the terms linked list
							termLinkedList.add(i);
						}
						//add linkedlist and term to hashmap
						invertedIndex.put(stringedText, termLinkedList);
					}
				}
			}
		} 
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	/** Creates printwriter for the output file
	 * @param outputFilePath: path of the output file
	 * @return void	 */
	public static void getOutputs (String outputFilePath) {
		try {
			outputFile = new PrintWriter(new File(outputFilePath));
		} 
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	/** Reads from input file and calls the various functions
	 * @param inputFilePath: path of the input file
	 * @return void	 */
	public static void getInputs (String inputFilePath) {
		try{
			inputFile = new Scanner(new FileReader(inputFilePath));
			while (inputFile.hasNextLine()) {
				String line = inputFile.nextLine();
				//get postings
				GetPostings(line);
				//term at a time and function
				TaatAnd(line);
				//term at a time or function
				TaatOr(line);
				//doc at a time and function
				DaatAnd(line);
				//doc at a time or function
				DaatOr(line);
			}
			inputFile.close();
			outputFile.close();
		}
		catch(Exception exception){
			exception.printStackTrace();
		}
	}

	/** Get postings list for each term
	 * @param termString: string containing all terms separated by space
	 * @return void	 */
	public static void GetPostings (String termString){
		try {
			//get all terms
			String[] terms = termString.split(" ");
			for (String term : terms){
				outputFile.println("GetPostings");
				outputFile.println(term);
				outputFile.print("Postings list: ");
				//get linkedlist for the term
				LinkedList<Integer> termLinkedList = invertedIndex.get(term);
				//check if term present in hashmap
				if (termLinkedList != null){
					for (Integer i = 0; i < termLinkedList.size(); i++) {
						outputFile.print(termLinkedList.get(i));
						if (i != (termLinkedList.size() -1)){
							outputFile.print(" ");
						}
					}
				}	
				outputFile.println();
			}

		} 
		catch (Exception exception) {
			exception.printStackTrace();
			System.out.println(exception.getMessage());
		}
	}

	/** Sorts the array of postings based on the size
	 * @param terms: array of postings
	 * @return terms: sorted array of postings	 */
	public static String[] GetSortedTermsList(String[] terms) {
		for (Integer i=0;i<terms.length;i++){
			for (Integer j=0;j<terms.length;j++){
				Integer counti = invertedIndex.get(terms[i]) != null ? invertedIndex.get(terms[i]).size():0;
				Integer countj = invertedIndex.get(terms[j]) != null ? invertedIndex.get(terms[j]).size():0;
				String temp;
				//swap if bigger
				if (counti < countj);
				temp = terms[i];
				terms[i] = terms[j];
				terms[j] = temp;
			}
		}
		return terms;
	}

	/** Term at a Time And Implementation
	 * @param termString: string containing all terms separated by space
	 * @return void	 */
	public static void TaatAnd (String termString) {
		try {
			String[] terms = termString.split(" ");
			Integer numComparisons = 0;
			outputFile.println("TaatAnd");
			outputFile.println(termString);
			outputFile.print("Results: ");

			//sort terms by size
			terms = GetSortedTermsList(terms);
			LinkedList<Integer>[] termsPostingsArray = new LinkedList[terms.length];
			for(Integer i=0; i<terms.length;i++){
				termsPostingsArray[i] = invertedIndex.get(terms[i]) != null ? invertedIndex.get(terms[i]): new LinkedList<Integer>();
			}
			Integer i = terms.length-1;
			while (i > 0){
				Integer pointer1 = 0;
				Integer pointer2 = 0;
				//create intermediate linkedlist which will be stored after computation
				LinkedList<Integer> tempLinkedList = new LinkedList<Integer>();
				while (pointer1 < termsPostingsArray[i].size() && pointer2 < termsPostingsArray[i-1].size()){
					//if both are same
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
				//if any intermediate linkedlist is empty then break
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

		} 
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	/** Term at a Time Or Implementation
	 * @param termString: string containing all terms separated by space
	 * @return void	 */
	public static void TaatOr (String termString) {
		try {
			String[] terms = termString.split(" ");
			Integer numComparisons = 0;
			outputFile.println("TaatOr");
			outputFile.println(termString);
			outputFile.print("Results: ");

			//get sorted term postings list
			terms = GetSortedTermsList(terms);
			LinkedList<Integer>[] termsPostingsArray = new LinkedList[terms.length];
			for(Integer i=0; i<terms.length;i++){
				termsPostingsArray[i] = invertedIndex.get(terms[i]) != null ? invertedIndex.get(terms[i]): new LinkedList<Integer>();
			}
			Integer i = terms.length-1;
			while (i > 0){
				Integer pointer1 = 0;
				Integer pointer2 = 0;
				
				//create intermediate linkedlist which will be stored after computation
				LinkedList<Integer> tempLinkedList = new LinkedList<Integer>();
				while (pointer1 < termsPostingsArray[i].size() && pointer2 < termsPostingsArray[i-1].size()){
					//if both are same
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
				//store the remaining docs as it is
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

		} 
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	/** Document at a Time And Implementation
	 * @param termString: string containing all terms separated by space
	 * @return void	 */
	public static void DaatAnd(String termString){
		try{
			String[] terms = termString.split(" ");
			terms = GetSortedTermsList(terms);
			Integer termsArrayLen = terms.length;
			Integer numComparisons = 0;
			Integer numDocuments = 0;
			//array to store all the result docs
			ArrayList<Integer> results = new ArrayList<Integer>();
			//list of pointers
			Integer[] pointerArray = new Integer[terms.length];
			Boolean running = true;
			Boolean isSame = false;
			//list of postings
			LinkedList<Integer>[] termsPostingsArray = new LinkedList[terms.length];
			Integer firstNonNullIndex = 0;
			Integer pointersReachedNullCount = 0;
			
			for(Integer i=0; i<terms.length;i++){
				termsPostingsArray[i] = invertedIndex.get(terms[i]) != null ? invertedIndex.get(terms[i]): new LinkedList<Integer>();
				pointerArray[i] = 0;
			}
			
			while(running){
				firstNonNullIndex = 0;
				isSame = true;
				
				//get the first non null index to store minimum
				while((firstNonNullIndex != termsArrayLen) && pointerArray[firstNonNullIndex] >= termsPostingsArray[firstNonNullIndex].size()){
					firstNonNullIndex++;
				}
				//if all lists are processed then break loop
				if(firstNonNullIndex.equals(termsArrayLen)){
					running = false;
					break;
				}
				Integer min = termsPostingsArray[firstNonNullIndex].get(pointerArray[firstNonNullIndex]);
				Integer minIndex = firstNonNullIndex;
				
				for(Integer i=0; i<termsArrayLen; i++){
					//if list is not processed
					if(pointerArray[i] < termsPostingsArray[i].size()){
						firstNonNullIndex = i;
						numComparisons++;
						//if it is not same then get check for new minimum
						if(!(termsPostingsArray[i].get(pointerArray[i])).equals(min)){
							isSame = false;
							if(termsPostingsArray[i].get(pointerArray[i]) < min){
								min = termsPostingsArray[i].get(pointerArray[i]);
								minIndex = i;
							}
							break;
						}
					}
				}
				//add only if all docs are same and increment pointer
				if(isSame){
					if(!results.contains(min)){
						results.add(min);
					}
					for(Integer i=0; i<termsArrayLen; i++){
						pointerArray[i]++;
						if(pointerArray[i] == termsPostingsArray[i].size()){
							running = false;
							break;
						}
					}
				}
				//else only increment pointer of minimum doc
				else{
					pointerArray[minIndex]++;
					if(pointerArray[minIndex] == termsPostingsArray[minIndex].size()){
						running = false;
						break;
					}
				}		
			}
			outputFile.println("DaatAnd");
			outputFile.println(termString);
			String resultString = "";
			if (results.size() == 0){
				resultString = "Empty";
			}
			else{
				for(Integer i=0;i<results.size();i++){
					resultString += results.get(i).toString();
					if (i < results.size() - 1){
						resultString += " ";
					}
				}
			}
			numDocuments = results.size();
			outputFile.println("Results: "+ resultString);
			outputFile.println("Number of documents in results: " + numDocuments);
			outputFile.println("Number of comparisons: " + numComparisons);
		}
		catch(Exception exception){
			exception.printStackTrace();
		}
	}
	
	/** Document at a Time Or Implementation
	 * @param termString: string containing all terms separated by space
	 * @return void	 */
	public static void DaatOr(String termString) {
		try{
			String[] terms = termString.split(" ");
			terms = GetSortedTermsList(terms);
			Integer termsArrayLen = terms.length;
			Integer numComparisons = 0;
			Integer numDocuments = 0;
			//create array to store results
			ArrayList<Integer> results = new ArrayList<Integer>();
			//create list of pointers
			Integer[] pointerArray = new Integer[terms.length];
			Boolean running = true;
			Boolean isSame = false;
			//create list of postings
			LinkedList<Integer>[] termsPostingsArray = new LinkedList[terms.length];
			Integer firstNonNullIndex = 0;
			Integer pointersReachedNullCount = 0;
			
			for(Integer i=0; i<terms.length;i++){
				termsPostingsArray[i] = invertedIndex.get(terms[i]) != null ? invertedIndex.get(terms[i]): new LinkedList<Integer>();
				pointerArray[i] = 0;
			}
			
			while(running){
				firstNonNullIndex = 0;
				isSame = true;
				
				//get first non null index to store minimum
				while((firstNonNullIndex != termsArrayLen) && pointerArray[firstNonNullIndex] >= termsPostingsArray[firstNonNullIndex].size()){
					firstNonNullIndex++;
				}
				//if all lists are processed then break loop
				if(firstNonNullIndex.equals(termsArrayLen)){
					running = false;
					break;
				}
				Integer min = termsPostingsArray[firstNonNullIndex].get(pointerArray[firstNonNullIndex]);
				Integer minIndex = firstNonNullIndex;
				
				for(Integer i=0; i<termsArrayLen; i++){
					//if list is not processed
					if(pointerArray[i] < termsPostingsArray[i].size()){
						firstNonNullIndex = i;
						numComparisons++;
						//if not equal to minimum then check for new minimum
						if(!(termsPostingsArray[i].get(pointerArray[i])).equals(min)){
							isSame = false;
							if(termsPostingsArray[i].get(pointerArray[i]) < min){
								min = termsPostingsArray[i].get(pointerArray[i]);
								minIndex = i;
							}
						}
					}
				}
				//if all docs are same then add min and increment all pointers
				if(isSame){
					if(!results.contains(min)){
						results.add(min);
					}
					for(Integer i=0; i<termsArrayLen; i++){
						pointerArray[i]++;
					}
				}
				//else add minimum doc and increment pointer of it
				else{
					if(!results.contains(min)){
						results.add(min);
					}
					pointerArray[minIndex]++;
				}		
			}
			outputFile.println("DaatOr");
			outputFile.println(termString);
			String resultString = "";
			if (results.size() == 0){
				resultString = "Empty";
			}
			else{
				for(Integer i=0;i<results.size();i++){
					resultString += results.get(i).toString();
					if (i < results.size() - 1){
						resultString += " ";
					}
				}
			}
			numDocuments = results.size();
			outputFile.println("Results: "+ resultString);
			outputFile.println("Number of documents in results: " + numDocuments);
			outputFile.println("Number of comparisons: " + numComparisons);
		}
		catch(Exception exception){
			exception.printStackTrace();
		}
	}

	public static void main (String[] args) throws IOException, ParseException{
		//load index and create inverted index from it
		getIndexFromDir(args[0]);
		//open output file for writing
		getOutputs(args[1]);
		//load inputs and write respective outputs to file
		getInputs(args[2]);
	}
}
