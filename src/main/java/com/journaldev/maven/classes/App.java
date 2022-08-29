package com.journaldev.maven.classes;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class App {

	private final static int CMA_TYPE = 1;
	private final static int EVERGREEN_TYPE = 2;
	private final static int MAERSK_TYPE = 3;
	private final static int MSC_TYPE = 4;
	private final static int TURKON_TYPE = 5;
	private final static String[] TYPE = {"CMA", "EVERGREEN", "MAERSK", "MSC", "TURKON"};
	private final static String ULTIMATE_FILE_PATH = "C:\\SC";
	private final static String LOCAL_FILE_PATH = "C:\\SC\\";
	private List<String> filesList;
	
	private final static String CMA_BL_REGEX = "\\b([A-Z]{3}\\d{7}[A-Z]{0,1})\\s*";
	private final static String EVERGREEN_BL_REGEX = "([^a-zA-Z0-9|\\s*][A-Z]{3}[0-9]{7})[\\s*|^a-zA-Z0-9]";
	private final static String MAERSK_BL_REGEX = "";
	private final static String MSC_BL_REGEX = "";
	private final static String TURKON_BL_REGEX = "\\s*(\\d{8}\\d{0,2})(?=BILL OF LADING)";

	public static Pattern PATTERN_CMA;
	public static Pattern PATTERN_EVERGREEN;
	public static Pattern PATTERN_MAERSK;
	public static Pattern PATTERN_MSC;
	public static Pattern PATTERN_TURKON;
	
	public static Matcher matcher;
	
	public static File currentFile;
	public static int currentStartPage;
	public static int currentEndPage;
	
	public static String printLog = "";
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new App();
				} catch (Exception e) {e.printStackTrace();}
			}
		});
	}
	
	public App() throws Exception {
		// RETREIVE ALL PDFs IN FOLDER PATH
		filesList = retrieveAllFiles();
		int fileTypes[] = new int[filesList.size()];
		int pageCount;
		// INITIALIZE ALL BL# PATTERNS
		PATTERN_CMA = Pattern.compile(CMA_BL_REGEX);
		PATTERN_EVERGREEN = Pattern.compile(EVERGREEN_BL_REGEX);
		PATTERN_MAERSK = Pattern.compile(MAERSK_BL_REGEX);
		PATTERN_MSC = Pattern.compile(MSC_BL_REGEX);
		PATTERN_TURKON = Pattern.compile(TURKON_BL_REGEX);

		for(int a=0 ; a<filesList.size(); a++) {
			currentFile = new File(filesList.get(a));
			out("read in file ["+a+"] : "+filesList.get(a));

			// DETERMINE EACH FILE's SHIPPING CARRIER ORIGIN
			fileTypes[a] = determineFileType(currentFile);
			out("Based on our program, this file is type '"+TYPE[fileTypes[a]-1]+"'\n");
			PDDocument doc = PDDocument.load(currentFile);
			pageCount = doc.getNumberOfPages();
			
			switch (fileTypes[a]) {
				case CMA_TYPE:
					out("processing CMA type");
					processCMA(pageCount);
					break;
//				case EVERGREEN_TYPE:
//					out("processing EVERGREEN type");
////					processEVERGREEN(pageCount);
//					break;
//				case MAERSK_TYPE:
//					out("processing MAERSK type");
////					processMAERSK(pageCount);
//					break;
//				case MSC_TYPE:
//					out("processing MSC type");
////					processMSC(pageCount);
//					break;
//				case TURKON_TYPE:
//					out("processing TURKON type");
//					processTURKON(pageCount);
//					break;
				default:
					break;
			}
		}
		
		/**
		 * Step 1: Read in one pdf file
		 * 		2: Determine the type of file
		 * 		3: Split the file by order
		 * 		4: Determine the B/L #s
		 * 		5: Rename the files by B/L #
		 */
	}
	
	public static void processCMA(int pageCount) {
		try {
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
			String text = pdfStripper.getText(doc);
			BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\SC\\text.txt")); // TO-DO: Update to proper folder path
			
			//Extract page to textfile
			doc.close();
			bw.write(text);
			bw.close();
			
			File textfile = new File("C:\\SC\\text.txt"); // TO-DO: Update to proper folder path
			BufferedReader br = new BufferedReader(new FileReader(textfile));
			String currentLine = br.readLine();
			out("before CMA while");
			while(currentLine != null) {
				matcher = PATTERN_CMA.matcher(currentLine);
				if(matcher.find()) {
					out("found CMA match: " + matcher.group(1));
				}
				currentLine = br.readLine();
			}

			if (pageCount == 1) {
				
			}
			br.close();
		} catch (Exception e) {
			out("We got an exception from processCMA " + e);
			e.printStackTrace();
		}
	}

	public static void processEVERGREEN(int pageCount) {
		try {
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
				
			String text = pdfStripper.getText(doc);
			BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\SC\\text.txt")); // TO-DO: Update to proper folder path
			
			//Extract page to textfile
			doc.close();
			bw.write(text);
			bw.close();
			
			File textfile = new File("C:\\SC\\text.txt"); // TO-DO: Update to proper folder path
			BufferedReader br = new BufferedReader(new FileReader(textfile));
			String currentLine = br.readLine();
			out("before EVERGREEN while");
			while(currentLine != null) {
				matcher = PATTERN_EVERGREEN.matcher(currentLine);
				if(matcher.find()) {
					out("found EVERGREEN match: " + matcher.group(1));
				}
				currentLine = br.readLine();
			}
			br.close();
		} catch (Exception e) {
			out("We got an exception from processEVERGREEN " + e);
			e.printStackTrace();
		}
	}
	
	public static void processMAERSK(int pageCount) {
		try {
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
				
			String text = pdfStripper.getText(doc);
			BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\SC\\text.txt")); // TO-DO: Update to proper folder path
			
			//Extract page to textfile
			doc.close();
			bw.write(text);
			bw.close();
			
			File textfile = new File("C:\\SC\\text.txt"); // TO-DO: Update to proper folder path
			BufferedReader br = new BufferedReader(new FileReader(textfile));
			String currentLine = br.readLine();
			out("before MAERSK while");
			while(currentLine != null) {
				matcher = PATTERN_MAERSK.matcher(currentLine);
				if(matcher.find()) {
					out("found MAERSK match: " + matcher.group(1));
				}
				currentLine = br.readLine();
			}
			br.close();
		} catch (Exception e) {
			out("We got an exception from processMAERSK " + e);
			e.printStackTrace();
		}
	}
	
	public static void processMSC(int pageCount) {
		try {
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
				
			String text = pdfStripper.getText(doc);
			BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\SC\\text.txt")); // TO-DO: Update to proper folder path
			
			// Extract page to textfile
			doc.close();
			bw.write(text);
			bw.close();
			
			File textfile = new File("C:\\SC\\text.txt"); // TO-DO: Update to proper folder path
			BufferedReader br = new BufferedReader(new FileReader(textfile));
			String currentLine = br.readLine();
			out("before MSC while");
			while(currentLine != null) {
				matcher = PATTERN_MSC.matcher(currentLine);
				if(matcher.find()) {
					out("found MSC match: " + matcher.group(1));
				}
				currentLine = br.readLine();
			}
			br.close();
		} catch (Exception e) {
			out("We got an exception from processMSC " + e);
			e.printStackTrace();
		}
	}
	
	public static void processTURKON(int pageCount) {
		try {
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
				
			int blCounter = 0;
			currentStartPage = 1;
			String currentBL = "";

			for(int page = 1; page <= pageCount; page ++) {
				boolean foundBL = false;
				pdfStripper.setStartPage(page);
				pdfStripper.setEndPage(page);
				String text = pdfStripper.getText(doc);

				//Extract page to textfile
				BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\SC\\text.txt")); // TO-DO: Update to proper folder path
				bw.write(text);
				bw.close();
				
				File textfile = new File("C:\\SC\\text.txt"); // TO-DO: Update to proper folder path
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String currentLine = br.readLine();

				while(currentLine != null) {
					matcher = PATTERN_TURKON.matcher(currentLine);
					if(matcher.find()) {
						foundBL = true;
						if(currentBL != "") {
							if(currentStartPage < page-1) splitDocAndRename(doc, currentStartPage, page-1, currentBL);
							else splitDocAndRename(doc, currentStartPage, currentStartPage, currentBL);
							currentStartPage = page; // NEW START PAGE FOR THE NEXT SPLIT
						}
						currentBL = matcher.group(1);
						blCounter++;
						break;
					}
					currentLine = br.readLine();
				}
				if(!foundBL && page == pageCount) splitDocAndRename(doc, currentStartPage, page, currentBL);
				br.close();
			}
			out("found " + blCounter + " in this document");
			doc.close();
		} catch (Exception e) {
			out("We got an exception from processTURKON " + e);
			e.printStackTrace();
		}
	}
	
	public static void splitDocAndRename(PDDocument doc, int start, int end, String newName) throws IOException {
		Splitter splitter = new Splitter();
		splitter.setStartPage(start);
		splitter.setEndPage(end);
		List<PDDocument> newDoc = splitter.split(doc);
		PDFMergerUtility mergerPdf = new PDFMergerUtility();
		String name = "";
		
		// MERGE ALL FILES OF THE SAME B/L#
		for (int page = 0; page < newDoc.size(); page++) {
			name = LOCAL_FILE_PATH + newName + "_" + page + ".pdf";
			newDoc.get(page).save(name);
			mergerPdf.addSource(name);
		}
		mergerPdf.setDestinationFileName(LOCAL_FILE_PATH + newName + ".pdf");
		mergerPdf.mergeDocuments();
		
		// DELETE SPLIT PAGES AFTER MERGING
		for (int page = 0; page < newDoc.size(); page++) {
			name = LOCAL_FILE_PATH + newName + "_" + page + ".pdf";
			File file = new File(name);
			file.delete();
		}
	}
	
	public static int determineFileType(File file) {
		out("inside determineFileType method");
		try {
			PDDocument doc = PDDocument.load(file);
			PDFTextStripper pdfStripper = new PDFTextStripper();
			String text = pdfStripper.getText(doc);
			BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\SC\\text.txt"));
			
			//Extract pdf to textfile
			doc.close();
			bw.write(text);
			bw.close();
			
			File textfile = new File("C:\\SC\\text.txt");
			BufferedReader br = new BufferedReader(new FileReader(textfile));
			String currentLine = br.readLine();
			
			for (int line = 0; currentLine != null; line ++) {
//				out(line+": "+currentLine);
				if(currentLine.contains("Invoicing and Disputes"))	return MSC_TYPE;
				if(currentLine.contains("Turkon"))	return TURKON_TYPE;
				if(currentLine.contains("Evergreen")) return EVERGREEN_TYPE;
				currentLine = br.readLine();
			}
			br.close();
		} catch (Exception ex) {
			out("we got an exception in determineFileType function, it is "+ ex);
			ex.printStackTrace();
		}
		return CMA_TYPE;
	}
	
	private static List <String> retrieveAllFiles() throws Exception {
		try (Stream<Path> walk = Files.walk(Paths.get(ULTIMATE_FILE_PATH))) {
			return walk.filter(p -> !Files.isDirectory(p)).map(p -> p.toString()).filter(f -> f.toLowerCase().endsWith(".pdf")).collect(Collectors.toList());
		}
	}
	
	public static void out(String stringToPrint) {
		System.out.println(stringToPrint);
		printLog += stringToPrint + "\n";
	}

}
