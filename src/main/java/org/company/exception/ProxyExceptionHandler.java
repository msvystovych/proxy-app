package org.company.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProxyExceptionHandler {

    @ExceptionHandler(UpstreamClientException.class)
    public ResponseEntity<String> handleClient(UpstreamClientException ex) {
        // Set the real 4xx status (e.g., 404)
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @ExceptionHandler(UpstreamServerException.class)
    public ResponseEntity<String> handleServer(UpstreamServerException ex) {
        return ResponseEntity.status(502).body("Upstream service failed with: " + ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> fallback(Exception ex) {
        return ResponseEntity.status(500).body("Unhandled error: " + ex.getMessage());
    }
}