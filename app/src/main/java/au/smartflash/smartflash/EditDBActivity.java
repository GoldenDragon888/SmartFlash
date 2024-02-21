package au.smartflash.smartflash;

import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
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
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import au.smartflash.smartflash.dao.WordDao;
import au.smartflash.smartflash.db.AppDatabase;
import au.smartflash.smartflash.model.Word;

public class EditDBActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
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
    private Uri selectedImage;
    private String selectedSubcategory = null;
    private Spinner spinnerCategory;
    private Spinner spinnerSubcategory;
    List<String> subcatList = new ArrayList<String>();
    private ArrayAdapter<String> subcategoryAdapter;
    private List<Word> words;
    private String category;
    private String subcategory;
    private EditText editTextCategory, editTextSubcat, editTextItem, editTextDifficulty;
    private Button buttonCategory, buttonSubcat, buttonDelete, buttonNew, buttonUpdate, buttonDeleteCategory, buttonDeleteSubcat;
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
    private ActionBarDrawerToggle toggle;
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

            if (id == R.id.nav_flashcards) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                // handle action for 'Edit Card Records'
            } else if (id == R.id.nav_edit_db) {
                Intent intent = new Intent(this, EditDBActivity.class);
                startActivity(intent);
                // handle action for 'Edit Card Records'
            } else if (id == R.id.nav_admin_activity) {
                Intent intent = new Intent(this, AdminActivity.class);
                startActivity(intent);
                // handle action for 'Import Export .CSV'
            } else if (id == R.id.nav_getai_activity) {
                Intent intent = new Intent(this, GetAICardsActivity.class);
                startActivity(intent);

            } else if (id == R.id.nav_getpaipairs_activity) {
                Intent intent = new Intent(this, CardPairListActivity.class);
                startActivity(intent);
                //}else if (id == R.id.user_registration) {
                // handle action for 'User Registration'
            } else if (id == R.id.nav_user_admin) {
                Intent intent = new Intent(this, UserAdminActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_card_image) {
                Intent intent = new Intent(this, ChooseCardFaceActivity.class);
                startActivity(intent);
            }

            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

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

        // Retrieve the Word object passed from MainActivity
        Word word = (Word) getIntent().getSerializableExtra("word");
        if (word != null) {
            currentWord = word; // Set currentWord to the received word
            displayWord(word); // Display the word in your UI
        } else {
            Log.e("EditDBActivity", "Received word is null");
            // Handle the case where no word was passed, such as displaying an error message or closing the activity
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("FLAG", "onResume: currentWord = " + currentWord);
    }
    private void initializeUIElements() {
        // ... Initialize your UI elements ...
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            NavigationView navigationView = findViewById(R.id.nav_view);
            if (navigationView != null) {
                navigationView.setNavigationItemSelectedListener(this);
            }

            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            // Set the color of the "hamburger" icon to white
            toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));

            toggle.syncState();
        } else {
            // Log an error or handle the case where the drawer is null
        }
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);

        editTextCategory = findViewById(R.id.editTextCategory);  // Make sure the ID matches the one in your XML
        editTextSubcat = findViewById(R.id.editTextSubcat);
        editTextItem = findViewById(R.id.editTextItem);

        // Initialize the buttons
        //Button buttonCategory = findViewById(R.id.buttonCategory); // replace with your button ID

        Button buttonDelete = findViewById(R.id.buttonDelete);
        buttonDelete.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
        Button buttonNew = findViewById(R.id.buttonNew);
        buttonNew.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
        Button buttonUpdate = findViewById(R.id.buttonUpdate);
        buttonUpdate.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
        Button cleanUpButton = findViewById(R.id.cleanUpButton);
        cleanUpButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));

        // Set the click listeners
        buttonDelete.setOnClickListener(view -> showDeleteWordConfirmationDialog());
        buttonNew.setOnClickListener(view -> createNewWord());
        buttonUpdate.setOnClickListener(view -> updateCurrentWord());
        cleanUpButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clean Up Categories")
                    .setMessage("Do you want to remove all invalid category records?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        deleteNullCategories();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        editTextDescription = findViewById(R.id.editTextDescription);

        editTextDetails = findViewById(R.id.editTextDetails);
        //saveDetailsEditButton = findViewById(R.id.saveDetailsEditButton);
        Button buttonDeleteCategory = findViewById(R.id.buttonDeleteCategory);
        buttonDeleteCategory.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
        Button buttonDeleteSubcat = findViewById(R.id.buttonDeleteSubcat);
        buttonDeleteSubcat.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));


        buttonDeleteCategory.setOnClickListener(v -> {
            String category = editTextCategory.getText().toString();
            if (!category.isEmpty()) {
                deleteCategory(category);
            } else {
                showSnackbar("No category specified");
            }
        });

        buttonDeleteSubcat.setOnClickListener(v -> {
            String category = editTextCategory.getText().toString();
            String subcategory = editTextSubcat.getText().toString();
            if (!category.isEmpty() && !subcategory.isEmpty()) {
                deleteSubcategory(category, subcategory);
            } else {
                showSnackbar("No category or subcategory specified");
            }
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

        Button buttonNext = findViewById(R.id.buttonNext);
        buttonNext.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));

        Button buttonHome = findViewById(R.id.buttonHome);
        buttonHome.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));


        buttonHome.setOnClickListener(view -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("updatedWord", currentWord); // 'updatedWord' is your edited Word object
            setResult(RESULT_OK, returnIntent);
            finish();

        });


        Button buttonCategory = findViewById(R.id.buttonCategory);
        buttonCategory.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));

        Button buttonSubcat = findViewById(R.id.buttonSubcat);
        buttonSubcat.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));

        imageView = findViewById(R.id.associated_image);
        associatedImage = findViewById(R.id.associated_image);


        buttonCategory.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(EditDBActivity.this);
            builder.setTitle("Select Category");

            getAllCategories(categories -> {
                Arrays.sort(categories, String.CASE_INSENSITIVE_ORDER);  // Sort the categories array first
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
                            buttonSubcat.setText(firstSubcategory);
                            // ... rest of the code ...
                        }
                    });
                });
                builder.show();
            });
        });

        buttonSubcat.setOnClickListener(view -> {
            String selectedCategory = editTextCategory.getText().toString();
            if (selectedCategory.isEmpty()) {
                showSnackbar("Please select a category first!");
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(EditDBActivity.this);
            builder.setTitle("Select Subcategory");

            getSubcategoriesForCategory(selectedCategory, subcategories -> {
                Arrays.sort(subcategories, String.CASE_INSENSITIVE_ORDER);  // Sort the subcategories array
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
                showSnackbar( "You've reached the end of the list.");
            }
        });
        buttonBack.setOnClickListener(view -> {
            if (currentWordsList != null && currentPosition > 0) {
                currentPosition--;
                displayWord(currentWordsList.get(currentPosition));
            } else {
                showSnackbar("You're at the start of the list.");
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
        // Use the currentWord's category, subcategory, and item to generate the file path
        String category = currentWord.getCategory() != null ? currentWord.getCategory() : "unknown";
        String subcategory = currentWord.getSubcategory() != null ? currentWord.getSubcategory() : "unknown";
        String item = currentWord.getItem() != null ? currentWord.getItem() : "unknown";

        return new File(getExternalFilesDir(null), "Images/" + category + "/" + subcategory + "/" + item + ".png");
    }

    private void pickImageFromGallery() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageIntent.setType("image/*");
        startActivityForResult(pickImageIntent, IMAGE_PICK_REQUEST);
    }
    public void deleteNullCategories() {
        executor.execute(() -> {
            try {
                wordDao.deleteNullCategories(); // Assuming you have a method in wordDao to delete null categories
                Log.d("FLAG", "Null categories deleted successfully.");
            } catch (Exception e) {
                Log.e("FLAG", "Error deleting null categories: " + e.getMessage(), e);
            }
        });
    }

    /*private void deleteCurrentWord() {
        if (currentWordsList != null && !currentWordsList.isEmpty()) {
            Word currentWord = currentWordsList.get(currentPosition);
            executor.execute(() -> {
                wordDao.deleteWord(currentWord);
                runOnUiThread(() -> {
                    showSnackbar("Word deleted successfully");
                    refreshWordsAndNavigate(); // Refresh the list and navigate
                });
            });
        } else {
            showSnackbar("No word to delete");
        }
    }*/
    private void deleteCurrentWord(boolean isSingleDeletion) {
        if (currentWord != null) {
            executor.execute(() -> {
                wordDao.deleteWord(currentWord);
                runOnUiThread(() -> {
                    showSnackbar("Word deleted successfully");
                    if (isSingleDeletion) {
                        displayFirstWordOfCurrentCategoryAndSubcategory();
                    } else {
                        refreshWordsAndNavigate();
                    }
                });
            });
        } else {
            showSnackbar("No word to delete");
            Log.e("EditDBActivity", "currentWord is null, cannot delete");
        }
    }
    private void displayFirstWordOfCurrentCategoryAndSubcategory() {
        String currentCategory = getCurrentCategory();
        String currentSubcategory = getCurrentSubcategory();

        getFirstWordForCategoryAndSubcategory(currentCategory, currentSubcategory, word -> {
            if (word != null) {
                displayWord(word);
            } else {
                showSnackbar("No more words to display in this category/subcategory.");
            }
        });
    }

    private void getFirstWordForCategoryAndSubcategory(String category, String subcategory, Callback<Word> callback) {
        executor.execute(() -> {
            Word firstWord = wordDao.getFirstWordByCategoryAndSubcategory(category, subcategory);
            runOnUiThread(() -> callback.onResult(firstWord));
        });
    }

    private String getCurrentCategory() {
        return editTextCategory.getText().toString();
    }

    private String getCurrentSubcategory() {
        return editTextSubcat.getText().toString();
    }
    private void refreshWordsAndNavigate() {
        String currentCategory = getCurrentCategory();
        String currentSubcategory = getCurrentSubcategory();

        getWordsForSubcategory(currentCategory, currentSubcategory, words -> {
            currentWordsList = words;
            if (currentWordsList != null && !currentWordsList.isEmpty()) {
                if (currentPosition >= currentWordsList.size()) {
                    currentPosition = 0; // Adjust for deleted word being the last in the list
                }
                Log.d("FLAG", "Displaying word at adjusted position: " + currentPosition);
                displayWord(currentWordsList.get(currentPosition));
            } else {
                Log.d("FLAG", "No words left in the category/subcategory after deletion.");
                showSnackbar("No more words to display.");
            }
        });
    }

    private void createNewWord() {
        executor.execute(() -> {
            // Get the highest ID and increment it
            int maxId = wordDao.getMaxId();
            Word newWord = new Word();
            newWord.setId(maxId + 1);
            newWord.setCategory(editTextCategory.getText().toString());
            newWord.setSubcategory(editTextSubcat.getText().toString());
            newWord.setItem(editTextItem.getText().toString());
            newWord.setDescription(editTextDescription.getText().toString());
            newWord.setDetails(editTextDetails.getText().toString());
            newWord.setDifficulty(spinnerDifficulty.getSelectedItem().toString());

            // Handle image
            if (selectedImage != null) {
                byte[] imageBytes = uriToByteArray(selectedImage);
                newWord.setImage(imageBytes);
            }

            // Insert the new word into the database
            wordDao.insert(newWord);

            // UI update on the main thread
            runOnUiThread(() -> {
                Log.d("FLAG", "New word added: " + newWord.toString());
                showSnackbar("New word added successfully");
                // Clear fields or other UI updates
            });
        });
    }

    private void updateCurrentWord() {
        if (currentWord != null) {
            currentWord.setCategory(editTextCategory.getText().toString());
            currentWord.setSubcategory(editTextSubcat.getText().toString());
            currentWord.setItem(editTextItem.getText().toString());
            currentWord.setDescription(editTextDescription.getText().toString());
            currentWord.setDetails(editTextDetails.getText().toString());
            currentWord.setDifficulty(spinnerDifficulty.getSelectedItem().toString());

            // Save the image if a new image has been selected
            if (selectedImage != null) {
                File imagePath = generateImagePath(currentWord);
                saveImageToFile(selectedImage, imagePath);
            }

            // Update the word in the database
            executor.execute(() -> {
                wordDao.update(currentWord);
                runOnUiThread(() -> {
                    showSnackbar("Word updated successfully");
                    Log.d("FLAG", "EditDBActivity - Word updated successfully: " + currentWord);
                });
            });
        } else {
            showSnackbar("No word selected for update");
            Log.e("FLAG", "EditDBActivity - currentWord is null, cannot update");
        }
    }
    private void initializeDirectories() {
        this.externalDir = getExternalFilesDir(null);
        if (this.externalDir == null) {
            // Handle the error, e.g., show an error message or disable features that rely on this directory.
        }
    }
    private void updateWordWithNewImage(Uri imageUri, File destinationFile) {
        // Convert imageUri to byte array if needed or store the image path
        byte[] imageBytes = uriToByteArray(imageUri);
        currentWord.setImage(imageBytes);

        // Update the word in the database
        executor.execute(() -> {
            wordDao.update(currentWord);
            runOnUiThread(() -> {
                Log.d("FLAG", "Word updated with new image successfully");
                showSnackbar("Word updated with new image successfully");
                // Refresh UI if necessary
            });
        });
    }

    private byte[] uriToByteArray(Uri uri) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    private void displayWord(Word word) {
        if (word == null) {
            // Handle null word
            clearTextFields();
            return;
        }

        currentWord = word; // Update currentWord
        updateTextFields(word); // Update text fields with word details
        loadImageForWord(word); // Load and display the associated image
    }

    private void updateTextFields(Word word) {
        editTextCategory.setText(safeString(word.getCategory()));
        editTextSubcat.setText(safeString(word.getSubcategory()));
        editTextItem.setText(safeString(word.getItem()));
        editTextDescription.setText(safeString(word.getDescription()));
        editTextDetails.setText(safeString(word.getDetails()));
        updateSpinnerSelection(word.getDifficulty());
    }

    private void loadImageForWord(Word word) {
        File imageFile = generateImagePath(word);
        Uri imageUri = Uri.fromFile(imageFile);

        RequestOptions requestOptions = new RequestOptions()
                .fitCenter()
                .centerInside()  // or use centerInside() based on your requirement
                .override(1100, 700)
                .error(R.drawable.smartflashnoimage)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE);

        Object resourceToLoad = imageFile.exists() ? imageUri : R.drawable.smartflashnoimage;
        Glide.with(this)
                .load(resourceToLoad)
                .apply(requestOptions)
                .into(associatedImage);
    }

    private void clearTextFields() {
        editTextCategory.setText("");
        editTextSubcat.setText("");
        editTextItem.setText("");
        editTextDescription.setText("");
        editTextDetails.setText("");
        spinnerDifficulty.setSelection(0);
        Glide.with(this).load(R.drawable.smartflashnoimage).into(associatedImage);
    }

    private void updateSpinnerSelection(String difficulty) {
        if (difficulty != null && !difficulty.isEmpty()) {
            int index = ((ArrayAdapter<String>) spinnerDifficulty.getAdapter()).getPosition(difficulty);
            spinnerDifficulty.setSelection(index);
        } else {
            spinnerDifficulty.setSelection(0);
        }
    }

    // Helper method to ensure strings are not null
    private String safeString(String input) {
        return input != null ? input : "";
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
                List<String> fetchedCategories = wordDao.getAllDistinctCategories();
                for (String category : fetchedCategories) {
                    if (category == null) {
                        Log.e("FLAG", "Null category found");
                    } else {
                        Log.d("FLAG", "Fetched Category: " + category);
                        categories.add(category);
                    }
                }
                categories.sort(String.CASE_INSENSITIVE_ORDER);
            } catch (Exception e) {
                Log.e("FLAG", "in getAllCategories method - BROKEN: " + e.getMessage(), e);
            }

            String[] categoryArray = categories.toArray(new String[0]);
            runOnUiThread(() -> callback.accept(categoryArray));
        });
    }

    private void getSubcategoriesForCategory(String category, Consumer<String[]> callback) {
        executor.execute(() -> {
            List<String> subcategories = new ArrayList<>();
            try {
                List<String> fetchedSubcategories = wordDao.getSubcategoriesByCategory(category);
                for (String subcategory : fetchedSubcategories) {
                    if (subcategory == null) {
                        Log.e("FLAG", "Null subcategory found in category: " + category);
                    } else {
                        Log.d("FLAG", "Fetched Subcategory: " + subcategory + " in category: " + category);
                        subcategories.add(subcategory);
                    }
                }
                subcategories.sort(String.CASE_INSENSITIVE_ORDER);
            } catch (Exception e) {
                Log.e("FLAG", "Error fetching subcategories: " + e.getMessage(), e);
            }

            String[] subcategoryArray = subcategories.toArray(new String[0]);
            runOnUiThread(() -> callback.accept(subcategoryArray));
        });
    }

    private void deleteCategory(String category) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete the entire category '" + category + "'? If so, you will need to download it again.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    executor.execute(() -> {
                        wordDao.deleteWordsByCategory(category);

                        // You need to convert LiveData to List synchronously since we are already off the main thread.
                        List<Word> updatedWords = wordDao.getAllWords(); // Make sure to call this after the deletion is done.
                        runOnUiThread(() -> {
                            showSnackbar("Category deleted successfully");
                            // TODO: Update your UI here if necessary.
                            initializeUIElements();
                            // The LiveData should be observed in the place where you are setting the adapter data.
                        });
                    });
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void deleteSubcategory(String category, String subcategory) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Subcategory")
                .setMessage("Are you sure you want to delete the entire subcategory '" + subcategory + "' in category '" + category + "'? If so, you will need to download it again.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    executor.execute(() -> {
                        wordDao.deleteWordsBySubcategory(category, subcategory);

                        // Same here for LiveData to List conversion.
                        List<Word> updatedWords = wordDao.getAllWords(); // Again, make sure to call this after the deletion.
                        runOnUiThread(() -> {
                            showSnackbar("Subcategory deleted successfully");
                            // TODO: Update your UI here if necessary.
                            initializeUIElements();

                            // The LiveData should be observed in the place where you are setting the adapter data.
                        });
                    });
                })
                .setNegativeButton(android.R.string.no, null).show();
    }
    private void showDeleteWordConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete this word?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Assuming isSingleDeletion is a field in this class
                // Set it to true if it's a single record deletion context
                // For example, set it to true here if this is the EditDBActivity
                deleteCurrentWord(true); // Or just use deleteCurrentWord(true) if it's always a single record deletion in this context
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
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
            // Update the item name of currentWord here before saving the image
            currentWord.setItem(editTextItem.getText().toString());

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
            // Ensure parent directories exist
            File parentDir = destinationFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir);
            }

            InputStream is = getContentResolver().openInputStream(selectedImage);
            OutputStream os = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            Log.d("FLAG", "Image saved successfully at: " + destinationFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("FLAG", "Failed to save image: " + e.getMessage());
        }
    }


    public void showSnackbar(String message) {
        View view = findViewById(android.R.id.content); // Finds the root view of the current activity layout
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);

        // Customize the Snackbar's background color and text
        //snackbar.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.dark_blue));
        snackbar.getView().setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));

        TextView textView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // Set text size

        // Customize the width and position of the Snackbar
        View snackbarLayout = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarLayout.getLayoutParams();

        params.gravity = Gravity.CENTER; // Center both vertically and horizontally
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;  // Set the width of the Snackbar to wrap its content
        snackbarLayout.setLayoutParams(params);

        snackbar.show();
    }
}


