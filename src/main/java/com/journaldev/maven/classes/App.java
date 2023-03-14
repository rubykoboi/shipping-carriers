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
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class App {

	private final static int CMA_TYPE = 0;
	private final static int COSCO_TYPE = 1;
	private final static int EVERGREEN_TYPE = 2;
	private final static int MAERSK_TYPE = 3;
	private final static int MSC_TYPE = 4;
	private final static int TURKON_TYPE = 5;
	private final static int WAN_HAI_TYPE = 6;
	private final static String[] TYPE = {"CMA","COSCO","EVERGREEN","MAERSK","MSC","TURKON","WAN HAI"};
//	/** SHARED PATHS
	private final static String ORDERS_FILE_PATH0 = "I:\\2020\\";
	private final static String ORDERS_FILE_PATH1 = "I:\\2021\\";
	private final static String ORDERS_FILE_PATH2 = "I:\\2022\\";
	private final static String ORDERS_FILE_PATH3 = "I:\\2023\\";
	private final static String ARRIVAL_NOTICES_FILE_PATH = "S:\\Purchasing\\GeneralShare\\ARRIVAL NOTICES\\";
	private final static String EXCEL_FILE = "S:\\Purchasing\\GeneralShare\\ARRIVAL NOTICES\\ShipmentIDs.xlsx";
	private final static String TEXTFILE_PATH = "S:\\Purchasing\\GeneralShare\\Robbi Programs\\LOG FILES\\Shipping Couriers Organizer_LOG_FILE.txt";
//	**/
	/** LOCAL PATHS
	private final static String ORDERS_FILE_PATH = "C:\\Orders\\";
	private final static String ARRIVAL_NOTICES_FILE_PATH = "C:\\SC\\";
	private final static String EXCEL_FILE = "C:\\SC\\ShipmentIDs.xlsx";
	private final static String TEXTFILE_PATH = "C:\\SC\\junk.txt";
	**/
	private List<String> filesList;
	private static List<String> ordersList;
	private static boolean processed;
	
	private final static String CMA_BL_REGEX = "(\\b([A-Z]{3}\\d{7}[A-Z]{0,1})\\s*)|^([A-Z]{4}\\d{6})[^\\S]";
	private final static String COSCO_BL_REGEX = "(?<=BL)(\\d{10})\\s*";
	private final static String EVERGREEN_BL_REGEX = "(?<=EGLV)(\\d{12})\\s*";
	private final static String MAERSK_BL_REGEX = "(?<=MAEU \\- )(.*?)(?=B/L No:)";
	private final static String MSC_BL_REGEX = "(MEDU[A-Z]{1,2}\\d{6,7})";
	private final static String TURKON_BL_REGEX = "\\s*(\\d{8}\\d{0,2})(?=BILL OF LADING)";
	private final static String WAN_HAI_BL_REGEX = "(?<=_)(\\d{3}CA\\d{5})|(\\d{3}C\\d{6})";
	private final static String SHIP_ID_REGEX = "\\s*(?<![A-Z])((EG|BE|CN|IN|ID|MY|NP|PK|TW|TH|VN|TR|UA|AR)\\d{5})\\s*";
	private final static String ARRIVAL_NOTICE_REGEX = "( AN)";

	private static HashMap<String, String> billToShipPair = new HashMap<String, String>();
	public static Pattern PATTERN_CMA;  
	public static Pattern PATTERN_COSCO;
	public static Pattern PATTERN_EVERGREEN;
	public static Pattern PATTERN_MAERSK;
	public static Pattern PATTERN_MSC;
	public static Pattern PATTERN_TURKON;
	public static Pattern PATTERN_WAN_HAI;
	public static Pattern PATTERN_SHIP_ID;
	public static Pattern PATTERN_AN;
	
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
					out("PROGRAM SUCCESSFULLY EXECUTED");
					BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
					bw.write(printLog);
					bw.close();
				} catch (Exception e) {e.printStackTrace();}
			}
		});
	}
	
	public App() throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook(EXCEL_FILE);
		populateShipIds(workbook);

		// INITIALIZE ALL BL# PATTERNS
		PATTERN_CMA = Pattern.compile(CMA_BL_REGEX);
		PATTERN_COSCO = Pattern.compile(COSCO_BL_REGEX);
		PATTERN_EVERGREEN = Pattern.compile(EVERGREEN_BL_REGEX);
		PATTERN_MAERSK = Pattern.compile(MAERSK_BL_REGEX);
		PATTERN_MSC = Pattern.compile(MSC_BL_REGEX);
		PATTERN_TURKON = Pattern.compile(TURKON_BL_REGEX);
		PATTERN_WAN_HAI = Pattern.compile(WAN_HAI_BL_REGEX);
		PATTERN_SHIP_ID = Pattern.compile(SHIP_ID_REGEX);
		PATTERN_AN = Pattern.compile(ARRIVAL_NOTICE_REGEX);

		// RETREIVE ALL PDFs IN FOLDER PATH
		filesList = retrieveAllFiles();	// ALL PDFS IN THE ARRIVAL NOTICES FOLDER
