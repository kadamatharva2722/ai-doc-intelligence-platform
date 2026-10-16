package com.enterprise.docintel.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID id;
    private String name;
    private String fileType;
    private String status;
    private String ownerUsername;
    private LocalDateTime createdAt;
}
