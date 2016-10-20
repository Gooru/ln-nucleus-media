package org.gooru.media.constants;

public final class ErrorsConstants {

    public static final String VE_000 = "Request body should not be empty, pass valid json";

    public static final String VE_001 = "Sessiontoken cannot be null";

    public static final String VE_002 = "Invalid URL passed";

    public static final String VE_003 =
        "Only image files should be uploaded by url, url also should have file name with appropriate extension(i.e png,gif,jpg)";

    public static final String VE_004 = "Entity type cannot be null";

    public static final String VE_005 = "Invalid entity type";

    public static final String VE_006 = "File size should not be more than 5MB, upload file less than 5MB";
    
    public static final String VE_007 = "No file provided to upload";

    public static final String MESSAGE = "message";

    public static final String HTTP = "http";

    private ErrorsConstants() {
    }

    public enum UploadErrorType {

        VALIDATION("validation"),
        SERVER("server");

        private final String type;

        UploadErrorType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

}
