package org.company.service;

import lombok.AllArgsConstructor;
import org.company.model.RequestResponseEntity;
import org.company.repository.RequestResponseRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Service for saving request and response data.
 * <p>
 * This service interacts with the RequestResponseRepository to store request and response data.
 */
@Service
@AllArgsConstructor
public class RequestResponseService {
    private final RequestResponseRepository requestResponseRepository;


    public void saveRequestResponse(String path, Map<String, String> headers, String responseBody) {
        RequestResponseEntity entity = new RequestResponseEntity();
        entity.setUrl(path);
        entity.setRequestHeaders(headers.toString());
        entity.setResponseBody(responseBody);
        entity.setTimestamp(Instant.now());
        requestResponseRepository.save(entity);
    }
}
