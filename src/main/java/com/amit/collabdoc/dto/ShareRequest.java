package com.amit.collabdoc.dto;

import com.amit.collabdoc.model.enums.PermissionType;



/**
 * DTO for a request to share a document.
 */
public class ShareRequest {
   
    private String email;

    private PermissionType permission;

    // Default constructor
    public ShareRequest() {
    }

    // Constructor with fields
    public ShareRequest(String email, PermissionType permissionType) {
        this.email = email;
        this.permission = permissionType;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public PermissionType getPermission() {
        return permission;
    }

    public void setPermission(PermissionType permission) {
        this.permission = permission;
    }
}
