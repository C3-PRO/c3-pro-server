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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by CH176656 on 5/4/2015.
 */
public abstract class C3PROResourceProvider {
    protected Queue sqs = new SQSAccess();
    protected S3Access s3 = new S3Access();

    Logger log = LoggerFactory.getLogger(C3PROResourceProvider.class);

    protected FhirContext ctx = FhirContext.forDstu2();

    protected void sendMessage(BaseResource resource) throws InternalErrorException {
        log.info("IN sendMessage");
        IParser jsonParser = this.ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        String message = jsonParser.encodeResourceToString(resource);
        try {
            if (!AppConfig.getProp(AppConfig.SECURITY_ENCRYPTION_ENABLED).toLowerCase().equals("yes")) {
                sqs.sendMessage(message);
            } else {
                byte [] publicKeyBin = null;
                String publicKeyUUID = null;
                try {
                    publicKeyBin = this.s3.getBinary(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY));
                    publicKeyUUID = this.s3.get(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY_ID));
                } catch (C3PROException e) {
                    log.error(e.getMessage());
                    throw new InternalErrorException("Error reading public key or public key uuid from AWS S3", e);
                }
                X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBin);
                KeyFactory keyFactory = KeyFactory.getInstance(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY_BASEALG));
                PublicKey publicKey = keyFactory.generatePublic(publicSpec);
                sqs.sendMessageEncrypted(message, publicKey, publicKeyUUID);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalErrorException("Error sending message to Queue", e);
        }
    }

    protected void putResource(BaseResource resource) throws InternalErrorException {
        String key = resource.getId().getResourceType() + resource.getId().getIdPart();
        String value = this.toJSONString(resource);
        try {
            this.s3.put(key, value);
        } catch (C3PROException e) {
            throw new InternalErrorException("Error writing resource to AWS S3", e);
        }
    }

    protected BaseResource getResource(String id) {
        Class cl = getResourceClass();
        String key = cl.getSimpleName() + id;

        String value=null;
        try {
            value = this.s3.get(key);
        } catch (C3PROException e) {
            log.warn("Questionnaire id:" + id + " not found");
            return null;
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
