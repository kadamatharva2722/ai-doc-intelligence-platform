package com.enterprise.docintel.common.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {

    @NotBlank(message = "Question cannot be blank")
    private String question;

    public ChatRequest() {
    }

    public ChatRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public static ChatRequestBuilder builder() {
        return new ChatRequestBuilder();
    }

    public static class ChatRequestBuilder {
        private String question;

        ChatRequestBuilder() {
        }

        public ChatRequestBuilder question(String question) {
            this.question = question;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(this.question);
        }
    }
}
