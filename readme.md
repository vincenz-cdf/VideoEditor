
# Spring Boot Video Processing Project

## Project Overview

This project is a Spring Boot application designed to process MP4 video files. It utilizes the Google Cloud Video Intelligence API to identify "interesting moments" within a video. Due to the API's restrictions on file size or length, the application first cuts the input MP4 file into smaller segments using `ffmpeg`. After processing each segment through the API to gather the interesting moments, the segments are recompiled back into a single MP4 file using `ffmpeg` again. The output is then returned to the user as a processed MP4 file.

## How it Works

1. **Input**: The user uploads an MP4 file to the application.
2. **Segmentation**: The application uses `ffmpeg` to cut the video into parts that comply with the Google Cloud Video Intelligence API restrictions.
3. **Processing**: Each video segment is processed by the API to detect interesting moments.
4. **Recompilation**: Once all segments have been processed, `ffmpeg` recompiles the segments back into a full video.
5. **Output**: The application outputs the processed MP4 file with the interesting moments highlighted or tagged.

## Dependencies

The project includes a variety of dependencies for testing, internal implementation, and API interaction:

```groovy
dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation 'org.junit.jupiter:junit-jupiter:5.8.1'

    // Exported dependency for consumers.
    api 'org.apache.commons:commons-math3:3.6.1'

    // Internal use dependency, not exposed to consumers.
    implementation 'com.google.guava:guava:30.1.1-jre'

    // Google Cloud Video Intelligence API
    implementation group: 'com.google.cloud', name: 'google-cloud-video-intelligence', version: '2.11.0'

    // Google API Client
    implementation group: 'com.google.api-client', name: 'google-api-client', version: '1.31.1'

    // Google Auth Library OAuth2 HTTP
    implementation group: 'com.google.auth', name: 'google-auth-library-oauth2-http', version: '0.23.0'

    // Google Protocol Buffers Java
    implementation group: 'com.google.protobuf', name: 'protobuf-java', version: '3.17.3'

    // Commons IO
    implementation group: 'commons-io', name: 'commons-io', version: '2.4'

    // Spring Boot Starter
    implementation 'org.springframework.boot:spring-boot-starter'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

## Additional Information

- The project requires JDK 11 or later.
- It's recommended to have `ffmpeg` installed on the system where the application is run.

## Setup and Installation

Instructions for setting up the application and installing necessary tools such as `ffmpeg` and configuring Google Cloud credentials will be provided in the project's README file.

---
