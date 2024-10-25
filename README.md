# AWS SQS Image and Text Recognition Pipeline

This project demonstrates the use of AWS cloud services to implement a distributed image and text recognition pipeline using two Amazon EC2 instances. One instance performs image recognition (detecting cars in images), while the second instance extracts text from those images. AWS services like S3, Rekognition, and SQS are utilized to achieve communication between the instances and perform recognition tasks.

## Project Overview

- **EC2 Instances**: 
  - **EC1-Image**: Detects cars in images from an S3 bucket using AWS Rekognition and sends the results to an SQS Queue.
  - **EC2-Text**: Listens to the SQS Queue for the processed images and performs text recognition on those images.
  
- **AWS Services**:
  - **S3**: Stores the images to be processed.
  - **Rekognition**: Used for both object (car) and text recognition.
  - **SQS**: Acts as a message queue to facilitate communication between the two EC2 instances.

---

## Setup and Configuration

### Prerequisites

- AWS Account (Student Account through Vocareum)
- Basic understanding of AWS EC2, SQS, and S3.
- Knowledge of how to use SSH for connecting to EC2 instances.

### Initial AWS Setup

#### 1. AWS Credentials Setup
- **Create AWS Account**: Use your NJIT email to sign up for an AWS account.
- **Set Up IAM Role**:
  - Go to AWS Management Console → IAM → Access Management → Policies → Create Policy.
  - Give full access to the following services:
    - Rekognition
    - S3
    - SQS
- **Create IAM Role for Lab**: This role will be used to allow the EC2 instances to communicate with AWS services.

#### 2. Creating EC2 Instances

Create two EC2 instances for running the image and text recognition components.

1. **Login to AWS**: Login using your AWS student account via the Vocareum lab and navigate to the AWS Management Console.
2. **Navigate to EC2**: In the Services menu, select EC2 and launch two EC2 instances.
3. **Instance Settings**:
    - **AMI**: Amazon Linux 2 AMI (HVM) - Kernel 5.10, SSD Volume Type.
    - **Instance Type**: t2.micro (eligible for free tier).
    - **Security Group**: Allow SSH (port 22), HTTP (port 80), and HTTPS (port 443) from "My IP."
    - **IAM Role**: Assign the `LabInstanceRole` or `EMR_EC2_DefaultRole` to the instance.

4. **Create Key Pair**: Create a key pair to access the instances via SSH and download the `.pem` file.

#### 3. Accessing EC2 Instances
- **Connect via SSH**:
  - On MacOS, use the terminal to connect:
    ```bash
    ssh -i "your-key-pair.pem" ec2-user@<your-instance-public-ip>
    ```
  - On Windows, use Putty or WinSCP with the `.ppk` key to connect.
  
- **Upload JAR Files**:
  - Use **Cyberduck** (on MacOS) or **WinSCP** (on Windows) to transfer the JAR files to the respective EC2 instances.

---

## Installation

1. **Install Java on EC2 Instances**:
   Run the following commands to install Java on each instance:
   ```bash
   sudo yum update
   sudo yum install java-1.8.0-openjdk -y
   sudo amazon-linux-extras install java-openjdk11 -y
   ```
   Verify the installation:
   ```bash
   java -version
   ```

2. **Set Up AWS Credentials**:
   Use the AWS credentials provided in Vocareum to set up AWS credentials on both EC2 instances:
   ```bash
   cd ~/.aws
   touch ~/.aws/credentials
   vi ~/.aws/credentials
   ```
   Paste the credentials and save the file by pressing `ESC` and typing `:wq`.

---

## Running the Application

### Step 1: Running Image Recognition (abi_image_rekognition)
The first EC2 instance will run the `ImageRekognition` JAR file, which detects cars in images and sends the results to an SQS queue.

1. Connect to the **abi_image_rekognition** instance via SSH.
2. Verify that the JAR file is uploaded:
   ```bash
   ls
   ```
3. Run the `image-rekognition` JAR file:
   ```bash
   java -jar image-rekognition-1.0-SNAPSHOT.jar
   ```

### Step 2: Running Text Recognition (abi_text_rekognition)
The second EC2 instance will listen to the SQS queue and perform text recognition on the images.

1. Connect to the **abi_text_rekognition** instance via SSH.
2. Verify the JAR file:
   ```bash
   ls
   ```
3. Run the `text-rekognition` JAR file:
   ```bash
   java -jar text-rekognition-1.0-SNAPSHOT.jar
   ```

---

## Project Workflow

### Image Recognition (EC1-Image)
- Fetches images from the public S3 bucket (`https://njit-cs-643.s3.us-east-1.amazonaws.com`).
- Detects cars in the images using Amazon Rekognition.
- Pushes the image and its recognition result to the SQS queue (`as4588.fifo`).

### Text Recognition (EC2-Text)
- Listens to the SQS queue for new image processing tasks.
- Fetches the image from the S3 bucket based on the message received from the SQS queue.
- Performs text recognition using Amazon Rekognition and prints the detected text.

### SQS Queue
The SQS Queue serves as the communication bridge between the two EC2 instances. It ensures that once an image is processed by `EC1-Image`, the `EC2-Text` instance is notified to perform text recognition.

---

## Screenshots and Detailed Execution Workflow
Detailed instructions and screenshots of the AWS SQS pipeline execution can be found in the `Cloud_Project1_AravindKalyanSivakumar.pdf` in this repository.

---

## Conclusion
This project showcases how cloud services like EC2, S3, Rekognition, and SQS can be used to create a distributed system that processes images and extracts text in a scalable and efficient manner. The use of SQS as a communication mechanism ensures seamless interaction between the two EC2 instances, facilitating asynchronous processing.

---
