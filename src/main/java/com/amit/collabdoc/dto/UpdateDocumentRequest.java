package com.amit.collabdoc.dto;

public class UpdateDocumentRequest {
    private String title;

    // No-arg constructor
    public UpdateDocumentRequest(){}

    // Constructor with args
    public UpdateDocumentRequest(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
