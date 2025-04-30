package org.company.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "cached_pages")
public class CachedPage {

    @Id
    private String url;  // E.g., /spring3/

    private String modifiedHtml;

    @Indexed(expireAfter = "PT10M") // 10 minutes TTL
    private Instant createdAt;
}