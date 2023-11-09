package videoeditor.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import videoeditor.service.FileStorageService;

@RequiredArgsConstructor
@RestController
class FileUploadController {

	@Autowired
	private FileStorageService fileStorageService;

	@PostMapping("/upload")
	public ResponseEntity<?> handleFileUpload(@RequestParam("video") MultipartFile file) {
		try {
			String uploadDirectory = fileStorageService.storeFile(file);
			Map<String, String> response = new HashMap<>();
			response.put("status", "success");
			response.put("uploadDirectory", uploadDirectory);

			// subfunctions.clearAll(uploadDirectory);

			return ResponseEntity.ok(response);
		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
		}
	}

	@GetMapping("/uploads/{folder}/{filename:.+}")
	public ResponseEntity<Resource> serveFile(@PathVariable String folder, @PathVariable String filename) {
		String inputFolderPath = "/Users/vincentvann/Documents/GitHub/VideoEditor/uploads/debut_daddiction___f-zero_99_02/cutedLongVideo_Interesting/";

	    File[] filesArray = new File(inputFolderPath).listFiles();
	    if(filesArray == null) {
	        // Handle the case where no files are found, maybe throw an exception or return
	        // return; // or just return based on your use case
	    }

	    List<File> files = Arrays.asList(filesArray);

	    // Sorting the list of files by their name
	    files.sort(Comparator.comparing(File::getName));
	    
	    StringBuilder commandBuilder = new StringBuilder();
	    commandBuilder.append("ffmpeg");
	    for (File file : files) {
	        if (file.isFile()) {
	            commandBuilder.append(" -i \"");
	            commandBuilder.append(file.getAbsolutePath());
	            commandBuilder.append("\"");
	        }
	    }
	    
	    System.out.println(commandBuilder.toString());
		return null;
	}


}
