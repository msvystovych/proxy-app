package org.company.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UpstreamServerException extends RuntimeException {
    private final HttpStatus status;

    public UpstreamServerException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}