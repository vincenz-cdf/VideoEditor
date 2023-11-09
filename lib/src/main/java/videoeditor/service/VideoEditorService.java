package videoeditor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.videointelligence.v1p3beta1.AnnotateVideoProgress;
import com.google.cloud.videointelligence.v1p3beta1.AnnotateVideoRequest;
import com.google.cloud.videointelligence.v1p3beta1.AnnotateVideoResponse;
import com.google.cloud.videointelligence.v1p3beta1.ExplicitContentDetectionConfig;
import com.google.cloud.videointelligence.v1p3beta1.Feature;
import com.google.cloud.videointelligence.v1p3beta1.ShotChangeDetectionConfig;
import com.google.cloud.videointelligence.v1p3beta1.VideoAnnotationResults;
import com.google.cloud.videointelligence.v1p3beta1.VideoContext;
import com.google.cloud.videointelligence.v1p3beta1.VideoIntelligenceServiceClient;
import com.google.cloud.videointelligence.v1p3beta1.VideoIntelligenceServiceSettings;
import com.google.cloud.videointelligence.v1p3beta1.VideoSegment;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

import videoeditor.service.utils.Subfunctions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class VideoEditorService {

	@Autowired
	private Subfunctions subfunctions;
	
	String os = System.getProperty("os.name").toLowerCase();
	boolean isWindows = os.contains("win");
	String shellCmd = isWindows ? "cmd" : "sh";
	String shellOption = isWindows ? "/c" : "-c";

	private void initializeFolder(String folderPath) {
		// Replace \\ with File.separator
		new File(folderPath + File.separator + "cutedLongVideo").mkdir();
		new File(folderPath + File.separator + "cutedLongVideo_Interesting").mkdir();

	}
	
	private VideoIntelligenceServiceClient authenticateWithGoogleCloud(String apiKeyPath) throws IOException {
		GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(apiKeyPath));
		VideoIntelligenceServiceSettings settings = VideoIntelligenceServiceSettings.newBuilder()
				.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
		return VideoIntelligenceServiceClient.create(settings);
	}
	
	private List<String> analyzeVideo(File file, VideoIntelligenceServiceClient client, int retryCount) throws IOException {
	    List<String> interestingMoments = new ArrayList<>();

	    String inputFilePath = file.getAbsolutePath();

	    // Set up a request to analyze the video for interesting moments
	    AnnotateVideoRequest request;
	    try (FileInputStream fis = new FileInputStream(inputFilePath)) {
	        request = AnnotateVideoRequest.newBuilder()
	                .setInputContent(ByteString.readFrom(fis))
	                .addFeatures(Feature.SHOT_CHANGE_DETECTION)
	                .setVideoContext(VideoContext.newBuilder()
	                        .setExplicitContentDetectionConfig(ExplicitContentDetectionConfig.newBuilder().build())
	                        .setShotChangeDetectionConfig(ShotChangeDetectionConfig.newBuilder().build())
	                        .build())
	                .build();
	    }

	    // Call the API asynchronously and process the response to get interesting moments
	    OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> futureResponse = client.annotateVideoAsync(request);

	    AnnotateVideoResponse response = retryApiCall(futureResponse, retryCount, file.getName());

	    if (response != null) {
	        VideoAnnotationResults results = response.getAnnotationResults(0);
	        for (VideoSegment segment : results.getShotAnnotationsList()) {
	            Duration startTime = segment.getStartTimeOffset();
	            Duration endTime = segment.getEndTimeOffset();
	            double duration = (endTime.getSeconds() - startTime.getSeconds()) 
	                            + (double)(endTime.getNanos() - startTime.getNanos()) / 1e9;
	            
	            if (duration < 10.0) {
	                String moment = startTime.getSeconds() + "-" + endTime.getSeconds();
	                interestingMoments.add(moment);
	            }
	        }
	    }

	    return interestingMoments;
	}

	private AnnotateVideoResponse retryApiCall(OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> futureResponse, int retryCount, String fileName) {
		int attempts = 0;
		while (attempts < retryCount) {
			try {
				return futureResponse.get();
			} catch (ExecutionException e) {
				System.out.println("Error for " + fileName + ", retrying...");
				attempts++;
			} catch (InterruptedException e) {
				System.err.println("Error waiting for the operation to complete: " + e.getMessage());
				break;
			}
		}
		System.out.println("Max retries reached for " + fileName);
		return null;
	}
	
	private String buildFFmpegCommand(String inputFilePath, String outputFilePath, List<String> interestingMoments) {
	    StringBuilder commandBuilder = new StringBuilder();

	    commandBuilder.append("ffmpeg -i ");
	    commandBuilder.append(inputFilePath);
	    commandBuilder.append(" -filter_complex \"");
	    
	    System.out.println("entering ffmepg");

	    for (int i = 0; i < interestingMoments.size(); i++) {
	        String[] time = interestingMoments.get(i).split("-");
	        commandBuilder.append("[0:v]trim=");
	        commandBuilder.append(time[0]);
	        commandBuilder.append(":");
	        commandBuilder.append(time[1]);
	        commandBuilder.append(",setpts=PTS-STARTPTS[v");
	        commandBuilder.append(i);
	        commandBuilder.append("];[0:a]atrim=");
	        commandBuilder.append(time[0]);
	        commandBuilder.append(":");
	        commandBuilder.append(time[1]);
	        commandBuilder.append(",asetpts=PTS-STARTPTS[a");
	        commandBuilder.append(i);
	        commandBuilder.append("];");
	    }

	    if (interestingMoments.size() > 0) {
	        commandBuilder.append("[v0][a0]");
	    }

	    for (int i = 1; i < interestingMoments.size(); i++) {
	        commandBuilder.append("[v");
	        commandBuilder.append(i);
	        commandBuilder.append("][a");
	        commandBuilder.append(i);
	        commandBuilder.append("]");
	    }

	    commandBuilder.append("concat=n=");
	    commandBuilder.append(interestingMoments.size());
	    commandBuilder.append(":v=1:a=1[v][a]\" -map \"[v]\" -map \"[a]\" -c:v libx264 -c:a aac ");
	    commandBuilder.append(outputFilePath);

	    return commandBuilder.toString();
	}

	public void mainProcess(String folderPath, String fileName) throws FileNotFoundException, IOException {
		initializeFolder(folderPath);

		// Deassemble
		try {
			subfunctions.deassemble(folderPath, fileName);
		} catch (IOException e) {
			System.out.println(e);
		}
		System.out.println("Deassemble done");

		String apiKeyPath = System.getProperty("user.dir") + "/src/main/resources/woven-respect-379814-cd7c85aa3113.json";

		try (VideoIntelligenceServiceClient client = authenticateWithGoogleCloud(apiKeyPath)) {
			// Process files
			File inputFolder = new File(folderPath + File.separator + "cutedLongVideo");

			File[] files = inputFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp4"));
			
			for (File file : files) {
		        String inputFilePath = file.getAbsolutePath();
		        String outputFilePath = folderPath + File.separator + "cutedLongVideo_Interesting" + File.separator + file.getName();
		        System.out.println("processing " + inputFilePath);
				List<String> interestingMoments = analyzeVideo(file, client, 3);

				// Build and execute FFmpeg command
				String[] finalCommand = {shellCmd, shellOption, buildFFmpegCommand(inputFilePath, outputFilePath, interestingMoments)};
				ProcessBuilder finalProcessBuilder = new ProcessBuilder(finalCommand);
				
				try {
					Process process = finalProcessBuilder.start();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

		// Reassemble
		try {
			subfunctions.reassemble(folderPath);
		} catch (IOException e) {
			System.out.println(e);
		}
		System.out.println("Reassemble done");
	}
	
}
