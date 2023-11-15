package au.smartflash.smartflash.model;

import java.util.Objects;

public class CategorySubcategoryPair {

    private String category;
    private String subcategory;

    // Constructor, getters, and setters

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

    // toString method for logging, if needed
    @Override
    public String toString() {
        return "CategorySubcategoryPair{" +
                "category='" + category + '\'' +
                ", subcategory='" + subcategory + '\'' +
                '}';
    }
}
