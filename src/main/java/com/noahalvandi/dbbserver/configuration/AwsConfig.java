package com.noahalvandi.dbbserver.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

    private final String AWS_ACCESS_KEY;
    private final String AWS_SECRET_KEY;
    private final String AWS_BUCKET_REGION;

    public AwsConfig() {
        Dotenv dotenv = Dotenv.configure().load();
        this.AWS_ACCESS_KEY = dotenv.get("AWS_ACCESS_KEY");
        this.AWS_SECRET_KEY = dotenv.get("AWS_SECRET_KEY");
        this.AWS_BUCKET_REGION = dotenv.get("AWS_BUCKET_REGION");
    }

    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
        return AmazonS3ClientBuilder.standard()
                .withRegion(AWS_BUCKET_REGION)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }
}

