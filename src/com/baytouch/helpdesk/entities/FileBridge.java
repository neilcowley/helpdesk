package com.baytouch.helpdesk.entities;

/*
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.Map;
*/

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.*;

import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.hibernate.search.bridge.StringBridge;

import com.baytouch.helpdesk.beans.FileControllerBean;

public class FileBridge implements StringBridge {

	// @Override
	// public String objectToString(final Object object) {
	public String objectToString( Object object) {

		try {
			if(object==null) return "" ; 

			File file = FileControllerBean.getAttachmentLocal((String) object);		
			if(file == null || !file.exists()) return (String) object; 
			String extension = getExtension(file);
			System.out.println("#START SEARCH# - FILENAME=" + file.getName() + " EXTENSION=" + extension + " PATH="  + file.getPath());
			
			if(extension.equals("txt") || extension.equals("pdf") ){
				return readTextFile(file,null);
			}else if(extension.equals("doc") || extension.equals("docx")){ 
				return readWordFile(file,extension); 
			}else if(extension.equals("xls") || extension.equals("xlsx")){ 
				return readExcelFile(file,extension); 
			}else{
				return "";
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return ""; 
	}

	private static String readExcelFile(File file, String extension){
	    try {
	        String text = ""; 
	        if(extension.equals("xls")){
	        	// OLD Excel format - 1997-2003
	        	InputStream is = new FileInputStream(file);
	        	HSSFWorkbook wbhssf = new HSSFWorkbook(is); 
	        	ExcelExtractor extractor = new ExcelExtractor(wbhssf);
	        	text = extractor.getText();
	        	extractor.close();
	        }else{
	        	// XSSFWorkbook - org.apache.poi.xssf.usermodel.XSSFWorkbook
	        	XSSFWorkbook wbxlsx = new XSSFWorkbook(file);
	        	XSSFExcelExtractor extractor = new XSSFExcelExtractor(wbxlsx);
	        	extractor.setFormulasNotResults(true);
		        extractor.setIncludeSheetNames(false);
		        text = extractor.getText();
		        extractor.close();
	        }
	      
	        return text; 
	        
	    } catch (Exception exep){
	        exep.printStackTrace();
	    }
		return null;
	}
		
	private static String readWordFile(File file, String extension){
	 	
	    try { 
	    	String text = ""; 
	    	FileInputStream fis = new FileInputStream(file.getAbsolutePath());
	    	if(extension.equals("doc")){
	    		// OLD Word document
	    		HWPFDocument doc = new HWPFDocument(fis);
	    		WordExtractor extractor  = new WordExtractor(doc);
	    		text = extractor.getText();
	    		extractor.close();
	    	}else{
	    		// XWPFWordExtractor - New Word Document
	 	        XWPFDocument doc = new XWPFDocument(fis);
	 	        XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
	    		text = extractor.getText();
	    		extractor.close();
	    	}

	    	return text;
	        
	    } catch (Exception exep){
	        exep.printStackTrace();
	    }
		return null;
	}
	
	private static String readTextFile(File file, Charset encoding)   throws IOException {
		if(encoding==null){
			encoding = Charset.defaultCharset(); 
		}
		byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
		return new String(encoded, encoding);
	}
	
	private String getExtension(File file) {
		return file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length());
	}
		
	/*
	FileInputStream is = new FileInputStream(file);
	byte[] fileContent ;
	
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	BufferedInputStream bin = new BufferedInputStream(is);
	BufferedOutputStream bos = new BufferedOutputStream(baos);

	byte[] buffer = new byte[1 * 1024];

	int bytesRead = 0;
	while ((bytesRead = bin.read(buffer)) != -1) {
		bos.write(buffer, 0, bytesRead);
	}
	
	bin.close();
	is.close();
	bos.flush();
	fileContent = baos.toByteArray();
	System.out.println("#3 - FileContent=" + fileContent.length ) ; 
	bos.close();
	baos.close();

//	System.out.println("#4 - Object=" + new String(Base64.getDecoder().decode(fileContent)) );
	
	return new String(Base64.getDecoder().decode(fileContent));
*/
	
}
