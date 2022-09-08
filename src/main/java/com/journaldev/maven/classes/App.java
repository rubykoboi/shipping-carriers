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

	private final static int CMA_TYPE = 0;
	private final static int COSCO_TYPE = 1;
	private final static int EVERGREEN_TYPE = 2;
	private final static int MAERSK_TYPE = 3;
	private final static int MSC_TYPE = 4;
	private final static int TURKON_TYPE = 5;
	private final static String[] TYPE = {"CMA", "COSCO", "EVERGREEN", "MAERSK", "MSC", "TURKON"};
	private final static String ULTIMATE_FILE_PATH = "C:\\SC\\";
	private final static String LOCAL_FILE_PATH = "C:\\SC\\";
//	private final static String ORDERS_FILE_PATH = "I:\\2022\\";
	private final static String ORDERS_FILE_PATH = "C:\\Orders\\";
	private List<String> filesList;
	private static List<String> ordersList;
	
	private final static String CMA_BL_REGEX = "\\b([A-Z]{3}\\d{7}[A-Z]{0,1})\\s*";
	private final static String COSCO_BL_REGEX = "(?<=BL)(\\d{10})\\s*";
	private final static String EVERGREEN_BL_REGEX = "(?<=EGLV)(\\d{12})\\s*";
	private final static String MAERSK_BL_REGEX = "";
	private final static String MSC_BL_REGEX = "\\s*(?<=BL# )(MEDU[A-Z]{2}\\d{6})";
	private final static String TURKON_BL_REGEX = "\\s*(\\d{8}\\d{0,2})(?=BILL OF LADING)";
	private final static String SHIP_ID_REGEX = "\\s*(?<![A-Z])([CIMTUV][ADNRWY]\\d{5})\\s*";
	private final static String TEXTFILE_PATH = "C:\\SC\\text.txt";

	public static Pattern PATTERN_CMA;
	public static Pattern PATTERN_COSCO;
	public static Pattern PATTERN_EVERGREEN;
	public static Pattern PATTERN_MAERSK;
	public static Pattern PATTERN_MSC;
	public static Pattern PATTERN_TURKON;
	public static Pattern PATTERN_SHIP_ID;
	
	public static Matcher matcher;
	public static Matcher shipMatcher;
	public static File currentFile;
	public static File currentOrder;
	public static int currentStartPage;
	public static int currentEndPage;
	
	public static String printLog = "";
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try { 
					new App(); 
					BufferedWriter bw = new BufferedWriter(new FileWriter(ULTIMATE_FILE_PATH+"Log.txt"));
					bw.write(printLog);
					bw.close();
				}
				catch (Exception e) {e.printStackTrace();}
			}
		});
	}
	
	public App() throws Exception {
		// RETREIVE ALL PDFs IN FOLDER PATH
		filesList = retrieveAllFiles();
		ordersList = retrieveOrderFiles();
		out("THERE ARE " + filesList.size() + " FILES IN " + LOCAL_FILE_PATH);
		out("THERE ARE " + ordersList.size() + " ORDER FILES IN " + ORDERS_FILE_PATH);
		int fileTypes[] = new int[filesList.size()];
		int pageCount;
		// INITIALIZE ALL BL# PATTERNS
		PATTERN_CMA = Pattern.compile(CMA_BL_REGEX);
		PATTERN_COSCO = Pattern.compile(COSCO_BL_REGEX);
		PATTERN_EVERGREEN = Pattern.compile(EVERGREEN_BL_REGEX);
		PATTERN_MAERSK = Pattern.compile(MAERSK_BL_REGEX);
		PATTERN_MSC = Pattern.compile(MSC_BL_REGEX);
		PATTERN_TURKON = Pattern.compile(TURKON_BL_REGEX);
		PATTERN_SHIP_ID = Pattern.compile(SHIP_ID_REGEX);

		// PROCESS ALL FILES: SPLIT AND RENAME WITH ACCORDING B/L #S
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
				case COSCO_TYPE:
					out("processing COSCO type");
					processCOSCO();
					break;
				case EVERGREEN_TYPE:
					out("processing EVERGREEN type");
					processEVERGREEN(pageCount);
					break;
				case MAERSK_TYPE:
					out("processing MAERSK type");
//					processMAERSK(pageCount);
					break;
				case MSC_TYPE:
					out("processing MSC type");
					processMSC(pageCount);
					break;
				case TURKON_TYPE:
					out("processing TURKON type");
					processTURKON(pageCount);
					break;
			}
			doc.close();
			currentFile.delete();
			filesList.remove(a);
			a--;
		}
		
		/**
		 * Next Steps 	- Read order files
		 * 				- Look inside and find the matching B/L
		 * 				- Merge and delete older file
		 */
	}
	
	public static void processCMA(int pageCount) {
		try {
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
				
			currentStartPage = 1;
			String currentBL = "", newBL = "";
			String shipId = "";
			
			boolean foundBL;
			for(int page = 1; page <= pageCount; page ++) {
				foundBL = false;
				pdfStripper.setStartPage(page);
				pdfStripper.setEndPage(page);
				String text = pdfStripper.getText(doc);

				// EXTRACT PAGE TO TEXT FILE
				BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
				bw.write(text);
				bw.close();
				
				File textfile = new File(TEXTFILE_PATH);
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String currentLine = br.readLine();

				while(currentLine != null) {
					matcher = PATTERN_CMA.matcher(currentLine);
					shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);
					
					if(shipMatcher.find()) {
						shipId = shipMatcher.group(1);
						out("found a ship ID on the following line:\n["+currentLine+"]");
					}
					if(matcher.find()) {
						foundBL = true;
						newBL = matcher.group(1);
						if(pageCount == 1) {
							if(!shipId.isEmpty()) {
								doc.save(LOCAL_FILE_PATH + shipId + ".pdf");
								searchAndMerge(LOCAL_FILE_PATH + shipId + ".pdf", shipId, CMA_TYPE, true);
							} else {
								String newFileName = getFileName(br, newBL);
								doc.save(LOCAL_FILE_PATH + newFileName + ".pdf");
								searchAndMerge(LOCAL_FILE_PATH + newFileName + ".pdf", newFileName, CMA_TYPE, false);
							}
							shipId = "";
						}
						else if(page == 1) currentBL = newBL;
						else if(!currentBL.equals(newBL)) {
							if(currentStartPage < page-1) {
								if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page-1, shipId, CMA_TYPE);
								else splitDocAndRename(doc, currentStartPage, page-1, getFileName(br, currentBL), CMA_TYPE);
								shipId = "";
							}
							else {
								if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, currentStartPage, shipId, CMA_TYPE);
								else splitDocAndRename(doc, currentStartPage, currentStartPage, getFileName(br, currentBL), CMA_TYPE);
								shipId = "";
							}
							currentStartPage = page; // NEW START PAGE FOR THE NEXT SPLIT
						} else if(page == pageCount) {
							if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page, shipId, CMA_TYPE);
							else splitDocAndRename(doc, currentStartPage, page, getFileName(br, currentBL), CMA_TYPE);
							shipId = "";
						}
						break;
					}
					currentLine = br.readLine();
				}
				if(!foundBL && page == pageCount) { 
					if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page, shipId, CMA_TYPE);
					else splitDocAndRename(doc, currentStartPage, page, currentBL, CMA_TYPE);
					shipId = "";
				}
				br.close();
			}
			doc.close();
		} catch (Exception e) {
			out("We got an exception from processCMA " + e);
			e.printStackTrace();
		}
	}

	public static void processCOSCO() {
		try {
			String fileName = currentFile.getAbsolutePath();
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
			
			matcher = PATTERN_COSCO.matcher(fileName);
			String currentBL = "";
			String shipId = "";
			
			String text = pdfStripper.getText(doc);

			// EXTRACT PAGE TO TEXT FILE
			BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
			bw.write(text);
			bw.close();
			
			File textfile = new File(TEXTFILE_PATH);
			BufferedReader br = new BufferedReader(new FileReader(textfile));
			String currentLine = br.readLine();

			while(currentLine != null) {
				shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);
				
				if(shipMatcher.find()) {
					shipId = shipMatcher.group(1);
					break;
				}
				currentLine = br.readLine();
			}
			
			if (matcher.find()) currentBL = matcher.group(1);
			if(!shipId.isBlank()) {
				doc.save(LOCAL_FILE_PATH+shipId+".pdf");
				searchAndMerge(LOCAL_FILE_PATH+shipId+".pdf", shipId, COSCO_TYPE, true);
			}
			else if(!currentBL.isBlank()) {
				doc.save(LOCAL_FILE_PATH+"COSU"+currentBL+".pdf");
				searchAndMerge(LOCAL_FILE_PATH+"COSU"+currentBL+".pdf", currentBL, COSCO_TYPE, false);
			}
			shipId = "";
			doc.close();
		} catch (Exception e) {
			out("We got an exception from processCOSCO " + e);
			e.printStackTrace();
		}
	}
	
	public static void processEVERGREEN(int pageCount) {
		try {
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
			
			currentStartPage = 1;
			String currentBL = "", newBL = "";
			String shipId = "";
			
			boolean foundProof = false, foundBL;
			
			for(int page = 1; page <= pageCount; page ++) {
				foundBL = false;
				newBL = "";
				pdfStripper.setStartPage(page);
				pdfStripper.setEndPage(page);
				String text = pdfStripper.getText(doc);

				// EXTRACT PAGE TO TEXT FILE
				BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
				bw.write(text);
				bw.close();
				
				File textfile = new File(TEXTFILE_PATH);
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String currentLine = br.readLine();

				while(currentLine != null) {
					matcher = PATTERN_EVERGREEN.matcher(currentLine);
					shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);

					if(shipMatcher.find()) {
						shipId = shipMatcher.group(1);
					}
					if(!foundProof) { // IF THERE IS NO "BILL OF LADING NO. ," just ignore
						if(currentLine.contains("BILL OF LADING NO. ")) foundProof = true;
					} else if(matcher.find()) {
						foundBL = true;
						newBL = matcher.group(1);
						if(pageCount == 1) {
							if(!shipId.isEmpty()) {
								doc.save(LOCAL_FILE_PATH + shipId + ".pdf");
								searchAndMerge(LOCAL_FILE_PATH + shipId + ".pdf", shipId, EVERGREEN_TYPE, true);
							} else {
								String newFileName = getFileName(br, "EGLV" + newBL);
								doc.save(LOCAL_FILE_PATH + newFileName + ".pdf");
								searchAndMerge(LOCAL_FILE_PATH + newFileName + ".pdf", newFileName, EVERGREEN_TYPE, false);
							}
							shipId = "";
						}
						else if(page == 1) break;
						else if(currentBL == newBL) break; // IF SAME BL IS FOUND, MOVE ON TO THE NEXT PAGE
						else if(currentBL == "") { // THERE WERE NO BLs FOUND BEFORE THIS NEW ONE, SO DISCARD PREVIOUS PAGES
							currentStartPage = page;
							page--;
							currentBL = newBL;
							shipId = getFileName(br, currentBL);
							break;
						} else { // NEW BL FOUND AND THE OLD ONE SHOULD BE SPLIT
							if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page-1, shipId, EVERGREEN_TYPE);
							else splitDocAndRename(doc, currentStartPage, page-1, getFileName(br, "EGLV"+currentBL), EVERGREEN_TYPE);
							shipId = "";
							currentStartPage = page;
						}
						currentBL = newBL;
					} else if(!currentBL.isEmpty() && currentLine.contains(currentBL)) { // FOLLOWING PAGES WITH THE SAME BL BUT NOT THE MAIN INVOICE PAGE
						foundBL = true;
						break;					
					}
					currentLine = br.readLine();
				}
				if(!foundBL && !currentBL.isEmpty()) { // WE DIDN'T FIND A BL BUT WE HAD A PREVIOUS ONE, SO WE END THE SPLIT BEFORE THIS PAGE 
					if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page-1, shipId, EVERGREEN_TYPE);
					else splitDocAndRename(doc, currentStartPage, page-1, "EGLV"+currentBL, EVERGREEN_TYPE);
					shipId = "";
					currentStartPage = page;
					currentBL = "";
				}
				if(foundBL && page == pageCount) {
					if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage,page, shipId, EVERGREEN_TYPE);
					else splitDocAndRename(doc, currentStartPage,page, "EGLV"+currentBL, EVERGREEN_TYPE);
					shipId = "";
				}
				br.close();
			}
			doc.close();
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
			BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
			
			// EXTRACT PAGE TO TEXT FILE
			doc.close();
			bw.write(text);
			bw.close();
			
			File textfile = new File(TEXTFILE_PATH);
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
			String fileName = currentFile.getAbsolutePath();
			matcher = PATTERN_MSC.matcher(fileName);
			String currentBL = "";
			String shipId = "";
			
			if (matcher.find()) currentBL = matcher.group(1);
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
				
			for(int page = 1; page <= pageCount; page ++) {
				pdfStripper.setStartPage(page);
				pdfStripper.setEndPage(page);
				String text = pdfStripper.getText(doc);

				// EXTRACT PAGE TO TEXT FILE
				BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
				bw.write(text);
				bw.close();
				
				File textfile = new File(TEXTFILE_PATH);
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String currentLine = br.readLine();
				while(currentLine != null) {
					shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);
					if(shipMatcher.find()) {
						shipId = shipMatcher.group(1);
					}
					if(currentLine.contains(currentBL)) {
						if(!shipId.isEmpty()) splitDocAndRename(doc, page, page, shipId, MSC_TYPE);
						else splitDocAndRename(doc, page, page, getFileName(br, currentBL), MSC_TYPE);
						shipId = "";
						break;
					}
					currentLine = br.readLine();
				}
				br.close();
			}
			doc.close();
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
			String shipId = "";

			for(int page = 1; page <= pageCount; page ++) {
				boolean foundBL = false;
				pdfStripper.setStartPage(page);
				pdfStripper.setEndPage(page);
				String text = pdfStripper.getText(doc);

				// EXTRACT PAGE TO TEXT FILE
				BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
				bw.write(text);
				bw.close();
				
				File textfile = new File(TEXTFILE_PATH);
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String currentLine = br.readLine();

				while(currentLine != null) {
					matcher = PATTERN_TURKON.matcher(currentLine);
					shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);

					if(shipMatcher.find()) {
						shipId = shipMatcher.group(1);
					}
					if(matcher.find()) {
						foundBL = true;
						if(currentBL != "") {
							if(currentStartPage < page-1) {
								if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page-1, shipId, TURKON_TYPE);
								else splitDocAndRename(doc, currentStartPage, page-1, getFileName(br, currentBL), TURKON_TYPE);
								shipId = "";
							}
							else {
								if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, currentStartPage, shipId, TURKON_TYPE);
								else splitDocAndRename(doc, currentStartPage, currentStartPage, getFileName(br, currentBL), TURKON_TYPE);
								shipId = "";
							}
							currentStartPage = page; // NEW START PAGE FOR THE NEXT SPLIT
						}
						currentBL = matcher.group(1);
						shipId = getFileName(br, currentBL);
						blCounter++;
						break;
					}
					currentLine = br.readLine();
				}
				if(!foundBL && page == pageCount) {
					if(!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page, shipId, TURKON_TYPE);
					else splitDocAndRename(doc, currentStartPage, page, currentBL, TURKON_TYPE);
					shipId = "";
				}
				br.close();
			}
			doc.close();
		} catch (Exception e) {
			out("We got an exception from processTURKON " + e);
			e.printStackTrace();
		}
	}
	
	public static String getFileName(BufferedReader br, String BL) throws Exception {
		String currentLine = br.readLine();
		String ship;
		while(currentLine != null) {
			shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);
			if(shipMatcher.find()) {
				ship = shipMatcher.group(1);
				out("from line ["+currentLine+"]");
				return ship;
			}
			currentLine = br.readLine();
		}
		return BL;
	}

	
	public static void splitDocAndRename(PDDocument doc, int start, int end, String newName, int carrierType) throws IOException {
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
		out("deleted files: ");
		for (int page = 0; page < newDoc.size(); page++) {
			name = LOCAL_FILE_PATH + newName + "_" + page + ".pdf";
			File file = new File(name);
			file.delete();
			out("["+page+"] "+ name);
		}
		out(" as they've already been merged with name "+newName);

		shipMatcher = PATTERN_SHIP_ID.matcher(newName);
		// UPDATE TO ACTUALLY MERGE AND DELETE
		if (searchAndMerge(LOCAL_FILE_PATH + newName + ".pdf", newName, carrierType, shipMatcher.find())) {
			File file = new File(LOCAL_FILE_PATH + newName + ".pdf");
			file.delete();
		}
	}
	
	public static int determineFileType(File file) {
		out("DETERMINING FILE TYPE...");
		try {
			PDDocument doc = PDDocument.load(file);
			PDFTextStripper pdfStripper = new PDFTextStripper();
			String text = pdfStripper.getText(doc);
			BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
			
			//Extract pdf to textfile
			doc.close();
			bw.write(text);
			bw.close();
			
			File textfile = new File(TEXTFILE_PATH);
			BufferedReader br = new BufferedReader(new FileReader(textfile));
			String currentLine = br.readLine();
			
			while(currentLine != null) {
				if(currentLine.toUpperCase().contains("COSCO"))	return COSCO_TYPE;
				if(currentLine.toUpperCase().contains("EVERGREEN")) return EVERGREEN_TYPE;
				if(currentLine.toUpperCase().contains("INVOICING AND DISPUTES"))	return MSC_TYPE;
				if(currentLine.toUpperCase().contains("TURKON"))	return TURKON_TYPE;
				currentLine = br.readLine();
			}
			br.close();
		} catch (Exception ex) {
			out("we got an exception in determineFileType function, it is "+ ex);
			ex.printStackTrace();
		}
		return CMA_TYPE;
	}
	
	public static boolean searchAndMerge(String filePath, String shipOrBill, int type, boolean isShipmentID) {
		try {
			String fileName;
			for(int i = 0; i < ordersList.size(); i++) {
				fileName = ordersList.get(i);
				if(isShipmentID) {
					out("it is a shipment ID");
					if(!fileName.contains(shipOrBill)) {
						continue;
					}
					out("we found a match: ");
					out("SHIPMENT ID >> " + shipOrBill + ".pdf");
					out("ORDER FILE >> " + fileName);
					
					// MERGE
					File file = new File(filePath);
					File orderFile = new File(fileName);
					
					// BEGIN COMMENT -----
					PDFMergerUtility mergerPdf = new PDFMergerUtility();
					mergerPdf.setDestinationFileName(fileName);
					mergerPdf.addSource(orderFile);
					mergerPdf.addSource(file);
					mergerPdf.mergeDocuments();
					
					out("we are renaming the above order file to have 'BL' at the end as proof that we have attached the bill of lading");
					// RENAME
					orderFile.renameTo(new File(fileName.substring(0,fileName.length()-4)+" BL.pdf"));
					// END COMMENT -----
					
					// DELETE FROM LIST
					ordersList.remove(i);
					out("deleted from the list and returning true");
					return true;
				}
				if(isShipmentID) out("THIS LINE SHOULD NOT HAVE BEEN REACHED");
				out("it is not a shipment ID");
				PDDocument doc = PDDocument.load(new File(fileName));
				PDFTextStripper pdfStripper = new PDFTextStripper();
				out("===READING " + fileName);
				String text = pdfStripper.getText(doc);

				switch (type) {
					case EVERGREEN_TYPE:
						out("EVERGREEN");
						if (fileName.contains("ID") || fileName.contains("TR")) {
							doc.close();
							continue;
						}						
					case MAERSK_TYPE:
						out("MAERSK");
						if (!fileName.contains("MAERSK")) {
							doc.close();
							continue;
						}
					case TURKON_TYPE:
						out("TURKON");
						if (!fileName.contains("TR")) {
							doc.close();
							continue;
						}
				}

				BufferedWriter bw = new BufferedWriter(new FileWriter("text.txt"));
				out("this file is "+ TYPE[type]);
				out("we passed switch block");
				// EXTRACT TO TEXTFILE
				bw.write(text);
				bw.close();
				File textfile = new File("text.txt");
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String tempString = br.readLine();
				
				// FIND MATCH
				while (tempString != null) {
					if(tempString.contains(shipOrBill)) {
						// MATCH THE BILL OF LADING TO THIS ORDER FILE
						out("we found a match: ");
						out("BILL OF LADING >> " + shipOrBill + ".pdf");
						out("ORDER FILE >> " + fileName);
						// MERGE
						File file = new File(filePath);
						File orderFile = new File(fileName);
						
						// BEGIN COMMENT -----
						PDFMergerUtility mergerPdf = new PDFMergerUtility();
						mergerPdf.setDestinationFileName(fileName);
						mergerPdf.addSource(orderFile);
						mergerPdf.addSource(file);
						mergerPdf.mergeDocuments();

						out("we are renaming the above order file to have 'BL' at the end as proof that we have attached the bill of lading--2");
						// RENAME
						orderFile.renameTo(new File(fileName.substring(0,fileName.length()-4)+" BL.pdf"));
						// END COMMENT -----
						
						// DELETE FROM LIST
						ordersList.remove(i);
						br.close();
						doc.close();
						return true;
					}
					out(tempString);
					out("does not contain " + shipOrBill);
					tempString = br.readLine();
				}
				br.close();
				doc.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	private static List <String> retrieveAllFiles() throws Exception {
		try (Stream<Path> walk = Files.walk(Paths.get(ULTIMATE_FILE_PATH))) {
			return walk.filter(p -> !Files.isDirectory(p)).map(p -> p.toString()).filter(f -> f.toLowerCase().endsWith(".pdf")).collect(Collectors.toList());
		}
	}
	
	private static List <String> retrieveOrderFiles() throws Exception {
		try (Stream<Path> walk = Files.walk(Paths.get(ORDERS_FILE_PATH))) {
			return walk.filter(p -> !Files.isDirectory(p)).map(p -> p.toString()).filter(f -> f.toLowerCase().endsWith(".pdf")).filter(f2 -> !f2.toUpperCase().contains("BL.PDF")).collect(Collectors.toList());
		}
	}
	
	public static void out(String stringToPrint) {
		System.out.println(stringToPrint);
		printLog += stringToPrint + "\n";
	}
}
