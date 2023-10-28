package au.smartflash.smartflash.model;

public class CategorySubcategoryPair {
    private String category;
    private String subcategory;

    public CategorySubcategoryPair(String category, String subcategory) {
        this.category = category;
        this.subcategory = subcategory;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    @Override
    public String toString() {
        return category + " - " + subcategory;
    }
}
