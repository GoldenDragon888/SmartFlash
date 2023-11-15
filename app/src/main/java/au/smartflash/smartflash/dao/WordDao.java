package au.smartflash.smartflash.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;

import java.util.List;

import au.smartflash.smartflash.model.Word;

@Dao
public interface WordDao {
    @Query("SELECT MAX(id) FROM Cards")
    int getMaxId();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWord(Word word);
    @Insert(onConflict = OnConflictStrategy.REPLACE) // or choose a different conflict strategy if preferred
    void insertAll(Word... words);
    @Query("SELECT COUNT(*) FROM Cards WHERE Item = :item AND Description = :description")
    int wordExists(String item, String description);
    @Query("SELECT * FROM Cards WHERE id = :wordId")
    Word getWordById(int wordId);
    @Query("SELECT * FROM Cards WHERE Category = :category AND Subcategory = :subcategory")
    LiveData<List<Word>> getWordsFromCategoryAndSubcategory(String category, String subcategory);
    @Delete
    void deleteWord(Word word);
    @Insert
    long insert(Word word);
    //@Update
    //void update(Word word);
    @Query("SELECT COUNT(*) FROM Cards WHERE Category = :category AND Subcategory = :subcategory AND Item = :item")
    int countWordsByCategorySubcategoryItem(String category, String subcategory, String item);

    @Update
    int update(Word word);
    @Query("UPDATE Cards SET Difficulty = :newDifficulty WHERE Item = :currentItem")
    int updateDifficultyByCurrentItem(String currentItem, String newDifficulty);

    @Query("DELETE FROM Cards")
    void deleteAll();
    @Update
    void updateWord(Word word);
    // Delete a word by its item name
    //@Query("DELETE FROM Cards WHERE Item = :currentItem")
    //int deleteWordByItem(String currentItem);
    @Query("DELETE FROM Cards WHERE id = :currentId")
    int deleteWordById(int currentId);
    @Query("SELECT * FROM Cards")
    LiveData<List<Word>> getAllWords();
    @Query("SELECT * FROM Cards WHERE Item = :front LIMIT 1")
    Word getWordByFront(String front);
    @Query("SELECT * FROM Cards WHERE Category = :category LIMIT 1")
    Word getFirstWordForCategory(String category);
    @Query("SELECT * FROM Cards WHERE Category = :category AND Subcategory = :subcategory")
    List<Word> getWordsByCategoryAndSubcategory(String category, String subcategory);
    @Query("SELECT * FROM Cards WHERE Difficulty = :difficulty AND Category = :category AND Subcategory = :subcategory ORDER BY RANDOM() LIMIT 1")
    Word getNextWord(String difficulty, String category, String subcategory);
    @Query("SELECT * FROM Cards WHERE Difficulty = :difficulty AND Category = :category AND Subcategory = :subcategory AND id != :excludingId ORDER BY RANDOM() LIMIT 1")
    Word getNextWordNotIncludingId(String difficulty, String category, String subcategory, int excludingId);
    @Query("SELECT DISTINCT Category FROM Cards")
    LiveData<List<String>> getUniqueCategories();
    @Query("SELECT DISTINCT Subcategory FROM Cards WHERE Category = :selectedCategory")
    LiveData<List<String>> getUniqueSubcategories(String selectedCategory);
    @Query("SELECT DISTINCT Category FROM Cards")
    List<String> getAllCategories();
    @Query("SELECT DISTINCT Subcategory FROM Cards WHERE Category = :category")
    List<String> getSubcategoriesForCategory(String category);
    @Query("SELECT DISTINCT Difficulty FROM Cards WHERE Category = :category AND Subcategory = :subcategory")
    List<String> getDifficultiesForCategoryAndSubcat(String category, String subcategory);
    @Query("SELECT * FROM Cards")
    List<Word> getAllListWords();
    @Query("UPDATE Cards SET Difficulty = :difficulty WHERE id = :wordId")
    int updateDifficulty(int wordId, String difficulty);

    @Query("SELECT DISTINCT Category FROM Cards")
    List<String> getAllDistinctCategories();
    @Query("SELECT DISTINCT Subcategory FROM Cards WHERE Category = :category")
    List<String> getSubcategoriesByCategory(String category);
    @Query("SELECT * FROM Cards WHERE Category = :category LIMIT 1")
    Word getFirstWordByCategory(String category);
    @Query("SELECT * FROM Cards WHERE Category = :category AND Subcategory = :subcategory LIMIT 1")
    Word getFirstWordByCategoryAndSubcategory(String category, String subcategory);
    @Query("SELECT * FROM Cards WHERE Category = :chosenCategory LIMIT 1")
    Word getCategoryData(String chosenCategory);
    @Query("SELECT * FROM Cards WHERE Category = :chosenCategory AND Subcategory = :chosenSubcategory LIMIT 1")
    Word getSubcategoryData(String chosenCategory, String chosenSubcategory);
    @Query("SELECT * FROM Cards WHERE Category = :category")
    List<Word> getWordsForCategory(String category);
    @Query("SELECT * FROM Cards WHERE Category = :category AND Subcategory = :subcategory")
    List<Word> getWordsForSubcategory(String category, String subcategory);
    @Query("SELECT * FROM Cards WHERE Category = :category AND Subcategory = :subcategory AND Difficulty = :difficulty")
    List<Word> getAllWordsForCategorySubcatAndDifficulty(String category, String subcategory, String difficulty);
    @Query("SELECT COUNT(*) FROM Cards WHERE Category = :category")
    int getCategoryCount(String category);
    @Query("SELECT COUNT(*) FROM Cards WHERE Category = :category AND Subcategory = :subcategory")
    int getSubcategoryCount(String category, String subcategory);
    @Query("SELECT COUNT(*) FROM Cards WHERE Category = :category AND Subcategory = :subcategory AND Difficulty = :difficulty")
    int getDifficultyCount(String category, String subcategory, String difficulty);
    @Query("SELECT * FROM Cards WHERE Category = :category AND Subcategory = :subcategory LIMIT 1")
    Word getNextWordForCategoryAndSubcat(String category, String subcategory);
    @Query("SELECT * FROM Cards WHERE Category = :category AND Subcategory = :subcategory AND Difficulty = :difficulty LIMIT 1")
    Word getNextWordForCategorySubcatAndDifficulty(String category, String subcategory, String difficulty);
    @Query("SELECT * FROM Cards WHERE Subcategory = :subcat LIMIT 1")
    Word getFirstWordForSubcategory(String subcat);
    @Query("SELECT * FROM Cards WHERE Item = :itemAi")
    Word getWordByItem(String itemAi);
    @Query("DELETE FROM Cards WHERE category = :category")
    void deleteWordsByCategory(String category);
    @Query("DELETE FROM Cards WHERE category = :category AND subcategory = :subcategory")
    void deleteWordsBySubcategory(String category, String subcategory);
}


