package au.smartflash.smartflash;

import java.util.Objects;

public class CategorySubcategoryPair {
    private final String category;
    private final String subcategory;

    public CategorySubcategoryPair(String category, String subcategory) {
        this.category = category;
        this.subcategory = subcategory;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategorySubcategoryPair that = (CategorySubcategoryPair) o;
        return Objects.equals(category, that.category) &&
                Objects.equals(subcategory, that.subcategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, subcategory);
    }
}
