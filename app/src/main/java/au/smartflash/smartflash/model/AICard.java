package au.smartflash.smartflash.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "AICards")
public class AICard implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "username_ai")
    private String usernameAi;

    @ColumnInfo(name = "category_ai")
    private String categoryAi;

    @ColumnInfo(name = "to_language_ai")
    private String toLanguageAi;

    @ColumnInfo(name = "subcategory_ai")
    private String subcategoryAi;

    @ColumnInfo(name = "item_ai")
    private String itemAi;

    @ColumnInfo(name = "description_ai")
    private String descriptionAi;

    @ColumnInfo(name = "details_ai")
    private String detailsAi;

    @ColumnInfo(name = "image_url_ai")
    private String imageUrlAi;

    @ColumnInfo(name = "date_updated_ai")
    private String dateUpdatedAi;

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
    }

    // Getters
    public int getId() { return id; }
    public String getUsernameAi() { return usernameAi; }
    public String getCategoryAi() { return categoryAi; }
    public String getToLanguageAi() { return toLanguageAi; }
    public String getSubcategoryAi() { return subcategoryAi; }
    public String getItemAi() { return itemAi; }
    public String getDescriptionAi() { return descriptionAi; }
    public String getDetailsAi() { return detailsAi; }
    public String getImageUrlAi() { return imageUrlAi; }
    public String getDateUpdatedAi() { return dateUpdatedAi; }

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

    // Empty constructor for Room
    @Ignore
    public AICard() {}
}
