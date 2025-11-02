package com.amit.collabdoc.dto;

/**
 * This DTO represents an edit sent from the client's editor.
 * UPDATED:
 * - 'delta' is the specific change (e.g., insert "h")
 * - 'fullDelta' is the *entire* document's new content, as a Quill Delta object.
 * This is what we will save to Redis and Postgres.
 */
public class EditorChange {

    private Object delta;
    private Object fullDelta; // RENAMED from 'content'

    // No Args constructor
    public EditorChange() {}

    public EditorChange(Object delta, Object fullDelta) {
        this.delta = delta;
        this.fullDelta = fullDelta;
    }

    // Getters and Setters
    public Object getDelta() {
        return delta;
    }

    public void setDelta(Object delta) {
        this.delta = delta;
    }

    public Object getFullDelta() {
        return fullDelta;
    }

    public void setFullDelta(Object fullDelta) {
        this.fullDelta = fullDelta;
    }
}

