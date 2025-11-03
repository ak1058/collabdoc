package com.amit.collabdoc.dto;

import com.amit.collabdoc.model.enums.PermissionType;

/**
 * A small DTO to hold information about a user a document is shared with.
 * This will be sent in a list inside the DocumentResponse.
 */
public class ShareInfo {
    private String username;
    private String email;
    private PermissionType permission;

    public ShareInfo() {
    }

    public ShareInfo(String username, String email, PermissionType permission) {
        this.username = username;
        this.email = email;
        this.permission = permission;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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
