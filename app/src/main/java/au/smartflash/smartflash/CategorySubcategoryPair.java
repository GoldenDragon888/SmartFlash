package au.smartflash.smartflash;

public class CategorySubcategoryPair {
    private final String category;

    private final String subcategory;

    public CategorySubcategoryPair(String paramString1, String paramString2) {
        this.category = paramString1;
        this.subcategory = paramString2;
    }

    public String getCategory() {
        return this.category;
    }

    public String getSubcategory() {
        return this.subcategory;
    }

    public String toString() {
        return this.category + " - " + this.subcategory;
    }
}
