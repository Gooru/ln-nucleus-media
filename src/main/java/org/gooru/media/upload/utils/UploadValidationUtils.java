package org.gooru.media.upload.utils;

import org.apache.commons.lang3.StringUtils;
import static org.gooru.media.upload.constants.ErrorsConstants.*;

import org.gooru.media.upload.constants.ErrorsConstants;
import org.gooru.media.upload.constants.FileUploadConstants;
import org.gooru.media.upload.constants.HttpConstants.HttpStatus;
import org.gooru.media.upload.constants.RouteConstants;
import org.gooru.media.upload.exception.FileUploadRuntimeException;
import org.gooru.media.upload.responses.models.Error;
import org.gooru.media.upload.responses.models.UploadError;
import org.gooru.media.upload.responses.models.UploadResponse;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.slf4j.Logger;

import io.vertx.core.json.JsonArray;

public class UploadValidationUtils {

  private static void addError(String fieldName, String errorCode, String message, JsonArray errors) {
    errors.add(new Error(fieldName, errorCode, message));
  }

  public static String rejectOnError(String fieldName, String errorCode, String message) {
    JsonArray errors = new JsonArray();
    errors.add(new Error(fieldName, errorCode, message));
    return errors.toString();
  }

  public static void rejectOnS3Error(Exception e, UploadResponse response, final Logger logger) {
    JsonArray errors = new JsonArray();
    if (e instanceof S3ServiceException) {
      addError(FIELD_NA, ((ServiceException) e).getErrorCode(), e.getMessage(), errors);
      setResponse(errors, response, HttpStatus.ERROR.getCode(), ErrorsConstants.UploadErrorType.SERVER.getType());
    } else {
      logger.error("S3 upload failed " + e);
      throw new FileUploadRuntimeException(e.getMessage(), ErrorsConstants.UploadErrorType.SERVER.getType());
    }
  }

  private static void setResponse(JsonArray errors, UploadResponse response, int httpStatus, String errorType) {
    if (errors.size() > 0) {
      response.setHasError(true);
      UploadError validationError = new UploadError();
      validationError.setErrors(errors);
      response.setError(validationError);
      response.setHttpStatus(httpStatus);
    }
  }

  public static UploadResponse validateEntityType(String entityType, UploadResponse response) {
    JsonArray errors = new JsonArray();
      if (entityType == null || entityType.isEmpty()) {
        addError(RouteConstants.ENTITY_TYPE, EC_VE_400, VE_004, errors);
      }

      if (entityType != null) {
        if (!(entityType.equalsIgnoreCase(RouteConstants.UploadEntityType.CONTENT.name()) ||
          entityType.equalsIgnoreCase(RouteConstants.UploadEntityType.USER.name()))) {
          addError(RouteConstants.ENTITY_TYPE, EC_VE_400, VE_005, errors);
        }
      }
    setResponse(errors, response, HttpStatus.BAD_REQUEST.getCode(), ErrorsConstants.UploadErrorType.VALIDATION.getType());
    return response;
  }

  public static UploadResponse validateFileUrl(String url, UploadResponse response) {
    JsonArray errors = new JsonArray();
    if(!url.startsWith(HTTP)){
      addError(RouteConstants.URL, EC_VE_400, VE_002, errors);
    }

    String extension = StringUtils.substringAfterLast(url, FileUploadConstants.DOT);
    if(extension == null || extension.isEmpty() ||  !isValidImgType(extension)){
      addError(RouteConstants.URL, EC_VE_400, VE_003, errors);
    }

    setResponse(errors, response, HttpStatus.BAD_REQUEST.getCode(), ErrorsConstants.UploadErrorType.VALIDATION.getType());
    return response;
  }

  private static boolean isValidImgType(String extension){
    boolean valid = false;
    for(String type : FileUploadConstants.IMG_TYPES){
      if(extension.equalsIgnoreCase(type)){
        valid = true;
        break;
      }
    }
    return valid;
  }

}
