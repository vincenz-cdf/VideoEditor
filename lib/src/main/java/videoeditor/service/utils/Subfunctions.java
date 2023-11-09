package videoeditor.service.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;


@Service
public class Subfunctions {
	
	String os = System.getProperty("os.name").toLowerCase();
	boolean isWindows = os.contains("win");
	String shellCmd = isWindows ? "cmd" : "sh";
	String shellOption = isWindows ? "/c" : "-c";

	public void deassemble(String folderPath, String fileName) throws IOException {
		String inputFilePath = folderPath + File.separator + fileName;
		String outputFolderPath = folderPath + File.separator + "cutedLongVideo" + File.separator;


		// Set duration of each segment in seconds
		int segmentDuration = 20 * 60; // 20 minutes

		// Calculate number of segments
		String[] durationCommand = {shellCmd, shellOption, "ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 " + inputFilePath};
		ProcessBuilder durationProcessBuilder = new ProcessBuilder(durationCommand);
		
		Process durationProcess = durationProcessBuilder.start();
		String durationOutput = new String(durationProcess.getInputStream().readAllBytes()).trim();
		double duration = Double.parseDouble(durationOutput);
		int segmentCount = (int) Math.ceil(duration / segmentDuration);

		// Generate ffmpeg command for each segment
		for (int i = 0; i < segmentCount; i++) {
			int start = i * segmentDuration;
			int end = Math.min((i + 1) * segmentDuration, (int) duration);
			
			String[] segmentCommand = {shellCmd, shellOption, "ffmpeg -i " + inputFilePath + " -ss " + start + " -t " + (end - start) + " -c copy " + outputFolderPath + "segment_" + (i + 1) + ".mp4"};
			ProcessBuilder segmentProcessBuilder = new ProcessBuilder(segmentCommand);
			
			try {
				Process process = segmentProcessBuilder.start();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public void reassemble(String folderPath) throws IOException {
		String inputFolderPath = folderPath + File.separator + "cutedLongVideo_Interesting" + File.separator;
		String outputFile = folderPath + File.separator + "finished.mp4";
		
		System.out.println(inputFolderPath);
		System.out.println(outputFile);

	    File[] filesArray = new File(inputFolderPath).listFiles();
	    if(filesArray == null) {
	        // Handle the case where no files are found, maybe throw an exception or return
	        throw new IOException("No files found in: " + inputFolderPath);
	        // return; // or just return based on your use case
	    }

	    List<File> files = Arrays.asList(filesArray);

	    // Sorting the list of files by their name
	    files.sort(Comparator.comparing(File::getName));
	    
	    StringBuilder commandBuilder = new StringBuilder();
	    commandBuilder.append("ffmpeg");
	    for (File file : files) {
	        if (file.isFile() && isVideoFile(file)) {
	            commandBuilder.append(" -i \"");
	            commandBuilder.append(file.getAbsolutePath());
	            commandBuilder.append("\"");
	        }
	    }

	    commandBuilder.append(" -filter_complex \"");
	    for (int i = 0; i < files.size(); i++) {
	        File file = files.get(i);
	        if (isVideoFile(file)) {
	            commandBuilder.append("[" + i + ":v:0][" + i + ":a:0]");
	        }
	    }
	    commandBuilder.append("concat=n=");
	    commandBuilder.append((int) files.stream().filter(this::isVideoFile).count());
	    commandBuilder.append(":v=1:a=1[out]\" -map \"[out]\" -c:v libx264 -preset ultrafast -c:a aac -strict -2 ");
	    commandBuilder.append(outputFile);

	    System.out.println(commandBuilder.toString());
	    String[] reassembleCommand = {shellCmd, shellOption, commandBuilder.toString()};
	    ProcessBuilder segmentProcessBuilder = new ProcessBuilder(reassembleCommand);

	    try {
	        Process process = segmentProcessBuilder.start();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	private boolean isVideoFile(File file) {
		String fileName = file.getName();
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
		return extension.equalsIgnoreCase("mp4") || extension.equalsIgnoreCase("mov")
				|| extension.equalsIgnoreCase("avi");
	}

	 public void clearAll(String uploadDirectory) {
	        String folderPath = uploadDirectory;

	        File folder = new File(folderPath);

	        if (!folder.exists()) {
	            System.out.println("Folder does not exist!");
	            return;
	        }

	        if (!folder.isDirectory()) {
	            System.out.println("Path is not a directory!");
	            return;
	        }

	        File[] files = folder.listFiles();
	        if (files != null) {
	            for (File file : files) {
	                if (file.isDirectory()) {
	                    deleteFolder(file);
	                } else {
	                    file.delete();
	                }
	            }
	        }

	        System.out.println("Folder cleared!");
	    }

	    private void deleteFolder(File folder) {
	        File[] files = folder.listFiles();
	        if (files != null) {
	            for (File file : files) {
	                if (file.isDirectory()) {
	                    deleteFolder(file);
	                } else {
	                    file.delete();
	                }
	            }
	        }
	        folder.delete();
	    }
}
