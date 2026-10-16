package com.enterprise.docintel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatResponse {
    private String question;
    private String answer;
    private LocalDateTime createdAt;
}
