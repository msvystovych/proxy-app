package org.company.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UpstreamClientException extends RuntimeException {
    private final HttpStatus status;

    public UpstreamClientException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}