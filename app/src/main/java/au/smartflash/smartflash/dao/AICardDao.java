package au.smartflash.smartflash.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import au.smartflash.smartflash.model.AICard;

@Dao
public interface AICardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AICard aiCard);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(AICard... aiCards);

    @Query("SELECT * FROM AICards WHERE Username = :username")
    List<AICard> getCardsByUsername(String username);

    @Query("SELECT * FROM AICards WHERE Date_updated = :date")
    List<AICard> getCardsByDate(String date);

    @Query("SELECT * FROM AICards WHERE Category = :category")
    List<AICard> getCardsByCategory(String category);

    @Update
    void update(AICard aiCard);

    @Delete
    void delete(AICard aiCard);

    // Add other queries as needed
}
