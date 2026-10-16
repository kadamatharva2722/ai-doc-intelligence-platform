package com.enterprise.docintel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class CompareRequest {
    @NotNull
    private UUID documentId1;

    @NotNull
    private UUID documentId2;
}
