
package com.aws.ec2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.amazon.sqs.javamessaging.*;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.model.CreateQueueRequest;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import com.amazonaws.services.rekognition.model.*;
import javax.jms.*;
import javax.jms.Queue;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;

@SpringBootApplication

class MyListener implements MessageListener {

    private FileWriter fileWriter;

    public MyListener(FileWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    @Override
    public void onMessage(Message message) {

        try {
            Regions clientRegion = Regions.US_EAST_1;
            String bucketName = "njit-cs-643";

            ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
            ListObjectsV2Result result;

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .build();

            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
                    .withRegion(clientRegion)
                    .build();

            result = s3Client.listObjectsV2(req);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String m = (String) ((TextMessage) message).getText().toString();

                // Handle termination signal (-1)
                if (m.equals("-1")) {
                    System.out.println("Termination signal (-1) received. Stopping processing.");
                    fileWriter.close();
                    message.acknowledge();
                    System.exit(0);
                }

                if (objectSummary.getKey().contains(m)) {
                    String photo = objectSummary.getKey();
                    // Perform text recognition on the image from the queue
                    DetectTextRequest request = new DetectTextRequest()
                            .withImage(new Image()
                                    .withS3Object(new S3Object()
                                            .withName(photo)
                                            .withBucket(bucketName)));
                    try {
                        DetectTextResult result1 = rekognitionClient.detectText(request);
                        List<TextDetection> textDetections = result1.getTextDetections();
                        if (!textDetections.isEmpty()) {
                            StringBuilder detectedText = new StringBuilder();
                            detectedText.append("Text Detected lines and words for: ").append(photo).append(" ==> ");

                            for (TextDetection text : textDetections) {
                                detectedText.append("  Text Detected: ").append(text.getDetectedText())
                                        .append(" , Confidence: ").append(text.getConfidence().toString()).append("\n");
                            }

                            System.out.println(detectedText.toString());

                            // Write the image index and recognized text to file
                            fileWriter.write("Image: " + photo + " -> " + detectedText.toString() + "\n");
                        }
                    } catch (AmazonRekognitionException e) {
                        System.out.print("Error during text recognition");
                        e.printStackTrace();
                    }
                }
            }

        } catch (JMSException | IOException e) {
            System.out.println("Please run the Instance-1 first...");
            e.printStackTrace();
        }
    }
}

public class TextRekognition {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(TextRekognition.class, args);

        Regions clientRegion = Regions.US_EAST_1;

        try {
            AmazonSQSClientBuilder.standard()
                    .withRegion(clientRegion)
                    .build();

            // Creating SQS queue even if it is not created, it will wait for instance 2 to start first.
            try {
                // Create a new connection factory with all defaults (credentials and region)
                // set automatically
                SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(),
                        AmazonSQSClientBuilder.defaultClient());

                // Create the connection.
                SQSConnection connection = connectionFactory.createConnection();
                // Get the wrapped client
                AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();

                if (!client.queueExists("as4588.fifo")) {
                    Map<String, String> attributes = new HashMap<>();
                    attributes.put("FifoQueue", "true");
                    attributes.put("ContentBasedDeduplication", "true");
                    client.createQueue(
                            new CreateQueueRequest().withQueueName("as4588.fifo").withAttributes(attributes));
                }

                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                // Create a queue identity and specify the queue name to the session
                Queue queue = session.createQueue("as4588.fifo");

                // Create a consumer for the 'queue'.
                MessageConsumer consumer = session.createConsumer(queue);

                // File writer to store recognized text
                FileWriter fileWriter = new FileWriter("recognized_text_output.txt");

                // Instantiate and set the message listener for the consumer.
                consumer.setMessageListener(new MyListener(fileWriter));

                // Start receiving incoming messages.
                connection.start();

                Thread.sleep(10000);

            } catch (Exception e) {
                System.out.println("Please run the Instance-1, the program will wait for the queue to have elements.");
                e.printStackTrace();
            }

        } catch (AmazonServiceException e) {
            System.out.println("Please run the Instance-1 first. Waiting...");
            e.printStackTrace();
        }
    }
}
