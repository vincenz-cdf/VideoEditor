package videoeditor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class FileStorageService {
	
	@Autowired
	private VideoEditorService videoEditorService;
	
	public String sanitizeFilename(String originalName) {
	    // Replace spaces and hashes and any other special characters you want to handle
		return originalName.toLowerCase()
	            .replace(" ", "_")
	            .replace("#", "_hash_")
	            .replaceAll("[^a-zA-Z0-9\\._-]", "");
	}


	public String storeFile(MultipartFile file) throws IOException {
	    String baseDir = new File(System.getProperty("user.dir")).getParent();
	    
	    // Sanitize the filename
	    String sanitizedFilename = sanitizeFilename(file.getOriginalFilename().replace(".mp4", ""));
	    String uploadDirectory = baseDir + File.separator + "uploads" + File.separator + sanitizedFilename;
	    String formattedName = sanitizedFilename + ".mp4";
	    
	    File uploadDir = new File(uploadDirectory);
	    if (!uploadDir.exists()) {
	        uploadDir.mkdirs();
	    }

	    File savedFile = new File(uploadDirectory, formattedName);
	    file.transferTo(savedFile);
	    
	    videoEditorService.mainProcess(uploadDirectory, formattedName);
	    
	    return uploadDirectory;
	}

}