//		ordersList = retrieveOrderFiles(ORDERS_FILE_PATH);
		ordersList = retrieveOrderFiles(ORDERS_FILE_PATH0);
		ordersList.addAll(retrieveOrderFiles(ORDERS_FILE_PATH1));
		ordersList.addAll(retrieveOrderFiles(ORDERS_FILE_PATH2));
		ordersList.addAll(retrieveOrderFiles(ORDERS_FILE_PATH3));
		out("# of Arrival Notices in S:\\Purchasing\\GeneralShare\\ARRIVAL NOTICES\\: " + filesList.size());
		out("# of shipment order files in I:\\2022: " + ordersList.size());
		
		int fileTypes[] = new int[filesList.size()];
		int pageCount;
		String filename;
		

		out("Processing begins...");
		// PROCESS ALL FILES: SPLIT AND RENAME WITH CORRESPONDING B/L #S
		for(int a=0; a<filesList.size(); a++) {
			filename = filesList.get(a);
			processed = false;
			currentFile = new File(filesList.get(a));
			out("Processing file ["+a+"] : "+filename);
			int index = filename.lastIndexOf("\\")+1;
			
			// DETERMINE EACH FILE's SHIPPING CARRIER ORIGIN
			fileTypes[a] = determineFileType(currentFile);
			out("FILE TYPE is " + TYPE[fileTypes[a]]);
			
			if (filename.substring(index, index + 3).equals("BOL")) {
				String shipID = getFileName(filename.substring(index+4,filename.lastIndexOf(".")));
				out("Identifier: " + shipID);
				if (shipID.matches(SHIP_ID_REGEX)) {
					out(shipID + " looks like a shipment ID");
					if(searchAndMerge(filename, shipID)) out("MERGED file with shipID: " +shipID);
					else {
						out("FAILED TO MERGE, matching file not found..? renaming the file with shipment ID found");
						File file = new File(filesList.get(a));
						String newFileName = filename.substring(0, index) + "SID " +shipID+".pdf";
						file.renameTo(new File(newFileName));
					}
				} else {
					out(shipID + " does not look like a shipment ID");
				}
				continue;
			} else if (filename.substring(index, index + 3).equals("SID")) {
				if(searchAndMerge(filename, filename.substring(index+4,filename.lastIndexOf("."))))
					out("SUCCESSFULLY MERGED " + filename);
				else out("FILE DID NOT GET MERGED: " + filename);
				continue;
			}
			
			shipMatcher = PATTERN_SHIP_ID.matcher(filename);
			if (shipMatcher.find()) {
				if(searchAndMerge(filename,shipMatcher.group(1)))
					out("SUCCESSFULLY MERGED " + filename);
				else out("FILE DID NOT GET MERGED: " + filename);
			}
			PDDocument doc = PDDocument.load(currentFile);
			pageCount = doc.getNumberOfPages();
			
			switch (fileTypes[a]) {
				case CMA_TYPE:
					processCMA(pageCount);
					break;
				case COSCO_TYPE:
					processCOSCO();
					break;
				case EVERGREEN_TYPE:
					processEVERGREEN(pageCount);
					break;
				case MAERSK_TYPE:
					processMAERSK(pageCount);
					break;
				case MSC_TYPE:
					processMSC(pageCount);
					break;
				case TURKON_TYPE:
					processTURKON(pageCount);
					break;
				case WAN_HAI_TYPE:
					processWANHAI();
					break;
			}
			doc.close();
			if (processed) {
				currentFile.delete();
				filesList.remove(a);
				a--;
			}
		}
		out("...process finalized");
	}
	
	public static void processCMA(int pageCount) {
		try {
			out("CMA process");
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
			File textfile = new File(TEXTFILE_PATH);
			
			currentStartPage = 1;
			String currentBL = "", newBL = "", shipId = "";
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
				
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String currentLine = br.readLine();

				while(currentLine != null) {
					matcher = PATTERN_CMA.matcher(currentLine);
					shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);
					
					if (shipMatcher.find()) shipId = shipMatcher.group(1);
					if (matcher.find()) {
						processed = true;
						foundBL = true;
						newBL = matcher.group(1);
						if(newBL == null) newBL = matcher.group(3);
						out("BOL found ==>" + newBL);
						if (pageCount == 1) {
							if (!shipId.isEmpty()) {
								doc.save(ARRIVAL_NOTICES_FILE_PATH + "SID " + shipId + ".pdf");
								if(searchAndMerge(ARRIVAL_NOTICES_FILE_PATH + "SID " + shipId + ".pdf", shipId))
									out("SUCCESSFULLY MERGED " + ARRIVAL_NOTICES_FILE_PATH + "SID " + shipId + ".pdf");
								else out("FILE DID NOT GET MERGED: " + ARRIVAL_NOTICES_FILE_PATH + "SID " + shipId + ".pdf");
								doc.close();
							} else {
								String newFileName = getFileName(newBL);
								if (newFileName.matches(SHIP_ID_REGEX)) {
									doc.save(ARRIVAL_NOTICES_FILE_PATH + "SID " + newFileName + ".pdf");
									if(searchAndMerge(ARRIVAL_NOTICES_FILE_PATH + "SID " + newFileName + ".pdf", newFileName))
										out("SUCCESSFULLY MERGED " + ARRIVAL_NOTICES_FILE_PATH + "SID " + newFileName + ".pdf");
									else out("FILE DID NOT GET MERGED: " + ARRIVAL_NOTICES_FILE_PATH + "SID " + newFileName + ".pdf");
								} else {
									doc.save(ARRIVAL_NOTICES_FILE_PATH + "BOL " + newFileName + ".pdf");
								}
								doc.close();
							}
							shipId = "";
						} else if (page == 1) currentBL = newBL;
						else if (!currentBL.equals(newBL)) {
							if (currentStartPage < page-1) {
								if (!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page-1, shipId, CMA_TYPE);
								else splitDocAndRename(doc, currentStartPage, page-1, getFileName(currentBL), CMA_TYPE);
								shipId = "";
							} else {
								if (!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, currentStartPage, shipId, CMA_TYPE);
								else splitDocAndRename(doc, currentStartPage, currentStartPage, getFileName(currentBL), CMA_TYPE);
								shipId = "";
							}
							currentStartPage = page; // NEW START PAGE FOR THE NEXT SPLIT
						} else if (page == pageCount) {
							if (!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page, shipId, CMA_TYPE);
							else splitDocAndRename(doc, currentStartPage, page, getFileName(currentBL), CMA_TYPE);
							shipId = "";
						}
						break;
					}
					currentLine = br.readLine();
				}
				if (!foundBL && page == pageCount) {
					if (!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page, shipId, CMA_TYPE);
					else splitDocAndRename(doc, currentStartPage, page, getFileName(currentBL), CMA_TYPE);
					shipId = "";
				}
				br.close();
			}
			doc.close();
			textfile.delete();
		} catch (Exception e) {
			out("Exception from processCMA: " + e.toString());
			e.printStackTrace();
		}
	}

	public static void processCOSCO() {
		try {
			out("COSCO process");
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

			while (currentLine != null) {
				shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);
				if (shipMatcher.find()) {
					shipId = shipMatcher.group(1);
					break;
				}
				currentLine = br.readLine();
			}
			br.close();
			textfile.delete();
			
			if (matcher.find()) {
				processed = true;
				currentBL = matcher.group(1);
			}
			if (!shipId.isBlank()) {
				doc.save(ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
				if(searchAndMerge(ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf", shipId))
					out("SUCCESSFULLY MERGED " + ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
				else out("FILE DID NOT GET MERGED: " + ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
					
			} else if (!currentBL.isBlank()) {
				shipId = getFileName(currentBL);
				if (shipId.matches(SHIP_ID_REGEX)) {
					doc.save(ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
					if(searchAndMerge(ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf", shipId))
						out("SUCCESSFULLY MERGED " + ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
					else out("FILE DID NOT GET MERGED: " + ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
				} else {
					doc.save(ARRIVAL_NOTICES_FILE_PATH+"BOL COSU"+shipId+".pdf");
					if(searchAndMerge(ARRIVAL_NOTICES_FILE_PATH+"BOL COSU"+shipId+".pdf", shipId))
						out("SUCCESSFULLY MERGED " + ARRIVAL_NOTICES_FILE_PATH+"BOL COSU"+shipId+".pdf");
					else out("FILE DID NOT GET MERGED: " + ARRIVAL_NOTICES_FILE_PATH+"BOL COSU"+shipId+".pdf");
				}
			}
			
			shipId = "";
			doc.close();
		} catch (Exception e) {
			out("Exception from processCOSCO: " + e.toString());
			e.printStackTrace();
		}
	}
	
	public static void processEVERGREEN(int pageCount) {
		try {
			out("EVERGREEN process");
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
			
			currentStartPage = 1;
			String currentBL = "", newBL = "", shipId = "";
			
			boolean foundProof = false, foundBL;
			File textfile = new File(TEXTFILE_PATH);
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
				
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String currentLine = br.readLine();

				while(currentLine != null) {
					matcher = PATTERN_EVERGREEN.matcher(currentLine);
					shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);

					if (shipMatcher.find()) {
						shipId = shipMatcher.group(1);
						out("Shipment ID: " +shipId);
					}
					if (!foundProof) { // IF THERE IS NO "BILL OF LADING NO. ," just ignore
						if (currentLine.contains("BILL OF LADING NO. ")) {
							foundProof = true;
						}
					} else if (matcher.find()) {
						foundBL = true;
						newBL = matcher.group(1);
						if (pageCount == 1) {
							shipId = getFileName(newBL);
							break;
						} else if (page == 1) {
							break;
						} else if (currentBL == newBL) {
							break; // IF SAME BL IS FOUND, MOVE ON TO THE NEXT PAGE
						} else if (currentBL == "") { // THERE WERE NO BLs FOUND BEFORE THIS NEW ONE, SO DISCARD PREVIOUS PAGES
							currentStartPage = page;
							currentBL = newBL;
							shipId = getFileName(currentBL);
							break;
						} else { // THERE WAS AN OLD BL 
							out("Previous BL : "+currentBL);
							if (!shipId.isEmpty()) {
								splitDocAndRename(doc, currentStartPage, page-1, shipId, EVERGREEN_TYPE);
							}
							else {
								splitDocAndRename(doc, currentStartPage, page-1, getFileName(currentBL), EVERGREEN_TYPE);
							}
							shipId = "";
							currentStartPage = page;
						}
						currentBL = newBL;
					} else if (!currentBL.isEmpty() && currentLine.contains(currentBL)) { // FOLLOWING PAGES WITH THE SAME BL BUT NOT THE MAIN INVOICE PAGE
						foundBL = true;
						break;					
					}
					currentLine = br.readLine();
				}
				if (!foundBL && !currentBL.isEmpty()) { // WE DIDN'T FIND A BL BUT WE HAD A PREVIOUS ONE, SO WE END THE SPLIT BEFORE THIS PAGE 
					if (!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page-1, shipId, EVERGREEN_TYPE);
					else splitDocAndRename(doc, currentStartPage, page-1, getFileName(currentBL), EVERGREEN_TYPE);
					shipId = "";
					currentStartPage = page;
					currentBL = "";
				}
				if (foundBL && page == pageCount) {
					if (!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage,page, shipId, EVERGREEN_TYPE);
					else splitDocAndRename(doc, currentStartPage,page, getFileName(currentBL), EVERGREEN_TYPE);
					shipId = "";
				}
				br.close();
			}
			if (!foundProof) processed = true;
			doc.close();
			textfile.delete();
		} catch (Exception e) {
			out("Exception from processEVERGREEN: " + e.toString());
			e.printStackTrace();
		}
	}
	
	public static void processMAERSK(int pageCount) {
		try {
			out("MAERSK process");
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
			String text = pdfStripper.getText(doc), shipId, currentBL = "";
			BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
			
			// EXTRACT PAGE TO TEXT FILE
			bw.write(text);
			bw.close();
			processed = false;
			File textfile = new File(TEXTFILE_PATH);
			BufferedReader br = new BufferedReader(new FileReader(textfile));
			String currentLine = br.readLine();
			while(currentLine != null) {
				shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);
				if (shipMatcher.find()) {
					processed = true;
					shipId = shipMatcher.group(1);
					splitDocAndRename(doc, 1, pageCount, shipId, MAERSK_TYPE);
					break;
				}
				currentLine = br.readLine();
				if(currentLine == null) break;
				matcher = PATTERN_MAERSK.matcher(currentLine);
				if (matcher.find())	{
					currentBL = matcher.group(1);
				}
				currentLine = br.readLine();
			}
			if(!processed) splitDocAndRename(doc, 1, pageCount, getFileName(currentBL), MAERSK_TYPE);

			doc.close();
			br.close();
			textfile.delete();
		} catch (Exception e) {
			out("Exception from processMAERSK: " + e.toString());
			e.printStackTrace();
		}
	}
	
	public static void processMSC(int pageCount) {
		try {
			out("MSC process");
			String fileName = currentFile.getAbsolutePath();
			matcher = PATTERN_MSC.matcher(fileName);
			String currentBL = "", shipId = "";
			boolean foundBL = false, processed = false;
			int savePageStart = 1, savePageEnd = 1;
			if (matcher.find()) currentBL = matcher.group(1);
			out("BL --> " + currentBL);
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();

			File textfile = new File(TEXTFILE_PATH);
			for(int page = 1; page <= pageCount; page ++) {
				pdfStripper.setStartPage(page);
				pdfStripper.setEndPage(page);
				String text = pdfStripper.getText(doc);

				// EXTRACT PAGE TO TEXT FILE
				BufferedWriter bw = new BufferedWriter(new FileWriter(TEXTFILE_PATH));
				bw.write(text);
				bw.close();
				
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String currentLine = br.readLine();
				while (currentLine != null) {
					shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);
					if (shipMatcher.find()) shipId = shipMatcher.group(1);
					if (currentLine.contains(currentBL)) {
						savePageEnd = page;
						if(!foundBL) {
							foundBL = true;
							savePageStart = page;
						}
						if(page == pageCount) {
							if (!shipId.isEmpty()) splitDocAndRename(doc, savePageStart, savePageEnd, shipId, MSC_TYPE);
							else splitDocAndRename(doc, savePageStart, savePageEnd, getFileName(currentBL), MSC_TYPE);
							processed = true;
							shipId = "";
							break;	
						} 
						break; // Goes to next page because we know that this page has the BL #
					}
					currentLine = br.readLine();
				}
				br.close();
				if(processed) break;
				if(foundBL) continue;
			}
			if(!processed && foundBL) {
				if (!shipId.isEmpty()) splitDocAndRename(doc, savePageStart, savePageEnd, shipId, MSC_TYPE);
				else splitDocAndRename(doc, savePageStart, savePageEnd, getFileName(currentBL), MSC_TYPE);
				shipId = "";
			}
			doc.close();
			textfile.delete();
		} catch (Exception e) {
			out("Exception from processMSC: " + e.toString());
			e.printStackTrace();
		}
	}
	
	public static void processTURKON(int pageCount) {
		try {
			out("TURKON process");
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();
			
			currentStartPage = 1;
			String currentBL = "", shipId = "";
			File textfile = new File(TEXTFILE_PATH);
			
			for(int page = 1; page <= pageCount; page ++) {
				boolean foundBL = false;
				pdfStripper.setStartPage(page);
				pdfStripper.setEndPage(page);
				String text = pdfStripper.getText(doc);

				// EXTRACT PAGE TO TEXT FILE
				BufferedWriter bw = new BufferedWriter(new FileWriter(textfile));
				bw.write(text);
				bw.close();
				BufferedReader br = new BufferedReader(new FileReader(textfile));
				String currentLine = br.readLine();

				while(currentLine != null) {
					matcher = PATTERN_TURKON.matcher(currentLine);
					shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);

					if (shipMatcher.find()) shipId = shipMatcher.group(1);
					if (matcher.find()) {
						foundBL = true;
						if (currentBL != "") {
							if (currentStartPage < page-1) {
								if (!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page-1, shipId, TURKON_TYPE);
								else splitDocAndRename(doc, currentStartPage, page-1, getFileName(currentBL), TURKON_TYPE);
							} else {
								if (!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, currentStartPage, shipId, TURKON_TYPE);
								else splitDocAndRename(doc, currentStartPage, currentStartPage, getFileName(currentBL), TURKON_TYPE);
							}
							currentStartPage = page; // NEW START PAGE FOR THE NEXT SPLIT
						}
						currentBL = matcher.group(1);
						shipId = getFileName(currentBL);
						break;
					}
					currentLine = br.readLine();
				}
				if (!foundBL && page == pageCount) {
					if (!shipId.isEmpty()) splitDocAndRename(doc, currentStartPage, page, shipId, TURKON_TYPE);
					else splitDocAndRename(doc, currentStartPage, page, getFileName(currentBL), TURKON_TYPE);
					shipId = "";
				}
				br.close();
			}
			doc.close();
			textfile.delete(); 
		} catch (Exception e) {
			out("Exception from processTURKON: " + e.toString());
			e.printStackTrace();
		}
	}
	
	public static void processWANHAI() {
		try {
			out("WanHai process");
			String fileName = currentFile.getAbsolutePath();
			PDDocument doc = PDDocument.load(currentFile);
			PDFTextStripper pdfStripper = new PDFTextStripper();

			matcher = PATTERN_WAN_HAI.matcher(fileName);
			String text = pdfStripper.getText(doc);
			String currentBL = "", shipId = "";
			File textfile = new File(TEXTFILE_PATH);

			// EXTRACT PAGE TO TEXT FILE
			BufferedWriter bw = new BufferedWriter(new FileWriter(textfile));
			bw.write(text);
			bw.close();

			BufferedReader br = new BufferedReader(new FileReader(textfile));
			String currentLine = br.readLine();

			if (matcher.find()) {
				processed = true;
				currentBL = matcher.group(1);
				if(currentBL == null) currentBL = matcher.group(2);
			}
			if (!currentBL.isBlank()) {
				shipId = getFileName(currentBL);
				if (shipId.matches(SHIP_ID_REGEX)) {
					out("ship ID found: " + shipId);
					doc.save(ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
					if(searchAndMerge(ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf", shipId))
						out("SUCCESSFULLY MERGED " + ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
					else out("DID NOT GET TO MERGE " + ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
				} else {
					while (currentLine != null) {
						shipMatcher = PATTERN_SHIP_ID.matcher(currentLine);
						if (shipMatcher.find()) {
							shipId = shipMatcher.group(1);
							break;
						}
						currentLine = br.readLine();
					}
					if (!shipId.isBlank()) {
						doc.save(ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
						if(searchAndMerge(ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf", shipId))
							out("SUCCESSFULLY MERGED " + ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
						else out("DID NOT GET TO MERGE " + ARRIVAL_NOTICES_FILE_PATH+"SID "+shipId+".pdf");
					} else {
						doc.save(ARRIVAL_NOTICES_FILE_PATH+"BOL "+shipId+".pdf");
						if(searchAndMerge(ARRIVAL_NOTICES_FILE_PATH+"BOL "+shipId+".pdf", shipId))
							out("SUCCESSFULLY MERGED " + ARRIVAL_NOTICES_FILE_PATH+"BOL "+shipId+".pdf");
						else out("DID NOT GET TO MERGE " + ARRIVAL_NOTICES_FILE_PATH+"BOL "+shipId+".pdf");
					}
				}
			}
			br.close();
			textfile.delete();
			shipId = "";
			doc.close();
		} catch (Exception e) {
			out("Exception from processWANHAI: " + e.toString());
			e.printStackTrace();
		}
	}
	
	public static String getFileName(String BL) throws Exception {
		String ship = billToShipPair.get(BL);
		if (ship == null) return BL.trim();
		return ship.trim();
	}
	
	public static void splitDocAndRename(PDDocument doc, int start, int end, String newName, int carrierType) throws IOException {
		if(newName.isBlank()) return;
		Splitter splitter = new Splitter();
		splitter.setStartPage(start);
		splitter.setEndPage(end);
		List<PDDocument> newDoc = splitter.split(doc);
		PDFMergerUtility mergerPdf = new PDFMergerUtility();
		String ID = newName;
		String name = "";
		processed = true;
		if (newName.matches(SHIP_ID_REGEX)) newName = "SID " + newName;
		else newName = "BOL "+newName;
		out(newDoc.size() + " is the size of the new document to be merging");
		out("BEGIN MERGING...");
		// MERGE ALL FILES OF THE SAME B/L#
		for (int page = 0; page < newDoc.size(); page++) {
			name = ARRIVAL_NOTICES_FILE_PATH + newName + "_" + page + ".pdf";
			newDoc.get(page).save(name);
			mergerPdf.addSource(name);
			out(name);
		}
		mergerPdf.setDestinationFileName(ARRIVAL_NOTICES_FILE_PATH + newName + ".pdf");
		mergerPdf.mergeDocuments();
		out("END MERGE: >> " + ARRIVAL_NOTICES_FILE_PATH + newName + ".pdf");
		
		// DELETE SPLIT PAGES AFTER MERGING
		for (int page = 0; page < newDoc.size(); page++) {
			name = ARRIVAL_NOTICES_FILE_PATH + newName + "_" + page + ".pdf";
			File file = new File(name);
			if(file.delete()) out("DELETED " + name);
		}
		if (searchAndMerge(ARRIVAL_NOTICES_FILE_PATH + newName + ".pdf", ID)) {
			File file = new File(ARRIVAL_NOTICES_FILE_PATH + newName + ".pdf");
			file.delete();
		}
	}
	
	public static int determineFileType(File file) {
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
				if (currentLine.toUpperCase().contains("COSCO") || currentLine.toUpperCase().contains("COSU"))	return COSCO_TYPE;
				if (currentLine.toUpperCase().contains("TURKON"))	return TURKON_TYPE;
				if (currentLine.toUpperCase().contains("MEDITERRANEAN SHIPPING COMPANY"))	return MSC_TYPE;
				if (currentLine.toUpperCase().contains("EVERGREEN")) return EVERGREEN_TYPE;
				if (currentLine.toUpperCase().contains("CMA CGM"))	return CMA_TYPE;
				if (currentLine.toUpperCase().contains("WAN HAI"))	return WAN_HAI_TYPE;
				currentLine = br.readLine();
			}
			br.close();
			textfile.delete();
		} catch (Exception ex) {
			out("Exception from determineFileType function: "+ ex.toString());
			ex.printStackTrace();
		}
		return MAERSK_TYPE;
	}
	
	/**
	 * This method merges the matching arrival notice with the shipment order
	 * by going through all the shipment order files and checking if 
	 * the shipment ID found from the arrival notice is contained in 
	 * the file name of the shipment order file
	 * @param filePath
	 * @param shipID
	 * @param type
	 * @return
	 */
	public static boolean searchAndMerge(String filePath, String shipID) {
		try {
			String fileName;
			for(int i = 0; i < ordersList.size(); i++) {
				fileName = ordersList.get(i);
				if (!fileName.contains(shipID)) continue;
				out("MERGING");
				out("ARRIVAL NOTICE >> " + filePath);
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
				
//				out("checking if pdpage can compare");
//				out(fileName +" has pages from "+file);
//				PDDocument doc = PDDocument.load(new File(fileName));
//				PDPage page1 = doc.getPage(12);
//				PDPage pagetest = doc.getPage(12);
//				if(page1.equals(pagetest)) out("test returns true");
//				else out("I guess we cannot compare two pages...");
//				PDDocument doc2 = PDDocument.load(file);
//				for(int checker = 0; checker < doc.getNumberOfPages(); checker++) {
//					for(int checkerb = 0; checkerb < doc2.getNumberOfPages(); checkerb++) {
//						out("comparing origin page " + checker + " and file page " + checkerb);
//						out("RESULT: " + (doc.getPage(checker).equals(doc2.getPage(1))));	
//					}
//				}
				
				
				// RENAME
				orderFile.renameTo(new File(fileName.replaceAll(" AN","").replace(".pdf"," AN.pdf")));
				file.delete();
				// END COMMENT -----
				
				return true;
			}
			return false;
		} catch (Exception e) {
			out("An Exception occured in searchAndMerge " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
	
	private static void populateShipIds(XSSFWorkbook workbook) throws Exception {
		Sheet sheet = workbook.getSheetAt(0);
		for(int index = 0; index < sheet.getPhysicalNumberOfRows(); index++) {
			Row row = sheet.getRow(index);
			if (row.getCell(0) == null || row.getCell(1) == null) continue;
			else {
				if(row.getCell(0).getStringCellValue().contains("EGLV"))  {
					billToShipPair.put(row.getCell(0).getStringCellValue().substring(4), row.getCell(1).toString());
				} else {
					billToShipPair.put(row.getCell(0).toString(), row.getCell(1).toString());
				}
			}
		}
	}
	
	private static List <String> retrieveAllFiles() throws Exception {
		try (Stream<Path> walk = Files.walk(Paths.get(ARRIVAL_NOTICES_FILE_PATH))) {
			return walk.filter(p -> !Files.isDirectory(p)).map(p -> p.toString()).filter(f -> f.toLowerCase().endsWith(".pdf")).filter(f2 -> !f2.matches(SHIP_ID_REGEX)).collect(Collectors.toList());
		}
	}
	
	private static List <String> retrieveOrderFiles(String path) throws Exception {
		try (Stream<Path> walk = Files.walk(Paths.get(path))) {
			return walk.filter(p -> !Files.isDirectory(p)).map(p -> p.toString()).filter(f -> f.toLowerCase().endsWith(".pdf"))
					.collect(Collectors.toList());
		}
	}
	
	public static void out(String stringToPrint) {
		System.out.println(stringToPrint);
		printLog += stringToPrint + "\n";
	}
}
