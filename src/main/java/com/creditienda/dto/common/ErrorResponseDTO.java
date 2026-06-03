package com.creditienda.dto.common;

public class ErrorResponseDTO {

    private boolean success = false;
    private ErrorDetail error;

    public ErrorResponseDTO(String type, String message, String detail) {
        this.error = new ErrorDetail(type, message, detail);
    }

    public boolean isSuccess() {
        return success;
    }

    public ErrorDetail getError() {
        return error;
    }

    public static class ErrorDetail {
        private String type;
        private String message;
        private String detail;

        public ErrorDetail(String type, String message, String detail) {
            this.type = type;
            this.message = message;
            this.detail = detail;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public String getDetail() {
            return detail;
        }
    }
}
