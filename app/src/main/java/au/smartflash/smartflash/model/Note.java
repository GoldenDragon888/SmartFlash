package au.smartflash.smartflash.model;

import com.google.firebase.Timestamp;

import java.util.List;

public class Note {
    private String id;
    private String name;
    private String content;
    private String color;
    private Timestamp createdAt;
    private Timestamp lastModified;
    private boolean isVisible;
    private boolean isEncrypted;
    private List<String> tags;


    // Default constructor (needed for Firestore)
    public Note() {
    }

    // Parameterized constructor
    public Note(String id, String name, String content, String color,
                Timestamp createdAt, Timestamp lastModified, boolean isVisible,
                boolean isEncrypted, List<String> tags) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.color = color;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.isVisible = isVisible;
        this.isEncrypted = isEncrypted;
        this.tags = tags;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getColor() {
        return color;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getLastModified() {
        return lastModified;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }
    public List<String> getTags() {
        return tags;
    }
    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastModified(Timestamp lastModified) {
        this.lastModified = lastModified;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
