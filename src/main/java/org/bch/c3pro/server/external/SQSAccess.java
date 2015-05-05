package org.bch.c3pro.server.external;

import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/**
 * Created by CH176656 on 5/1/2015.
 */
public class SQSAccess implements Queue {

    private AmazonSQS sqs = null;

    @Override
    public void sendMessage(String resource) throws C3PROException {
        setCredentials();
        this.sqs.sendMessage(new SendMessageRequest(AppConfig.getProp(AppConfig.AWS_SQS_URL), resource));
    }

    private void setCredentials() throws C3PROException {
        if (this.sqs == null) {
            AWSCredentials credentials = null;
            try {
                System.setProperty("aws.profile", AppConfig.getProp(AppConfig.AWS_SQS_PROFILE));
                credentials = new ProfileCredentialsProvider().getCredentials();
            } catch (Exception e) {
                throw new C3PROException(
                        "Cannot load the credentials from the credential profiles file. " +
                                "Please make sure that the credentials file is at the correct " +
                                "location (~/.aws/credentials), and is in valid format.",
                        e);
            }
            this.sqs = new AmazonSQSClient(credentials);
            Region usWest2 = Region.getRegion(Regions.fromName(AppConfig.getProp(AppConfig.AWS_SQS_REGION)));
            sqs.setRegion(usWest2);
        }
    }
}
