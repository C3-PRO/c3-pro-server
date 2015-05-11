package org.bch.c3pro.server.external;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.apache.commons.codec.binary.Base64;
import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.logging.Logger;

/**
 * Created by CH176656 on 5/1/2015.
 */
public class SQSAccess implements Queue {

    private AmazonSQS sqs = null;
    Logger log = Logger.getAnonymousLogger();

    @Override
    public void sendMessage(String resource) throws C3PROException {
        setCredentials();
        this.sqs.sendMessage(new SendMessageRequest(AppConfig.getProp(AppConfig.AWS_SQS_URL), resource));
    }

    @Override
    public void sendMessageEncrypted(String resource, PublicKey publicKey) throws C3PROException {

        setCredentials();

        // Generate the symetric private key to encrypt the message
        SecretKey symetricKey = generateSecretKey();

        byte []encKeyToSend = null;
        byte []encResource = null;
        Cipher cipher;
        String encMessageStr = null;
        String encKeyToSendStr = null;
        try {
            // We encrypt the symetric key using the public available key
            int size = Integer.parseInt(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_SIZE));
            //SecureRandom random = new SecureRandom();
            //IvParameterSpec iv = new IvParameterSpec(random.generateSeed(16));
            encKeyToSend = encryptRSA(publicKey, symetricKey.getEncoded());

            // We encrypt the message
            cipher = Cipher.getInstance(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_ALG));
            //cipher.init(Cipher.ENCRYPT_MODE, symetricKey, iv);
            cipher.init(Cipher.ENCRYPT_MODE, symetricKey, new IvParameterSpec(new byte[size]));
            encResource = cipher.doFinal(resource.getBytes(AppConfig.UTF));
        } catch (UnsupportedEncodingException e) {
            throw new C3PROException(e.getMessage(), e);
        } catch (InvalidKeyException e) {
                throw new C3PROException(e.getMessage(), e);
        } catch (Exception e) {
            throw new C3PROException(e.getMessage(), e);
        }

        // We send the encrypted message to the Queue. We Base64 encode it
        SendMessageRequest mse = new SendMessageRequest(
                AppConfig.getProp(AppConfig.AWS_SQS_URL),
                Base64.encodeBase64String(encResource));

        MessageAttributeValue atr = new MessageAttributeValue();
        atr.setStringValue(Base64.encodeBase64String(encKeyToSend));
        atr.setDataType("String");
        mse.addMessageAttributesEntry(AppConfig.getProp(AppConfig.SECURITY_METADATAKEY), atr);
        try {
            this.sqs.sendMessage(mse);
        } catch (Exception e) {
            throw new C3PROException(e.getMessage(), e);
        }

    }

    public SecretKey generateSecretKey() throws C3PROException {
        SecretKey key = null;

        try {
            KeyGenerator generator = KeyGenerator.getInstance(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_BASEALG));
            int size = Integer.parseInt(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_SIZE));
            SecureRandom random = new SecureRandom();
            generator.init(size*8, random);
            key = generator.generateKey();
        } catch (Exception e) {
            throw new C3PROException(e.getMessage(), e);
        }
        return key;
    }

    public byte[] encryptRSA(PublicKey key, byte[] text) throws C3PROException {
        Cipher cipher = null;
        byte [] out = null;
        try {
            cipher = Cipher.getInstance(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY_ALG));
            cipher.init(Cipher.ENCRYPT_MODE, key);
            out = cipher.doFinal(text);
        } catch (Exception e) {
            throw new C3PROException(e.getMessage(), e);
        }
        return out;
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
