package org.gooru.media.service;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gooru.media.constants.FileUploadConstants;
import org.gooru.media.constants.HttpConstants.HttpStatus;
import org.gooru.media.constants.RouteConstants;
import org.gooru.media.exception.FileUploadRuntimeException;
import org.gooru.media.infra.S3Client;
import org.gooru.media.responses.models.UploadResponse;
import org.gooru.media.utils.UploadValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class MediaUploadServiceImpl implements MediaUploadService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaUploadServiceImpl.class);

    private S3Client s3Client = S3Client.instance();

    @Override
    public UploadResponse uploadFile(RoutingContext context, String uploadLocation, long fileMaxSize) {
        UploadResponse response = new UploadResponse();
        String fileName = null;
        String entityType = context.request().getParam(RouteConstants.ENTITY_TYPE);
        String url = context.request().getParam(RouteConstants.URL);
        String contentType = FileUploadConstants.CONTENT_TYPE_DEFAULT;

        if (url != null && !url.isEmpty()) {
            response = UploadValidationUtils.validateFileUrl(url, response);
            if (response.isHasError()) {
                LOG.error("Upload by url failed");
                return response;
            } else {
                fileName = downloadAndSaveFile(url, uploadLocation, fileMaxSize);
                LOG.debug("File downloaded and saved.  Filename : " + fileName);
            }
        } else {
            Set<FileUpload> files = context.fileUploads();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Context uploaded files : " + files.size());
            }
            
            if (files.size() == 0) {
                LOG.error("No file to upload. Aborting");
                response.setHasError(true);
                response.setHttpStatus(HttpStatus.BAD_REQUEST.getCode());
                return response;
            }

            for (FileUpload f : files) {
                LOG.info("Original file name : " + f.fileName() + " Uploaded file name in file system : "
                    + f.uploadedFileName());
                fileName = renameFile(f.fileName(), f.uploadedFileName());
                contentType = f.contentType();
                LOG.debug("content type of the file: {}", contentType);
            }
        }
        if (fileName != null) {
            return s3Client.uploadFileS3(uploadLocation, entityType, fileName, response, contentType);
        }
        
        response.setHasError(true);
        response.setHttpStatus(HttpStatus.ERROR.getCode());
        LOG.error("file not uploaded, something went wrong");
        return response;
    }

    private String renameFile(String originalFileName, String uploadedFileName) {
        try {
            // Get file extension
            int index = originalFileName.lastIndexOf(FileUploadConstants.DOT);

            if (index > 0) {
                String exten = originalFileName.substring(index + 1);
                File oldFile = new File(uploadedFileName);
                File newFile = new File(uploadedFileName + FileUploadConstants.DOT + exten);
                oldFile.renameTo(newFile);
                uploadedFileName = newFile.getName();
                LOG.info("Renamed file name : " + uploadedFileName);
            } else {
            	File newFile = new File(uploadedFileName);
            	uploadedFileName = newFile.getName();
            }

        } catch (Exception e) {
            LOG.error("Rename file name failed : ", e);
        }
        return uploadedFileName;

    }

    private static String downloadAndSaveFile(String fileUrl, String uploadLocation, Long fileMaxSize) {
        try {
            LOG.debug("File url : " + fileUrl);
            String extension = StringUtils.substringAfterLast(fileUrl, FileUploadConstants.DOT);
            String fileName = UUID.randomUUID().toString() + FileUploadConstants.DOT + extension;
            File outputFile = new File(uploadLocation + fileName);
            URL url = new URL(fileUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
            conn.connect();
            FileUtils.copyInputStreamToFile(conn.getInputStream(), outputFile);
            if (outputFile.length() > fileMaxSize.intValue()) {
                outputFile.delete();
                throw new FileUploadRuntimeException("Url file upload failed, file size exceeded 5 MB ",
                    HttpStatus.BAD_REQUEST.getCode());
            }

            return fileName;
        } catch (Exception e) {
            LOG.error("DownloadImage failed:exception:", e);
            throw new FileUploadRuntimeException("Download image failed", HttpStatus.ERROR.getCode());
        }
    }

}
