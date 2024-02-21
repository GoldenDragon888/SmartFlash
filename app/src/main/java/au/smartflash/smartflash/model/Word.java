package au.smartflash.smartflash.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "Cards")
public class Word implements Serializable {

    @NonNull
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "Category")
    private String category;

    @ColumnInfo(name = "Subcategory")
    private String subcategory;

    @ColumnInfo(name = "Item")
    private String item;

    @ColumnInfo(name = "Description")
    private String description;

    @ColumnInfo(name = "Details")
    private String details;

    @ColumnInfo(name = "Difficulty")
    private String difficulty;

    @ColumnInfo(name = "Image")
    private byte[] image;

    // Constructor
    public Word(int id, String category, String subcategory, String item, String description, String details, String difficulty, byte[] image) {
        this.id = id;
        this.category = category;
        this.subcategory = subcategory;
        this.item = item;
        this.description = description;
        this.details = details;
        this.difficulty = difficulty;
        this.image = image;
    }

    // Getters
    public int getId() { return id; }
    public String getCategory() { return category; }
    public String getSubcategory() { return subcategory; }
    public String getItem() { return item; }
    public String getDescription() { return description; }
    public String getDetails() { return details; }
    public String getDifficulty() { return difficulty; }
    public byte[] getImage() { return image; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setCategory(String category) { this.category = category; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    public void setItem(String item) { this.item = item; }
    public void setDescription(String description) { this.description = description; }
    public void setDetails(String details) { this.details = details; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setImage(byte[] image) { this.image = image; }

    @Override
    public String toString() {
        return "Word{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", subcategory='" + subcategory + '\'' +
                ", item='" + item + '\'' +
                ", description='" + description + '\'' +
                ", details='" + details + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", imageurl='" + image + '\'' +
                '}';
    }

    @Ignore
    public Word() {}
}
