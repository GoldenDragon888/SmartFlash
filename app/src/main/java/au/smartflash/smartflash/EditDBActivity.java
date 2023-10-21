package au.smartflash.smartflash;

import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import au.smartflash.smartflash.dao.WordDao;
import au.smartflash.smartflash.db.AppDatabase;
import au.smartflash.smartflash.model.Word;

public class EditDBActivity extends AppCompatActivity {
    private static final int OVERLAY_PERMISSION_REQ_CODE = 9101;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1234;
    private static final int REQUEST_RECORD_AUDIO = 5678;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 200;
    private Button buttonBack, buttonHome, buttonNext;
    List<String> categoriesList = new ArrayList<String>();
    private ArrayAdapter<String> categoryAdapter;
    private Word currentWord;
    private AppDatabase db;
    private File externalDir;
    private ImageView imageView;
    private String selectedCategory = null;
    private Uri selectedImageUri;
    private String selectedSubcategory = null;
    private Spinner spinnerCategory;
    private Spinner spinnerSubcategory;
    List<String> subcatList = new ArrayList<String>();
    private ArrayAdapter<String> subcategoryAdapter;
    private List<Word> words;
    private String category;
    private String subcategory;
    private EditText editTextCategory, editTextSubcat, editTextItem, editTextDifficulty;
    private Button buttonCategory, buttonSubcat, buttonDelete, buttonNew, buttonUpdate;
    private WordDao wordDao;
    private List<Word> currentWordsList;  // Assuming the data is represented by the "Word" class.
    private int currentPosition = 0;
    private CardView descriptionCardView;
    private CardView editDescriptionOverlay;
    private TextView  editDescriptionContent;
    private Button  saveDescriptionEditButton;
    private FrameLayout frameOverlay;
    private FrameLayout frameOverlayDetails;
    private TextView editTextDetails, editTextDescription, editDetailsContent;
    private Button saveDetailsEditButton;
    private int previousPosition = 1; // Initializing it to 1 as a default value, so it defaults to "Easy" if nothing else is selected.
    private Spinner spinnerDifficulty;
    private ImageView associatedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_db);

        if (savedInstanceState != null) {
            currentWord = (Word) savedInstanceState.getSerializable("CURRENT_WORD");
        }

        Log.d("FLAG", "IN EditDBActivity before Room");
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "SMARTFLASHDB.sqlite").build();
        wordDao = db.wordDao();

        initializeDirectories();
        initializeUIElements();
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("FLAG", "onResume: currentWord = " + currentWord);
    }

    private void initializeUIElements() {
        // ... Initialize your UI elements ...

        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);

        editTextCategory = findViewById(R.id.editTextCategory);  // Make sure the ID matches the one in your XML
        editTextSubcat = findViewById(R.id.editTextSubcat);
        editTextItem = findViewById(R.id.editTextItem);

        // Initialize the buttons
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonNew = findViewById(R.id.buttonNew);
        buttonUpdate = findViewById(R.id.buttonUpdate);

        // Set the click listeners
        buttonDelete.setOnClickListener(view -> deleteCurrentWord());
        buttonNew.setOnClickListener(view -> createNewWord());
        buttonUpdate.setOnClickListener(view -> updateCurrentWord());

        frameOverlay = findViewById(R.id.frameOverlay);
        editDescriptionContent = findViewById(R.id.editDescriptionContent);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextDescription.setFocusableInTouchMode(false);
        saveDescriptionEditButton = findViewById(R.id.saveDescriptionEditButton);

        editTextDescription.setOnClickListener(view -> {
            String currentText = editTextDescription.getText().toString();
            if ("Description".equals(currentText)) {
                editDescriptionContent.setText("");  // Clear the overlay input
            } else {
                editDescriptionContent.setText(currentText);
            }
            frameOverlay.setVisibility(View.VISIBLE);
        });

        saveDescriptionEditButton.setOnClickListener(view -> {
            String newContent = editDescriptionContent.getText().toString();
            if (TextUtils.isEmpty(newContent)) {
                editTextDescription.setText("Description");  // Reset to hint if empty
            } else {
                editTextDescription.setText(newContent);
            }
            frameOverlay.setVisibility(View.GONE);
        });

        // Initialize the new UI components for the Details overlay
        frameOverlayDetails = findViewById(R.id.frameOverlayDetails);
        editDetailsContent = findViewById(R.id.editDetailsContent);
        editTextDetails = findViewById(R.id.editTextDetails);
        saveDetailsEditButton = findViewById(R.id.saveDetailsEditButton);

        editTextDetails.setFocusableInTouchMode(false);

        // Set listeners for Details EditText and Save button
        editTextDetails.setOnClickListener(view -> {
            Log.d("FLAG", "OverlayDebug - Details text clicked");
            String currentText = editTextDetails.getText().toString();
            if ("Details".equals(currentText)) {
                editDetailsContent.setText("");  // Clear the overlay input
            } else {
                editDetailsContent.setText(currentText);
            }
            frameOverlayDetails.setVisibility(View.VISIBLE);
        });

        saveDetailsEditButton.setOnClickListener(view -> {
            Log.d("FLAG", "OverlayDebug - Save button clicked for Details");
            String newContent = editDetailsContent.getText().toString();
            if (TextUtils.isEmpty(newContent)) {
                editTextDetails.setText("Details");  // Reset to hint if empty
            } else {
                editTextDetails.setText(newContent);
            }
            frameOverlayDetails.setVisibility(View.GONE);
        });

        Spinner spinnerDifficulty = findViewById(R.id.spinnerDifficulty);

        String[] difficulties = getResources().getStringArray(R.array.difficulties);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.custom_spinner_item, difficulties) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                if (position == 0) {
                    view.setTypeface(null, Typeface.BOLD);  // Make the "Difficulty" item bold
                } else {
                    view.setTypeface(null, Typeface.NORMAL);  // Make other items normal
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                if (position == 0) {
                    view.setTypeface(null, Typeface.BOLD);  // Make the "Difficulty" item bold in the dropdown as well
                } else {
                    view.setTypeface(null, Typeface.NORMAL);  // Make other items normal in the dropdown
                }
                return view;
            }
            @Override
            public boolean isEnabled(int position) {
                return position != 0;  // Make the first item ("Difficulty") non-selectable
            }
        };
        spinnerDifficulty.setAdapter(adapter);

        //spinnerDifficulty.setSelection(-1, false);
        //spinnerDifficulty.setPrompt("Difficulty");

        buttonNext = findViewById(R.id.buttonNext);
        buttonBack = findViewById(R.id.buttonBack);
        buttonHome = findViewById(R.id.buttonHome);

        buttonCategory = findViewById(R.id.buttonCategory);
        buttonSubcat = findViewById(R.id.buttonSubcat);

        imageView = findViewById(R.id.associated_image);
        associatedImage = findViewById(R.id.associated_image);


        Button homeButton = findViewById(R.id.buttonHome);
        homeButton.setOnClickListener(v -> finish());

        buttonCategory.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(EditDBActivity.this);
            builder.setTitle("Select Category");

            getAllCategories(categories -> {
                builder.setItems(categories, (dialog, which) -> {
                    String selectedCategory = categories[which];

                    // Set the button's text to reflect the chosen category
                    buttonCategory.setText(selectedCategory);

                    editTextCategory.setText(selectedCategory);
                    populateCategoryData(selectedCategory);  // Populate the data after selecting the category

                    // Automatically set the first subcategory for the chosen category
                    getSubcategoriesForCategory(selectedCategory, subcategories -> {
                        if (subcategories != null && subcategories.length > 0) {
                            String firstSubcategory = subcategories[0];
                            editTextSubcat.setText(firstSubcategory);

                            // Also update the subcategory button text to show the first subcategory
                            buttonSubcat.setText(firstSubcategory);

                            // No need to call populateSubcategoryData() here since we've already set the first subcategory in populateCategoryData
                        }
                    });
                });
                builder.show();
            });
        });
        buttonSubcat.setOnClickListener(view -> {
            String selectedCategory = editTextCategory.getText().toString();
            if (selectedCategory.isEmpty()) {
                Toast.makeText(EditDBActivity.this, "Please select a category first!", Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(EditDBActivity.this);
            builder.setTitle("Select Subcategory");

            getSubcategoriesForCategory(selectedCategory, subcategories -> {
                builder.setItems(subcategories, (dialog, which) -> {
                    String selectedSubcategory = subcategories[which];

                    // Set the button's text to reflect the chosen subcategory
                    buttonSubcat.setText(selectedSubcategory);

                    editTextSubcat.setText(selectedSubcategory);
                    populateSubcategoryData(selectedCategory, selectedSubcategory);  // Populate the data after selecting the subcategory
                });
                builder.show();
            });
        });
        buttonNext.setOnClickListener(view -> {
            if (currentWordsList != null && currentPosition < currentWordsList.size() - 1) {
                currentPosition++;
                Log.d("FLAG", "buttonNext record position: " + currentPosition + " of size: " + currentWordsList.size());
                displayWord(currentWordsList.get(currentPosition));

            } else {
                Log.d("FLAG", "buttonNext Current word list is empty: ");
                Toast.makeText(EditDBActivity.this, "You've reached the end of the list.", Toast.LENGTH_SHORT).show();
            }
        });
        buttonBack.setOnClickListener(view -> {
            if (currentWordsList != null && currentPosition > 0) {
                currentPosition--;
                displayWord(currentWordsList.get(currentPosition));
            } else {
                Toast.makeText(EditDBActivity.this, "You're at the start of the list.", Toast.LENGTH_SHORT).show();
            }
        });

        associatedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImageFromGallery();
            }
        });

    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("CURRENT_WORD", currentWord); // Assuming Word class implements Serializable
    }

    private static final int IMAGE_PICK_REQUEST = 1001;
    private File generateImagePath(Word currentWord) {
        String category = currentWord.getCategory() != null ? currentWord.getCategory() : "unknown";
        String subcategory = currentWord.getSubcategory() != null ? currentWord.getSubcategory() : "unknown";

        return new File(getExternalFilesDir(null), "Images/" + category + "/" + subcategory + "/" + currentWord.getItem() + ".png");
    }

    private void pickImageFromGallery() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageIntent.setType("image/*");
        startActivityForResult(pickImageIntent, IMAGE_PICK_REQUEST);
    }

    private void deleteCurrentWord() {
        if (currentWordsList != null && !currentWordsList.isEmpty()) {
            Word currentWord = currentWordsList.get(currentPosition);
            executor.execute(() -> {
                wordDao.deleteWord(currentWord);
                runOnUiThread(() -> {
                    // Optionally update your UI after deletion
                    Toast.makeText(this, "Word deleted successfully", Toast.LENGTH_SHORT).show();
                    // TODO: Refresh your data/view after deleting.
                });
            });
        } else {
            Toast.makeText(this, "No word to delete", Toast.LENGTH_SHORT).show();
        }
    }
    private void createNewWord() {
        // Read from the EditText fields to create a new Word object
        Word newWord = new Word();
        newWord.setCategory(editTextCategory.getText().toString());
        newWord.setSubcategory(editTextSubcat.getText().toString());
        newWord.setItem(editTextItem.getText().toString());
        newWord.setDescription(editTextDescription.getText().toString());
        newWord.setDetails(editTextDetails.getText().toString());
        newWord.setDifficulty(spinnerDifficulty.getSelectedItem().toString());
        // TODO: Set other fields...

        executor.execute(() -> {
            // Insert the new Word into the database
            wordDao.insert(newWord);

            runOnUiThread(() -> {
                Toast.makeText(this, "New word added successfully", Toast.LENGTH_LONG).show();

                // TODO: Clear other fields...
            });
        });
    }
    private void updateCurrentWord() {
        if (currentWordsList != null && !currentWordsList.isEmpty()) {
            Word currentWord = currentWordsList.get(currentPosition);
            // Update the currentWord object with the data from the EditText fields
            currentWord.setCategory(editTextCategory.getText().toString());
            currentWord.setSubcategory(editTextSubcat.getText().toString());
            currentWord.setItem(editTextItem.getText().toString());
            currentWord.setDescription(editTextDescription.getText().toString());
            currentWord.setDetails(editTextDetails.getText().toString());
            currentWord.setDifficulty(spinnerDifficulty.getSelectedItem().toString());

            // TODO: Set other fields...

            executor.execute(() -> {
                wordDao.update(currentWord);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Word updated successfully", Toast.LENGTH_SHORT).show();
                });
            });
        } else {
            Toast.makeText(this, "No word selected for update", Toast.LENGTH_SHORT).show();
        }
    }
    private void initializeDirectories() {
        this.externalDir = getExternalFilesDir(null);
        if (this.externalDir == null) {
            // Handle the error, e.g., show an error message or disable features that rely on this directory.
        }
    }
    private void loadImageForWord(Word currentWord) {
        File imageFile = generateImagePath(currentWord);
        Uri imageUri = Uri.fromFile(imageFile);
        Log.d("FLAG", "Image filepath: " + imageFile);

        RequestOptions requestOptions = new RequestOptions()
                .fitCenter()
                .override(1100, 700)
                .error(R.drawable.smartflashnoimage)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE);

        Object resourceToLoad = imageFile.exists() ? imageUri : R.drawable.smartflashnoimage;
        if (!imageFile.exists()) {
            Log.d("FLAG", "Image not found: " + imageFile);
        }

        Glide.with(this)
                .load(resourceToLoad)
                .apply(requestOptions)
                .into(this.associatedImage);

    }

    private void displayWord(Word word) {
        currentWord = word; // This sets the class-level currentWord

        editTextCategory.setText(currentWord.getCategory());
        editTextSubcat.setText(currentWord.getSubcategory());
        editTextItem.setText(currentWord.getItem());
        editTextDescription.setText(currentWord.getDescription());
        editTextDetails.setText(currentWord.getDetails());
        String difficulty = currentWord.getDifficulty();
        int index = ((ArrayAdapter<String>) spinnerDifficulty.getAdapter()).getPosition(difficulty);
        spinnerDifficulty.setSelection(index);
        loadImageForWord(currentWord);
    }

    private void getCategoryData(String chosenCategory, Callback<Word> callback) {
        // Use your executor to fetch data off the main thread
        executor.execute(() -> {
            // Fetch data using DAO
            Word data = wordDao.getCategoryData(chosenCategory);

            // Run on the main thread to update UI
            runOnUiThread(() -> {
                callback.onResult(data);
            });
        });
    }

    private void getSubcategoryData(String chosenCategory, String chosenSubcategory, Callback<Word> callback) {
        // Use your executor to fetch data off the main thread
        executor.execute(() -> {
            // Sample: Replace with your database fetch logic
            Word data = wordDao.getSubcategoryData(chosenCategory, chosenSubcategory);

            // Run on the main thread to update UI
            runOnUiThread(() -> {
                callback.onResult(data);
            });
        });
    }

    private void populateCategoryData(String chosenCategory) {
        getCategoryData(chosenCategory, data -> {
            editTextCategory.setText(data.getCategory());
            editTextSubcat.setText(data.getSubcategory());  // If there's no specific getter for the first subcategory, adjust this
            editTextItem.setText(data.getItem());
            editTextDescription.setText(data.getDescription());
            editTextDetails.setText(data.getDetails());

            // Assuming you've already initialized the spinner and its adapter before calling this method
            String difficulty = data.getDifficulty();
            int position = ((ArrayAdapter<String>) spinnerDifficulty.getAdapter()).getPosition(difficulty);
            spinnerDifficulty.setSelection(position);

        });

    // Fetch the list of words for the chosen category
        getWordsForCategory(chosenCategory, words -> {
            if (words != null && !words.isEmpty()) {
                currentWordsList = words;
                currentPosition = 0;
                displayWord(currentWordsList.get(currentPosition));
            } else {
                Log.d("FLAG", "No words found for selected category: " + chosenCategory);
            }
        });
    }
    private void getWordsForCategory(String category, Consumer<List<Word>> callback) {
        executor.execute(() -> {
            List<Word> wordsFetched = new ArrayList<>();
            try {
                wordsFetched = wordDao.getWordsForCategory(category);
                Log.d("FLAG", "Fetched words for category " + category + ": " + wordsFetched);
            } catch (Exception e) {
                Log.e("FLAG", "Error fetching words for category " + category + ": " + e.getMessage(), e);
            }

            // Create a final reference for the fetched words
            final List<Word> finalWords = wordsFetched;

            // Return the fetched words to the UI thread using a callback
            runOnUiThread(() -> callback.accept(finalWords));
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    private void getWordsForSubcategory(String category, String subcategory, Callback<List<Word>> callback) {
        executor.execute(() -> {
            List<Word> wordsFetched = wordDao.getWordsForSubcategory(category, subcategory);
            final List<Word> finalWords = wordsFetched;
            runOnUiThread(() -> callback.onResult(finalWords));
        });
    }

    private void populateSubcategoryData(String chosenCategory, String chosenSubcategory) {
        // Fetch subcategory-specific data and set to EditTexts
        getSubcategoryData(chosenCategory, chosenSubcategory, data -> {
            editTextSubcat.setText(data.getSubcategory());
            editTextItem.setText(data.getItem());
            editTextDescription.setText(data.getDescription());
            editTextDetails.setText(data.getDetails());
            String difficulty = data.getDifficulty();
            int index = ((ArrayAdapter<String>) spinnerDifficulty.getAdapter()).getPosition(difficulty);
            spinnerDifficulty.setSelection(index);
        });

        // Fetch the list of words for the chosen subcategory and category
        getWordsForSubcategory(chosenCategory, chosenSubcategory, words -> {
            if (words != null && !words.isEmpty()) {
                currentWordsList = words;
                currentPosition = 0;
                displayWord(currentWordsList.get(currentPosition));
            } else {
                Log.d("FLAG", "No words found for selected subcategory: " + chosenSubcategory + " in category: " + chosenCategory);
            }
        });
    }


    private Executor executor = Executors.newSingleThreadExecutor();

    private void getAllCategories(Consumer<String[]> callback) {
        Log.d("FLAG", "in getAllCategories method");

        executor.execute(() -> {
            List<String> categories = new ArrayList<>();
            try {
                categories = wordDao.getAllDistinctCategories();
                Log.d("FLAG", "Fetched Categories: " + categories);

            } catch (Exception e) {
                Log.e("FLAG", "in getAllCategories method - BROKEN: " + e.getMessage(), e);
            }

            String[] categoryArray = categories.toArray(new String[0]);
            // Use a callback to return the fetched data to the UI thread
            runOnUiThread(() -> callback.accept(categoryArray));
        });
    }

    private void getSubcategoriesForCategory(String category, Consumer<String[]> callback) {
        executor.execute(() -> {
            List<String> subcategories = new ArrayList<>();
            try {
                subcategories = wordDao.getSubcategoriesByCategory(category);
                Log.d("FLAG", "Fetched Subcategories for " + category + ": " + subcategories);

            } catch (Exception e) {
                Log.e("FLAG", "Error fetching subcategories: " + e.getMessage(), e);
            }

            String[] subcategoryArray = subcategories.toArray(new String[0]);
            // Use a callback to return the fetched data to the UI thread
            runOnUiThread(() -> callback.accept(subcategoryArray));
        });
    }
    private void loadWordsForCategory(String chosenCategory) {
        executor.execute(() -> {
            currentWordsList = wordDao.getWordsForCategory(chosenCategory);
            currentPosition = 0;

            runOnUiThread(() -> {
                if (!currentWordsList.isEmpty()) {
                    displayWord(currentWordsList.get(currentPosition));
                }
            });
        });
        Button btnHome = findViewById(R.id.buttonHome);

        btnHome.setOnClickListener(view -> {
            Intent intent = new Intent(EditDBActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // This flag ensures that if MainActivity is already open, it will be brought to the front
            startActivity(intent);
            finish(); // Optionally, if you want to remove the current activity from the back stack
        });

    }

    private void loadFirstRecordOfCategory(String category) {
        try {
            currentWord = wordDao.getFirstWordByCategory(category);
            // TODO: Display the `currentWord` details on your UI
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFirstRecordOfCategoryAndSubcat(String category, String subcategory) {
        try {
            currentWord = wordDao.getFirstWordByCategoryAndSubcategory(category, subcategory);
            // TODO: Display the `currentWord` details on your UI
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();

            if(currentWord == null) {
                Log.e("FLAG", "Word object is null" + currentWord);
                return;
            }

            File destinationFile = generateImagePath(currentWord);
            Log.d("FLAG", "Desination file path: " + destinationFile);

            if(destinationFile.exists()) {
                // Prompt user to override the existing image
                new AlertDialog.Builder(this)
                        .setTitle("Override Image")
                        .setMessage("An image for this word already exists. Do you want to replace it?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            saveImageToFile(selectedImage, destinationFile);
                            loadImageIntoUI(selectedImage);  // Load the selected image into the ImageView
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                saveImageToFile(selectedImage, destinationFile);
                loadImageIntoUI(selectedImage);  // Load the selected image into the ImageView
            }
        }
    }
    private void loadImageIntoUI(Uri imageUri) {
        RequestOptions requestOptions = new RequestOptions()
                .fitCenter()
                .override(1100, 700)
                .error(R.drawable.smartflashnoimage)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE);

        Glide.with(this)
                .load(imageUri)
                .apply(requestOptions)
                .into(imageView);  // Assuming imageView is your ImageView's variable name
    }
    private void saveImageToFile(Uri selectedImage, File destinationFile) {
        try {
            InputStream is = getContentResolver().openInputStream(selectedImage);
            OutputStream os = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            Toast.makeText(this, "Image saved successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage(Uri sourceUri, File destinationFile) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            OutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Toast.makeText(this, "Image Updated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error updating image", Toast.LENGTH_SHORT).show();
        }
    }

}


