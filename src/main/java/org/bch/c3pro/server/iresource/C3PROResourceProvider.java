package org.bch.c3pro.server.iresource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.parser.IParser;
import com.amazonaws.services.opsworks.model.App;
import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;
import org.bch.c3pro.server.external.Queue;
import org.bch.c3pro.server.external.S3Access;
import org.bch.c3pro.server.external.SQSAccess;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

/**
 * Created by CH176656 on 5/4/2015.
 */
public abstract class C3PROResourceProvider {
    private Queue sqs = new SQSAccess();
    private S3Access s3 = new S3Access();
    Logger log = Logger.getAnonymousLogger();
    protected FhirContext ctx = FhirContext.forDstu2();

    protected void sendMessage(BaseResource resource) throws InternalErrorException {
        log.info("IN SEND MESSAGE");
        IParser jsonParser = this.ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        String message = jsonParser.encodeResourceToString(resource);
        try {
            if (!AppConfig.getProp(AppConfig.SECURITY_ENCRYPTION_ENABLED).toLowerCase().equals("yes")) {
                sqs.sendMessage(message);
            } else {
                byte [] publicKeyBin = null;
                try {
                    log.info("BEFORE READING PUBLICKEY");
                    publicKeyBin = this.s3.getBinary(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY));
                } catch (C3PROException e) {
                    log.info(e.getMessage());
                    new InternalErrorException("Error reading public key from AWS S3", e);
                }
                log.info("GRABING publickey");
                X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBin);
                log.info("GRABING publickey2");
                KeyFactory keyFactory = KeyFactory.getInstance(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY_BASEALG));
                log.info("GRABING publickey3");
                PublicKey publicKey = keyFactory.generatePublic(publicSpec);
                log.info("GRABING publickey4");
                sqs.sendMessageEncrypted(message, publicKey);
                log.info("GRABING publickey5");
            }
        } catch (Exception e) {
            log.info("EXCEPTION" + e.getMessage());
            new InternalErrorException("Error sending message to Queue", e);
        }
    }

    protected void putResource(BaseResource resource) throws InternalErrorException {
        String key = resource.getId().getResourceType() + resource.getId().getIdPart();
        String value = this.toJSONString(resource);
        try {
            this.s3.put(key, value);
        } catch (C3PROException e) {
            new InternalErrorException("Error writing resource to AWS S3", e);
        }
    }

    protected BaseResource getResource(String id) {
        Class cl = getResourceClass();
        String key = cl.getSimpleName() + id;

        String value=null;
        try {
            value = this.s3.get(key);
        } catch (C3PROException e) {
            new InternalErrorException("Error reading resource from AWS S3", e);
        }
        IParser parser = ctx.newJsonParser();
        BaseResource baseResource = parser.parseResource(getResourceClass(), value);
        return baseResource;
    }

    protected abstract String generateNewId();
    protected abstract Class<BaseResource> getResourceClass();

    private String toJSONString(BaseResource resource) {
        IParser jsonParser = this.ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        return jsonParser.encodeResourceToString(resource);
    }
}
