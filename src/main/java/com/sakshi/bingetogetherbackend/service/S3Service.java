package com.sakshi.bingetogetherbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.s3.bucket.name:bingetogether-bucket}")
    private String bucketName;

    @Value("${aws.access.key.id}")
    private String accessKey;

    @Value("${aws.secret.access.key}")
    private String secretKey;

    @Value("${aws.s3.region:auto}")
    private String region;

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    @Value("${aws.s3.public.url:https://pub-665fdebcd34e4dfc99be0d29f193efb0.r2.dev}")
    private String publicBaseUrl;

    public Map<String, String> generatePresignedUrl(String fileName, String contentType) {
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of("auto"))
                .serviceConfiguration(serviceConfiguration)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ));

        if (endpoint != null && !endpoint.isBlank()) {
            presignerBuilder.endpointOverride(URI.create(endpoint));
        }

        try (S3Presigner presigner = presignerBuilder.build()) {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(30))
                    .putObjectRequest(objectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

            String uploadUrl = presignedRequest.url().toString();

            // Cloudflare R2 URL force karein
            String base = "https://pub-665fdebcd34e4dfc99be0d29f193efb0.r2.dev";
            if (publicBaseUrl != null && !publicBaseUrl.isBlank() && !publicBaseUrl.contains("supabase")) {
                base = publicBaseUrl;
            }

            String fileUrl = (base.endsWith("/") ? base : base + "/") + uniqueFileName;

            Map<String, String> response = new HashMap<>();
            response.put("uploadUrl", uploadUrl);
            response.put("fileUrl", fileUrl);

            return response;
        }
    }
}