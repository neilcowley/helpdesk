package com.baytouch.helpdesk.beans;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import com.baytouch.helpdesk.entities.AttachData;
import com.baytouch.helpdesk.entities.AttachInfo;
import com.baytouch.helpdesk.entities.SupportCall;

@Named
@RequestScoped 
public class FileControllerBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final int MAX_FILE_SIZE = 16500000;
	
	public static Path getUploadLocation() throws IOException {
		Properties props = new Properties();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Path path = null;
		String uploadLocation = "";
		props.load(cl.getResourceAsStream("config.properties"));
		uploadLocation = Boolean.parseBoolean(props.getProperty("testMode"))==false?"uploadLocation":"uploadLocationTest";
		path = Paths.get(props.getProperty(uploadLocation));

		if (Files.notExists(path)) {
			Files.createDirectory(path);
		}

		return path;
	}
	
	public static File getAttachmentLocal(AttachInfo attachInfo) throws IOException {
		return getAttachmentLocal(attachInfo.getTmpFileName()); 
	}

	public static File getAttachmentLocal(String fileName) throws IOException {

		Path folderUpload = getUploadLocation();
		File dir = new File(folderUpload.toString());
		File f = new File(dir, fileName);

		if (!f.exists()) {
			return null;
		} else {
			return f;
		}
	}

	public void uploadAttachment(SupportCall sc, long fileSize,  String fileName, String contentType, InputStream input) {

		try {
			boolean storeInMySql = false; // Does not store files in database anymore

			if (fileSize <= MAX_FILE_SIZE){
				
				AttachInfo attachinfo = new AttachInfo();
				attachinfo.setSupportCall(sc);
				attachinfo.setFileName(fileName);
				attachinfo.setContentType(contentType);
				// String[] contentTypeSplit = contentType.split("/");
				//if (contentTypeSplit[0].equals("image")) {
				
				if(!storeInMySql){
					Path folderUpload = getUploadLocation();
					int pos = fileName.lastIndexOf(".");
					String extension = fileName.substring(pos, fileName.length());
					Path filePath = Files.createTempFile(folderUpload, fileName + "-", extension);
					Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
					attachinfo.setTmpFileName(filePath.getFileName().toString());
				} else {
					// Store in database but in a separate separate table to prevent delay when loading the support call					
					AttachData attachdata = new AttachData();
					attachdata.setAttachInfo(attachinfo);
					attachdata.setFileData( getOutputStream(input).toByteArray());
					attachinfo.setAttachdata(attachdata);
				}
				sc.getAttachments().add(attachinfo);
			} else {
				System.out.println("*** ERROR createAttachment() - MAX_FILE_SIZE limit (" + MAX_FILE_SIZE
						+ ") exceeded: " + fileSize );
			}
		} catch (Exception ex) {
			System.out.println("*** ERROR createAttachment(): " + ex.getMessage() + " ***");
			ex.printStackTrace();
		}
	}

	private ByteArrayOutputStream getOutputStream(InputStream input) throws IOException{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = input.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();
		return buffer;
	}
	
}
