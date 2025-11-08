package com.assessment.interest_calculator.exception;

import lombok.Getter;

@Getter
public class DigioAAException extends RuntimeException {
    private final String errorCode;
    private final String details;
    private final String version;

    public DigioAAException(String errorMsg, String errorCode, String details, String version) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.details = details;
        this.version = version;
    }

    @Override
    public String toString() {
        return String.format("DigioAAException[errorCode=%s, message=%s, details=%s, version=%s]",
            errorCode, getMessage(), details, version);
    }
}
