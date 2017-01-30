package org.gooru.media.infra;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.gooru.media.bootstrap.startup.Initializer;
import org.gooru.media.constants.ConfigConstants;
import org.gooru.media.constants.RouteConstants;
import org.gooru.media.responses.models.UploadResponse;
import org.gooru.media.utils.UploadValidationUtils;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public final class S3Client implements Initializer {

    private static final Logger LOG = LoggerFactory.getLogger(S3Client.class);
    private static final Logger S3_LOG = LoggerFactory.getLogger("log.s3");
    private JsonObject config;
    protected Context context;
    private RestS3Service restS3Service;
    private static final String FILE_NAME = "filename";

    @Override
    public void initializeComponent(Vertx vertx, JsonObject config) {
        synchronized (Holder.INSTANCE) {
            try {
                this.config = config.getJsonObject(ConfigConstants.S3_CONFIG);
                AWSCredentials awsCredentials = new AWSCredentials(this.config.getString(ConfigConstants.S3_ACCESS_KEY),
                    this.config.getString(ConfigConstants.S3_SECRET));
                restS3Service = new RestS3Service(awsCredentials);
            } catch (Exception e) {
                LOG.error("S3 rest service start failed ! ", e);
            }
        }
    }

    public UploadResponse uploadFileS3(String fileLocation, String entityType, String fileName,
        UploadResponse response, String contentType) {

        try {
            UploadValidationUtils.validateEntityType(entityType, response);
            if (response.isHasError()) {
                return response;
            }

            Path path = Paths.get(fileLocation + fileName);
            byte[] data = Files.readAllBytes(path);
            String bucketName = getBucketName(entityType);

            // Upload file to s3
            long start = System.currentTimeMillis();
            S3Object fileObject = new S3Object(fileName, data);
            fileObject.setContentType(contentType);
            fileObject.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            S3Object uploadedObject = restS3Service.putObject(bucketName, fileObject);

            if (uploadedObject != null) {
                LOG.debug("File uploaded to s3 succeeded :   key {} ", uploadedObject.getKey());
                LOG.debug(
                    "Elapsed time to complete upload file to s3 in service :" + (System.currentTimeMillis() - start)
                        + " ms");
                JsonObject res = new JsonObject();
                res.put(FILE_NAME, uploadedObject.getKey());
                S3_LOG.info("S3 Uploaded Id : " + uploadedObject.getKey());
                response.setResponse(res);
                // Delete temp file after the s3 upload
                boolean fileDeleted = Files.deleteIfExists(path);
                if (fileDeleted) {
                    LOG.debug("Temp file have been deleted from local file system : File name {} ", path.getFileName());
                } else {
                    LOG.error("File delete from local file system failed : File name {} ", path.getFileName());
                }
            }
        } catch (Exception e) {
            LOG.error("Upload failed : ", e);
            UploadValidationUtils.rejectOnS3Error(e, response, LOG);
            return response;
        }
        return response;
    }

    private String getBucketName(String entityType) {
        String bucketName = null;
        if (entityType.equalsIgnoreCase(RouteConstants.UploadEntityType.CONTENT.name())) {
            bucketName = this.config.getString(ConfigConstants.S3_CONTENT_BUCKET_NAME);
        } else if (entityType.equalsIgnoreCase(RouteConstants.UploadEntityType.USER.name())) {
            bucketName = this.config.getString(ConfigConstants.S3_USER_BUCKET_NAME);
        }
        LOG.debug("S3 upload bucket name {} ", bucketName);
        return bucketName;
    }

    public static S3Client instance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final S3Client INSTANCE = new S3Client();
    }
}
