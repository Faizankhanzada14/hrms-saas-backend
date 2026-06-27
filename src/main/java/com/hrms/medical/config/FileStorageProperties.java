package com.hrms.medical.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "hrms.file-storage")
public class FileStorageProperties {

    /** Directory where uploaded attachments are persisted. */
    private String uploadDir = "./uploads/medical-claims";

    /** Maximum allowed attachment size in bytes (10 MB default). */
    private long maxFileSizeBytes = 10_485_760L;
}
