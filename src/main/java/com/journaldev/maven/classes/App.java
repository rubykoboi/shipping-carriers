package com.journaldev.maven.classes;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private List<String> filesList;
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new App();
				} catch (Exception e ) { e.printStackTrace(); }
			}
		});
	}
	
	public App() throws Exception {
		// RETREIVE ALL PDFs IN FOLDER PATH
		filesList = retrieveAllFiles();
		int fileTypes[] = new int[filesList.size()];

		for(int a=0 ; a<filesList.size(); a++) {
			File file = new File(filesList.get(a));
			out("read in file ["+a+"] : "+filesList.get(a));

			// DETERMINE EACH FILE's SHIPPING CARRIER ORIGIN
			fileTypes[a] = determineFileType(file);
			out("Based on our program, this file is type '"+TYPE[fileTypes[a]-1]+"'\n");
			// TO-DO: PERFORM BL# SEARCH ON EACH FILE BY TYPE
			switch (fileTypes[a]) {
				case CMA_TYPE:
					processCMA();
				case EVERGREEN_TYPE:
					processEVERGREEN();
				case MAERSK_TYPE:
					processMAERSK();
				case MSC_TYPE:
					processMSC();
				case TURKON_TYPE:
					processTURKON();
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
	
	public static void processCMA() {
		// TO-DO: process CMA files
	}

	public static void processEVERGREEN() {
		// TO-DO: process EVERGREEN files
	}
	
	public static void processMAERSK() {
		// TO-DO: process MAERSK files
	}
	
	public static void processMSC() {
		// TO-DO: process MSC files
	}
	
	public static void processTURKON() {
		// TO-DO: process TURKON files
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
	}

}
