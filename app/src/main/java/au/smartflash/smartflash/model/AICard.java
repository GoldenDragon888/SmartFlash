package au.smartflash.smartflash.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

@Entity(tableName = "AICards")
public class AICard implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private Integer id;  // Changed to Integer to allow null values

    @ColumnInfo(name = "Username")
    private String usernameAi;

    @ColumnInfo(name = "Category")
    private String categoryAi;

    @ColumnInfo(name = "To_language")
    private String toLanguageAi;

    @ColumnInfo(name = "Subcategory")
    private String subcategoryAi;

    @ColumnInfo(name = "Item")
    private String itemAi;

    @ColumnInfo(name = "Description")
    private String descriptionAi;

    @ColumnInfo(name = "Details")
    private String detailsAi;

    @ColumnInfo(name = "Image")
    private String imageUrlAi;

    @ColumnInfo(name = "Date_updated")
    private String dateUpdatedAi;

    @Ignore // This field is not part of the database table
    private boolean selected;

    // Full constructor
    public AICard(String usernameAi, String categoryAi, String toLanguageAi, String subcategoryAi, String itemAi,
                  String descriptionAi, String detailsAi, String imageUrlAi, String dateUpdatedAi) {
        this.usernameAi = usernameAi;
        this.categoryAi = categoryAi;
        this.toLanguageAi = toLanguageAi;
        this.subcategoryAi = subcategoryAi;
        this.itemAi = itemAi;
        this.descriptionAi = descriptionAi;
        this.detailsAi = detailsAi;
        this.imageUrlAi = imageUrlAi;
        this.dateUpdatedAi = dateUpdatedAi;
        this.selected = false; // Initialize with default false
    }

    // Getters
    public Integer getId() { return id; }
    @PropertyName("Username")
    public String getUsernameAi() { return usernameAi; }
    @PropertyName("Category")
    public String getCategoryAi() { return categoryAi; }

    public String getToLanguageAi() { return toLanguageAi; }
    @PropertyName("Subcategory")
    public String getSubcategoryAi() { return subcategoryAi; }
    @PropertyName("Item")
    public String getItemAi() { return itemAi; }
    @PropertyName("Description")
    public String getDescriptionAi() { return descriptionAi; }
    @PropertyName("Details")
    public String getDetailsAi() { return detailsAi; }
    @PropertyName("Image")
    public String getImageUrlAi() { return imageUrlAi; }
    @PropertyName("Date_updated")

    public String getDateUpdatedAi() { return dateUpdatedAi; }
    public boolean isSelected() { return selected; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUsernameAi(String usernameAi) { this.usernameAi = usernameAi; }
    public void setCategoryAi(String categoryAi) { this.categoryAi = categoryAi; }
    public void setToLanguageAi(String toLanguageAi) { this.toLanguageAi = toLanguageAi; }
    public void setSubcategoryAi(String subcategoryAi) { this.subcategoryAi = subcategoryAi; }
    public void setItemAi(String itemAi) { this.itemAi = itemAi; }
    public void setDescriptionAi(String descriptionAi) { this.descriptionAi = descriptionAi; }
    public void setDetailsAi(String detailsAi) { this.detailsAi = detailsAi; }
    public void setImageUrlAi(String imageUrlAi) { this.imageUrlAi = imageUrlAi; }
    public void setDateUpdatedAi(String dateUpdatedAi) { this.dateUpdatedAi = dateUpdatedAi; }
    public void setSelected(boolean selected) { this.selected = selected; }

    // Empty constructor for Room

    public AICard() {}
}
