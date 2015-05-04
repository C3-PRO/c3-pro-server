package org.bch.c3pro.server.external;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQSClient;
import org.apache.commons.io.IOUtils;
import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.*;

/**
 * Created by CH176656 on 5/4/2015.
 */
public class S3Access implements KeyValueStorage {
    private AmazonS3 s3 = null;

    @Override
    public void put(String key, String value) throws C3PROException {
        setCredentials();
        InputStream in = toInputStream(value);
        byte[] bytes = null;
        try {
            bytes = value.getBytes("UTF-8");
        } catch (Exception e) {
            // never is going to happen, but just in case....
            throw new C3PROException(e.getMessage(), e);
        }
        ObjectMetadata om = new ObjectMetadata();
        om.setContentLength(bytes.length);
        s3.putObject(new PutObjectRequest(AppConfig.getProp(AppConfig.AWS_S3_BUCKET_NAME), key, in, om));
    }

    @Override
    public String get(String key) throws C3PROException {
        setCredentials();
        S3Object s3Object = s3.getObject(new GetObjectRequest(AppConfig.getProp(AppConfig.AWS_S3_BUCKET_NAME), key));
        String out = readFromInputStream(s3Object.getObjectContent());
        return out;
    }

    private String readFromInputStream(InputStream in) throws C3PROException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        StringBuffer sb = new StringBuffer();
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new C3PROException(e.getMessage(), e);
        } finally {
            try {
                br.close();
            } catch (IOException ee) {
                throw new C3PROException(ee.getMessage(), ee);
            }
        }

        return sb.toString();
    }

    private InputStream toInputStream(String value) throws C3PROException {
        try {
            return IOUtils.toInputStream(value, "UTF-8");
        } catch (IOException e) {
            throw new C3PROException(e.getMessage(), e);
        }
    }

    private void setCredentials() throws C3PROException {
        if (this.s3 == null) {
            AWSCredentials credentials = null;
            try {
                System.setProperty("aws.profile", AppConfig.getProp(AppConfig.AWS_S3_PROFILE));
                credentials = new ProfileCredentialsProvider().getCredentials();
            } catch (Exception e) {
                throw new C3PROException(
                        "Cannot load the credentials from the credential profiles file. " +
                                "Please make sure that the credentials file is at the correct " +
                                "location (~/.aws/credentials), and is in valid format.",
                        e);
            }
            this.s3 = new AmazonS3Client(credentials);
            Region usWest2 = Region.getRegion(Regions.fromName(AppConfig.getProp(AppConfig.AWS_S3_REGION)));
            s3.setRegion(usWest2);
        }
    }
}
