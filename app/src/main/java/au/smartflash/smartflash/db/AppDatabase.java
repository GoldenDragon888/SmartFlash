package au.smartflash.smartflash.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import au.smartflash.smartflash.dao.AICardDao;
import au.smartflash.smartflash.dao.WordDao;
import au.smartflash.smartflash.model.AICard;
import au.smartflash.smartflash.model.Word;

@Database(entities = {Word.class, AICard.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "SMARTFLASHDB.sqlite"; // Just the name, no path
    private static AppDatabase instance;
    public abstract WordDao wordDao();
    public abstract AICardDao aiCardDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .createFromAsset("databases/" + DATABASE_NAME)  // This line adds the pre-populated database
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

}
