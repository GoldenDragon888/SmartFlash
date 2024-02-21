package au.smartflash.smartflash;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import au.smartflash.smartflash.db.AppDatabase;
import au.smartflash.smartflash.model.Word;

import android.Manifest;

public class AdminActivity extends AppCompatActivity {
    private static final int REQUEST_STORAGE_PERMISSION = 1000;
    private static final int STORAGE_PERMISSION_CODE = 23;
    // Declare an ActivityResultLauncher for file picking
    private ActivityResultLauncher<Intent> mGetContentLauncher;

    // Check if storage permissions are granted
    public boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android is 11 (R) or above
            return Environment.isExternalStorageManager();
        } else {
            // Below Android 11
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
        }
    }
    // Request storage permissions
    private void requestForStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android is 11 (R) or above
            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            } catch (Exception e) {
                // Fallback in case the above action is not available
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        } else {
            // Below Android 11
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    },
                    STORAGE_PERMISSION_CODE
            );
        }
    }

    // Activity result callback for storage permission request
    private ActivityResultLauncher<Intent> storageActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {

                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                // Android is 11 (R) or above
                                if (Environment.isExternalStorageManager()) {
                                    // Manage External Storage Permissions Granted
                                    Log.d("FLAG", "onActivityResult: Manage External Storage Permissions Granted");
                                } else {
                                    // Storage Permissions Denied
                                    Toast.makeText(AdminActivity.this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Below Android 11
                                // Handle the result for lower Android versions if needed
                            }
                        }
                    });

    ActivityResultLauncher<Intent> mGetContent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d("FLAG", "File picked, processing the result");
                    Intent intent = result.getData();
                    if (intent != null) {
                        Uri uri = intent.getData();
                        loadCSVIntoDatabase(uri);
                        Intent mainActivityIntent = new Intent(this, MainActivity.class);
                        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(mainActivityIntent);
                    }
                }
            }
    );
    private TextView textView;
    private AtomicInteger idGenerator = new AtomicInteger(0);
    private boolean isCsvFile(Uri uri) {
        if (uri != null) {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            return fileExtension != null && fileExtension.equals("csv");
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin); // I assume 2131492892 corresponds to your layout. Use a named constant instead.

        Log.d("FLAG", "AdminActivity started");

        checkStoragePermissions();
        // In your onCreate or onViewCreated method:
        mGetContentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("FLAG", "inside mGetContentLauncher");
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            Uri uri = intent.getData();
                            // Check the file extension
                            if (isCsvFile(uri)) {
                                // Handle the selected CSV file
                                loadCSVIntoDatabase(uri);
                            } else {
                                // Show an error message or toast indicating that the selected file is not a CSV
                                Toast.makeText(this, "Please select a CSV file", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        TextView textView = findViewById(R.id.csv_file_path);

        Button select_csv_button = findViewById(R.id.select_csv_button);
        select_csv_button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
        select_csv_button.setOnClickListener(v -> {
            Log.d("FLAG", "select_csv_button onClick, about to launch file picker");
            if (Build.VERSION.SDK_INT >= 30) {
                if (Environment.isExternalStorageManager()) {
                    Log.d("FLAG", "select_csv_button onClick, to checkAndLaunchFilePicker 1");
                    checkAndLaunchFilePicker();
                } else {
                    Log.d("FLAG", "select_csv_button onClick, to requestStoragePermissionsAndLaunchFilePicker");
                    requestStoragePermissionsAndLaunchFilePicker();
                }
            } else {
                Log.d("FLAG", "select_csv_button onClick, to checkAndLaunchFilePicker 2");
                checkAndLaunchFilePicker();
            }
        });

        Button button_Export = findViewById(R.id.button_Export);
        button_Export.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));
        button_Export.setOnClickListener(v -> {
            exportDatabaseToCSV();
        });


        Button homeButton = findViewById(R.id.home_button);
        homeButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));
        homeButton.setOnClickListener(v -> {
            finish();
        });

    }
    private void checkAndLaunchFilePicker() {
        if (checkStoragePermissions()) {
            Log.d("FLAG", "checkAndLaunchFilePicker, checkStoragePermissions = true");

            launchFilePicker();
        } else {
            Log.d("FLAG", "checkAndLaunchFilePicker, to checkStoragePermissions = false");

            checkStoragePermission(); // Call the method to request permissions
        }
    }

    private void requestStoragePermissionsAndLaunchFilePicker() {
        requestForStoragePermissions();
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "This permission is required to select a file.", Toast.LENGTH_SHORT).show();
            }

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            performFileSearch();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted to import CSV.", Toast.LENGTH_SHORT).show();
                performFileSearch();
            } else {
                Toast.makeText(this, "Permission denied. You cannot select a file without this permission.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void loadCSVIntoDatabase(Uri uri) {
        Log.d("FLAG", "loadCSVIntoDatabase - Starting to import CSV");

        try {
            // Open an input stream from the content URI
            InputStream inputStream = getContentResolver().openInputStream(uri);

            if (inputStream != null) {
                AppDatabase appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "SMARTFLASHDB.sqlite")
                        .allowMainThreadQueries() // Only for debugging, remove in production
                        .build();
                ArrayList<Word> words = new ArrayList<>();
                int currentId = appDatabase.wordDao().getMaxId();
                String line;

                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                    while ((line = bufferedReader.readLine()) != null) {
                        Word newWord = parseCSVLine(line);

                        if (!isDuplicateWord(words, newWord)) {
                            currentId++;
                            newWord.setId(currentId);
                            words.add(newWord);
                        }
                    }
                } catch (IOException e) {
                    Log.e("FLAG", "Error reading CSV file", e);
                }

                ArrayList<Word> wordsToAdd = new ArrayList<>();
                for (Word word : words) {
                    if (appDatabase.wordDao().wordExists(word.getItem(), word.getDescription()) == 0) {
                        Log.d("FLAG", "Existing Word: " + word.getItem() + " - " + word.getDescription());
                        wordsToAdd.add(word);
                    }
                }

                appDatabase.wordDao().insertAll(wordsToAdd.toArray(new Word[0]));
                displayImportSuccessMessage();
            } else {
                Log.e("FLAG", "InputStream is null. Unable to open the file.");
            }
        } catch (Exception e) {
            Log.e("FLAG", "CSV Import - An error occurred", e);
        }
    }

    private Word parseCSVLine(String line) {
        String[] columns = line.split(",");
        String category = columns.length > 0 ? capitalizeWords(columns[0].trim()) : "";
        String subcategory = columns.length > 1 ? capitalizeWords(columns[1].trim()) : "";
        String item = columns.length > 2 ? capitalizeWords(columns[2].trim()) : "";
        String description = columns.length > 3 ? columns[3].trim() : "";
        String details = columns.length > 4 ? columns[4].trim() : "";
        String difficulty = columns.length > 5 && columns[5] != null ? columns[5].trim() : "Easy";

        Log.d("FLAG", "loadCSVIntoDatabase - reading csv Category: " + category);

        return new Word(0, category, subcategory, item, description, details, difficulty, null);
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder capitalizedText = new StringBuilder();
        String[] words = text.split("\\s"); // Split the string by whitespace
        for (String word : words) {
            if (!word.isEmpty()) {
                capitalizedText.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }
        return capitalizedText.toString().trim(); // Trim to remove the last extra space
    }

    private boolean isDuplicateWord(ArrayList<Word> words, Word newWord) {
        for (Word word : words) {
            if (word.getItem().equals(newWord.getItem()) &&
                    word.getDescription().equals(newWord.getDescription()) &&
                    word.getCategory().equals(newWord.getCategory()) &&
                    word.getSubcategory().equals(newWord.getSubcategory()) &&
                    word.getDetails().equals(newWord.getDetails()) &&
                    word.getDifficulty().equals(newWord.getDifficulty())) {
                return true;
            }
        }
        return false;
    }

    private void displayImportSuccessMessage() {
        runOnUiThread(() -> {
            Toast.makeText(AdminActivity.this.getApplicationContext(), "Data imported successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Log.d("FLAG", "loadCSVIntoDatabase started: Should go to MainActivity");
            startActivity(intent);
        });
    }
    // Launch the file picker with SAF
    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Specify the desired file type
        Log.d("FLAG", "launchFilePicker, before mGetContentLauncher.launch(intent); ");
        mGetContentLauncher.launch(intent);
        Log.d("FLAG", "launchFilePicker, after mGetContentLauncher.launch(intent); ");

    }
    public void performFileSearch() {
        Toast.makeText(this, "performFileSearch what's next?.", Toast.LENGTH_SHORT).show();
        Log.d("FLAG", "performFileSearch what's next?.");

        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        startActivityForResult(intent, 42);
    }
    private void exportDatabaseToCSV() {
        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "CSV_DB_Backup");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String csvFileName = "database_backup.csv";
        File csvFile = new File(folder, csvFileName);

        AppDatabase appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "SMARTFLASHDB.sqlite")
                .allowMainThreadQueries() // Only for debugging, remove in production
                .build();

        try {
            FileWriter writer = new FileWriter(csvFile);

            // Example of how to write a header line, adjust columns as per your database schema
            writer.append("Category,Subcategory,Item,Description,Details,Difficulty\n");

            // Fetch data from database and write to the file
            List<Word> words = appDatabase.wordDao().getAllWords(); // Assuming there's a method to get all words
            for (Word word : words) {
                writer.append(word.getCategory()).append(",");
                writer.append(word.getSubcategory()).append(",");
                writer.append(word.getItem()).append(",");
                writer.append(word.getDescription()).append(",");
                writer.append(word.getDetails()).append(",");
                writer.append(word.getDifficulty()).append("\n");
            }

            writer.flush();
            writer.close();
            Log.d("FLAG", "CSV Export - File written to " + csvFile.getAbsolutePath());
            Toast.makeText(this, "Successfully Exported local DB to /storage/emulated/0/CSV_DB_Backup/database_backup.csv", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("FLAG", "CSV Export - Error", e);
        }
    }

}
