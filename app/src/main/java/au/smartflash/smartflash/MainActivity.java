package au.smartflash.smartflash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;


import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import au.smartflash.smartflash.dao.WordDao;
import au.smartflash.smartflash.db.AppDatabase;
import au.smartflash.smartflash.model.OnTaskCompleted;
import au.smartflash.smartflash.model.Word;

import android.Manifest;

public class MainActivity extends AppCompatActivity implements OnTaskCompleted, NavigationView.OnNavigationItemSelectedListener {
    private static final int AUDIO_REQUEST_CODE = 1234;
    private static final int OVERLAY_PERMISSION_REQ_CODE = 9101;
    private static final int REQUEST_CODE = 1234;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1234;
    private static final int REQUEST_RECORD_AUDIO = 5678;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 200;
    private static final String TAG = "MainActivity";
    private ArrayAdapter<String> adapterCategory;
    private ArrayAdapter<String> adapterSubcat;
    private Button audioCheckButton;

    private ImageView back_desc_container;
    private ImageView back_details_container;
    private ImageView back_image_container;
    private ImageView back_item_container;
    private FrameLayout frontCardContainer;
    private TextView frontTextView;
    private FrameLayout frontItemContainer;

    private FrameLayout backCardContainer;
    private TextView backTextView;
    private FrameLayout backItemContainer;

    private FrameLayout detailsCardContainer;
    private TextView detailsTextView;
    private FrameLayout detailsItemContainer;
    private FrameLayout imageCardContainer;
    private ImageView imageViewFront;  // assuming this is the front
    private FrameLayout imageViewBackContainer;  // this contains the back of the card
    private TextView backImageTextView;

    Button btnEasy;
    Button btnHard;
    Button btnMedium;
    Button buttonCategory;
    Button buttonDifficulty;
    Button buttonSubcat;
    List<String> categoriesList = new ArrayList<String>();
    private int categoryCount = 0;
    private Word currentAudio;
    private int currentIndex = 0;
    private int currentPosition;
    private int currentRecordIndex;
    private Word currentWord;
    private AppDatabase db;
    private WordDao wordDao;
    private TextView descriptionTextView;
    private String difficulty = "All";
    private int difficultyCount = 0;
    private DrawerLayout drawer;
    private File externalDir;
    private ImageView imageImageView;
    private ImageView imageViewPart1;
    private ImageView imageViewPart2;
    private ImageView imageViewPart3;
    private ImageView imageViewPart4;
    private ImageView image_showCard;
    private boolean isBackVisible = true;
    private boolean isDescriptionVisible = true;
    private boolean isFrontVisible = true;
    private boolean isImageVisible = true;
    private boolean isRecording = false;
    private boolean isSpinnerListenerSet = false;
    private MediaPlayer mediaPlayer = null;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = new String[] { "android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE" };
    private Word previousAudio;
    private Word previousWord;
    private MediaRecorder recorder;
    private String selectedCategory;
    private Spinner spinnerCategory;
    private Spinner spinnerSubcat;
    private String subcategory;
    private String category;
    List<String> subcatList = new ArrayList<String>();
    private Observer<List<String>> subcategoriesObserver;
    private int subcategoryCount = 0;
    private TextView textViewCategory;
    private TextView textViewDifficulty;
    private TextView textViewSubcat;
    TextView tvCardCount;
    private List<Word> wordList;
    private List<Word> wordsList = new ArrayList<Word>();
    private List<Word> allListWords;  // assuming Word is the datatype you're using
    private Map<String, List<String>> categoryToSubcatMap = new HashMap<>();
    private Map<Pair<String, String>, List<String>> categorySubcatToDifficultyMap = new HashMap<>();
    private final String PREF_NAME = "MyPrefs";
    private final String KEY_LAST_CATEGORY = "lastCategory";
    private final String KEY_LAST_SUBCAT = "lastSubcat";
    private final String KEY_LAST_DIFFICULTY = "lastDifficulty";
    private boolean isFirstRun = true;
    private int lastWordId = -1; // Store the last word's ID
    private Stack<Word> wordHistory = new Stack<>();
    private SharedPreferences prefs;
    private ActionBarDrawerToggle toggle;
    private static final int PERMISSION_REQUEST_CODE = 1234;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private static final String PREFS_NAME = "AppPrefs";
    private static final String DATABASE_INITIALIZED = "database_initialized";
    private static final String DATABASE_NAME = "SMARTFLASHDB.sqlite";
    private LiveData<List<String>> uniqueCategories;
    private FirebaseAuth auth;
    private static final String PREFS_USER_NAME = "UserPrefs";
    private static final String PREF_EMAIL = "EmailKey";
    private static final String PREF_PASSWORD = "PasswordKey";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        initializeDatabase();

        uniqueCategories = wordDao.getUniqueCategories();

        // Initialize SharedPreferences
        prefs = getSharedPreferences("YourAppPreferences", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up ActionBarDrawerToggle
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);

        // Set the color of the "hamburger" icon to white
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        toggle.syncState();

        // Check if user is signed in (non-null)
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // No user is signed in, redirect to UserAdminActivity
            startActivity(new Intent(this, UserAdminActivity.class));
            finish();  // Optional: Close the MainActivity
        } else {
            setupSubcategoriesMap();
            //initDatabase();
            initViews();
            initializeUIComponents();

            setOnClickListeners();
            initializeDataObservers();
            loadMappingsFromDatabase();
            loadWordsIfNeeded();
            retrievePreferences();
            updateUI();
            updateButtonStates(difficulty);
            // Check and request storage permissions
            if (!checkStoragePermissions()) {
                requestForStoragePermissions();
            }
            showNextFlashcard();
        }
    }
    private static final int REQUEST_STORAGE_PERMISSION = 1000;
    private static final int STORAGE_PERMISSION_CODE = 23;
    private boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }
    private static final int RECORD_AUDIO_REQUEST_CODE = 101;

    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_AUDIO_REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can start recording
                String audioFilePath = getAudioFilePath(currentWord.getItem());
                startRecordingToFile(audioFilePath);
            } else {
                // Permission denied, show rationale or handle accordingly
                Toast.makeText(this, "Record audio permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

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
                                    Toast.makeText(MainActivity.this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Below Android 11
                                // Handle the result for lower Android versions if needed
                            }
                        }
                    });

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    // First, let's set up a LiveData to hold the main categories
    //private final LiveData<List<String>> uniqueCategories = getWordDao().getUniqueCategories();

    // Next, set up a LiveData to hold the subcategories based on the main categories
    private final MediatorLiveData<Map<String, List<String>>> subcategoriesMap = new MediatorLiveData<>();

    private void setupSubcategoriesMap() {
        subcategoriesMap.addSource(uniqueCategories, categories -> {
            // Remove the source immediately to prevent re-adding sources later
            subcategoriesMap.removeSource(uniqueCategories);

            Map<String, List<String>> tempMap = new HashMap<>();
            AtomicInteger counter = new AtomicInteger(categories.size());

            // Keep track of current LiveData sources so we can remove them before adding new ones
            List<LiveData<List<String>>> currentSources = new ArrayList<>();

            for (String category : categories) {
                LiveData<List<String>> subcatsLiveData = wordDao.getUniqueSubcategories(category);
                currentSources.add(subcatsLiveData);

                subcategoriesMap.addSource(subcatsLiveData, subcategories -> {
                    tempMap.put(category, subcategories);

                    if (counter.decrementAndGet() == 0) {
                        // Remove current LiveData sources
                        for (LiveData<List<String>> source : currentSources) {
                            subcategoriesMap.removeSource(source);
                        }
                        subcategoriesMap.setValue(tempMap);
                    }
                });
            }
        });
    }


    public void initializeDataObservers() {
        uniqueCategories.observe(this, categories -> {
            Log.d("FLAG", "getUniqueCategories onChanged categories: " + categories);
            categoriesList = categories;
        });

        subcategoriesMap.observe(this, map -> {
            Log.d("FLAG", "SubcategoriesMap onChanged map: " + map);
            categoryToSubcatMap.clear();
            categoryToSubcatMap.putAll(map);
        });
    }

    private void initializeDatabase() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Check if database has already been initialized
        if (!prefs.getBoolean(DATABASE_INITIALIZED, false)) {
            try {
                db = Room.databaseBuilder(
                                getApplicationContext(),
                                AppDatabase.class,
                                DATABASE_NAME
                        )
                        .createFromAsset("databases/" + DATABASE_NAME)
                        .fallbackToDestructiveMigration()
                        .build();

                // Once initialized successfully, set the flag in SharedPreferences
                prefs.edit().putBoolean(DATABASE_INITIALIZED, true).apply();
            } catch (Exception e) {
                // Handle or log any initialization errors here
                Log.e("DatabaseInit", "Failed to initialize database: " + e.getMessage());
            }
        } else {
            db = AppDatabase.getInstance(getApplicationContext());
        }

        // Initialize wordDao after db initialization
        wordDao = db.wordDao();
    }

    private void initViews() {
        setSupportActionBar(findViewById(R.id.toolbar));  // Assuming you have a toolbar with the id 'toolbar'

        //descriptionTextView = findViewById(R.id.back_desc_text_view);
        //backTextView = findViewById(R.id.back_desc_text_view);
        //frontTextView = findViewById(R.id.back_item_text_view);
        btnEasy = findViewById(R.id.btnEasy);
        btnMedium = findViewById(R.id.btnMedium);
        btnHard = findViewById(R.id.btnHard);
        buttonCategory = findViewById(R.id.button_category);
        buttonSubcat = findViewById(R.id.button_subcat);
        buttonDifficulty = findViewById(R.id.button_difficulty);
        imageImageView = findViewById(R.id.image_showCard);

        frontCardContainer = findViewById(R.id.front_card_view);
        frontTextView = findViewById(R.id.front_text_view);
        frontItemContainer = findViewById(R.id.back_item_container);

        backCardContainer = findViewById(R.id.back_card_view);
        backTextView = findViewById(R.id.back_text_view);
        backItemContainer = findViewById(R.id.back_desc_container);

        detailsCardContainer = findViewById(R.id.details_card_view);
        detailsTextView = findViewById(R.id.details_text_view);
        detailsItemContainer = findViewById(R.id.back_details_container);

        imageCardContainer = findViewById(R.id.image_card_view);
        imageViewFront = findViewById(R.id.image_showCard);  // assuming this is the front
        imageViewBackContainer = findViewById(R.id.back_image_container);  // this contains the back of the card

        imageViewPart1 = findViewById(R.id.imageViewPart1);
        imageViewPart2 = findViewById(R.id.imageViewPart2);
        imageViewPart3 = findViewById(R.id.imageViewPart3);
        imageViewPart4 = findViewById(R.id.imageViewPart4);

        // Set up the flipping
        setupCardFlip(frontCardContainer, frontTextView, frontItemContainer, new BooleanHolder(false));
        setupCardFlip(backCardContainer, backTextView, backItemContainer, new BooleanHolder(false));
        setupCardFlip(detailsCardContainer, detailsTextView, detailsItemContainer, new BooleanHolder(false));
        setupCardFlip(imageCardContainer, imageViewFront, imageViewBackContainer, new BooleanHolder(false));
    }

    private void setOnClickListeners() {
            btnEasy.setOnClickListener(v -> showUpdateDifficultyDialog("Easy"));
            btnMedium.setOnClickListener(v -> showUpdateDifficultyDialog("Medium"));
            btnHard.setOnClickListener(v -> showUpdateDifficultyDialog("Hard"));

            buttonCategory.setOnClickListener(v -> showCategorySelectionDialog());
            buttonSubcat.setOnClickListener(v -> showSubcatSelectionDialog());
            buttonDifficulty.setOnClickListener(v -> showDifficultySelectionDialog());

            findViewById(R.id.next_button).setOnClickListener(v -> handleNextButtonClick());
            findViewById(R.id.back_button).setOnClickListener(v -> handlePreviousButtonClick());

            findViewById(R.id.play_button).setOnClickListener(v -> playCurrentWordAudio());
            findViewById(R.id.stop_button).setOnClickListener(v -> stopMediaPlayer());
            audioCheckButton = findViewById(R.id.audio_check);

            findViewById(R.id.record_button).setOnClickListener(v -> {
                Log.d("FLAG", "recordButton was clicked");
                toggleRecording();
            });
    }

    private void showUpdateDifficultyDialog(String difficulty) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                .setTitle("Update Difficulty")
                .setMessage("Are you sure you want to update the difficulty to " + difficulty + "?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> updateDifficulty(difficulty))
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert);
        dialogBuilder.show();
    }

    //private Executor executor = Executors.newSingleThreadExecutor(); // Initialize the executor
/*
    private void updateDifficulty(String newDifficulty) {
        if (currentWord != null) {
            synchronized (currentWord) {
                currentWord.setDifficulty(newDifficulty);
                Log.d("FLAG", "Setting difficulty in-memory: " + currentWord.getDifficulty());

                executor.execute(() -> {
                    try {
                        if (db != null && wordDao != null) {
                            Log.d("FLAG", "Updating difficulty with currentWord: " + currentWord);

                            // Update the database and check the return value
                            int rowsAffected = wordDao.update(currentWord);
                            if (rowsAffected > 0) {
                                Log.d("FLAG", "Successfully updated word: " + currentWord.getItem());
                            } else {
                                Log.e("FLAG", "Failed to update word: No matching word found in the database.");
                                return; // Exit from this execution if update wasn't successful
                            }

                            // Fetch fresh data after a slight delay, ensuring the update is committed
                            try {
                                Thread.sleep(100);  // introducing a small delay, you might adjust or remove this
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            currentWord = wordDao.getWordById(currentWord.getId());
                            Log.d("FLAG", "Fetched from database: " + currentWord.getDifficulty());
                        } else {
                            Log.e("DatabaseUpdate", "Database or DAO is null. Check initialization.");
                        }
                    } catch (Exception e) {
                        Log.e("DatabaseUpdate", "Failed to update difficulty: " + e.getMessage());
                    }
                });
            }
        } else {
            Toast.makeText(this, "No word selected for update", Toast.LENGTH_SHORT).show();
        }
    }

*/
    private void updateDifficulty(String newDifficulty) {
        if (currentWord != null) {
            synchronized (currentWord) {
                currentWord.setDifficulty(newDifficulty);
                Log.d("FLAG", "Setting difficulty in-memory: " + currentWord.getDifficulty());

                executor.execute(() -> {
                    Log.d("FLAG", "Updating word with new difficulty - currentWord: " + currentWord);
                    wordDao.update(currentWord);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Word updated successfully", Toast.LENGTH_SHORT).show();
                        updateCatSubcatDiffLabelsandCounts();
                        //handleSelectedDifficulty(newDifficulty);
                    });
                });
            }
        } else {
                Toast.makeText(this, "No word selected for update", Toast.LENGTH_SHORT).show();
        }

    }
    private void displayToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }
    private void handleNextButtonClick() {
        String currentDifficulty = difficulty;
        Log.d("FLAG", "Handling next button click. Current difficulty: " + currentDifficulty);

        if ("Random".equals(difficulty)) {
            currentDifficulty = getRandomDifficulty();
        }

        wordHistory.push(currentWord);
        showNextFlashcard();
        updateButtonStates(currentDifficulty);
        updateCatSubcatDiffLabelsandCounts();
    }

    private String getRandomDifficulty() {
        double randomValue = Math.random();
        if (randomValue < 0.5) {
            return "Hard";
        } else if (randomValue < 0.8) {
            return "Medium";
        } else {
            return "Easy";
        }
    }

    private void handlePreviousButtonClick() {
        if (!wordHistory.isEmpty()) {
            wordHistory.pop(); // Pop the current word

            if (!wordHistory.isEmpty()) {
                Word previousWord = wordHistory.peek(); // Get the previous word without removing it
                updateWordDisplay(previousWord);
            } else {
                Toast.makeText(MainActivity.this, "No previous word.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "No previous word.", Toast.LENGTH_SHORT).show();
        }
    }

    private void playCurrentWordAudio() {
        if (currentWord == null) {
            // Handle the situation when the word is null
            showSnackbar("No word available to play!"); // assuming you have a showSnackbar method
            return; // exit the method
        }
        String wordFront = currentWord.getItem();
        String audioFilePath = getAudioFilePath(wordFront);
        playAudio(audioFilePath);

    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void loadWordsIfNeeded() {
        Log.d("FLAG", "Before checking conditions: selectedCategory = " + selectedCategory + ", subcategory = " + subcategory);
        if ("All".equals(difficulty) && selectedCategory != null && subcategory != null) {
            loadActualWordsForCategoryAndSubcategory(selectedCategory, subcategory);
        } else {
            Log.d("FLAG", "Either 'All' is not chosen, or Category/Subcategory are not initialized yet.");
        }
    }
    private void loadActualWordsForCategoryAndSubcategory(String category, String subcategory) {
        Log.d("FLAG", "loadActualWordsForCategoryAndSubcategory called with category: " + category + ", subcategory: " + subcategory);
        executor.execute(() -> {
            wordsList = wordDao.getWordsForSubcategory(category, subcategory);
            currentIndex = 0;
            // Fetch and display the word on the UI
            fetchWord();
        });
    }

    private void addDefaultRecord() {
        new Thread(() -> {
            // Place the code from MainActivity$$ExternalSyntheticLambda2 here
            // for example:
            // yourDatabase.addDefaultRecord();
        }).start();
    }
    private void loadImageForWord(Word word) {

        if (this.externalDir == null) {
            Log.d("FLAG", "loadImageForWord: externalDir is testing null " + word);
            Glide.with(this)
                    .load(R.drawable.smartflashnoimage)
                    .into(this.imageImageView);
            return;
        }

        File imageFile = new File(getExternalFilesDir(null), "Images/" + this.selectedCategory + "/" + this.subcategory + "/" + word.getItem() + ".png");
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
                .into(this.imageImageView);
    }

    private boolean checkForDefaultRecord() {
        // Create a FutureTask that queries the database for a specific word.
        FutureTask<Boolean> task = new FutureTask<>(() -> {
            Word word = wordDao.getWordByFront("Word");
            return word != null;
        });

        // Start the task in a new thread since database calls should not be made on the main thread.
        Thread thread = new Thread(task);
        thread.start();

        // Wait for the task to finish and get the result.
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false; // Return false or handle error appropriately.
        }
    }

    private void displayWord(int paramInt) {
        List<Word> list = this.wordsList;
        if (list != null && !list.isEmpty() && paramInt >= 0 && paramInt < this.wordsList.size()) {
            updateWordDisplay(this.wordsList.get(paramInt));
        } else {
            Log.e("ERROR", "Word list is empty or the provided index is invalid.");
        }
    }

    private String getAudioFilePath(String itemName) {
        // Get the app's external directory
        File externalAppDir = getExternalFilesDir(null);

        // Construct the path for the audio directory
        File audioDir = new File(externalAppDir, "Smartflash/Audio/" + selectedCategory + "/" + subcategory);
        Log.d("FLAG", "Audio Directory path: " + audioDir);

        // Check if the directory exists, if not, create it
        if (!audioDir.exists()) {
            Log.d("FLAG", "Audio directory doesn't exist. Creating now.");
            if (!audioDir.mkdirs()) {
                Log.d("FLAG", "Error creating audio directory");
                return null;
            }
        }

        // Construct the complete audio file path
        File audioFile = new File(audioDir, itemName + ".wav");
        String audioFilePath = audioFile.getAbsolutePath();

        if (audioFilePath.length() == 0) {
            audioCheckButton.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_green));
        } else {
            audioCheckButton.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_red));
        }

        Log.d("FLAG", "Got audioFilePath Path: " + audioFilePath);
        return audioFilePath;
    }


    private int getAudioResId(String paramString) {
        return getResources().getIdentifier(paramString, "raw", getPackageName());
    }

    private Uri getImageUriFromMediaStore(String paramString1, String paramString2) {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContentResolver().query(uri, new String[] { "_id" }, "_display_name=? AND relative_path=?", new String[] { paramString2, paramString1 }, null);
        if (cursor != null)
            try {
                if (cursor.moveToFirst()) {
                    long l = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                    return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, l);
                }
            } finally {
                if (cursor != null)
                    try {
                        cursor.close();
                    } finally {
                        cursor = null;
                    }
            }
        if (cursor != null)
            cursor.close();
        return null;
    }

    private void goToFirstRecord() {
        List<Word> list = this.wordList;
        if (list != null && !list.isEmpty()) {
            this.currentRecordIndex = 0;
            updateWordDisplay(this.wordList.get(0));
        }
    }

    private void goToRecord(int paramInt) {
        Log.d("FLAG", "goToRecord - index: " + paramInt);
        if (this.currentWord != null && paramInt >= 0 && paramInt < this.wordList.size()) {
            this.currentRecordIndex = paramInt;
            updateWordDisplay(this.wordList.get(paramInt));
        } else {
            Log.d("FLAG", "goToRecord - currentWord is null or index out of bounds");
        }
    }

    private int getSelectedCategoryCount() {
        int categoryCount = wordDao.getCategoryCount(selectedCategory);
        return categoryCount;
    }

    private int getSelectedSubcategoryCount() {
        int subcategoryCount = wordDao.getSubcategoryCount(selectedCategory, subcategory);
        return subcategoryCount;
    }

    private int getSelectedDifficultyCount() {
        int difficultyCount;

        if ("All".equals(difficulty)) {
            difficultyCount = wordDao.getSubcategoryCount(selectedCategory, subcategory);
        } else {
            difficultyCount = wordDao.getDifficultyCount(selectedCategory, subcategory, difficulty);
        }

        return difficultyCount;
    }


    private void handleSelectedCategory(final String category) {
        performDatabaseOperation(() -> {
            Word firstWord = wordDao.getFirstWordForCategory(category);
            if (firstWord != null) {
                runOnUiThread(() -> {
                    updateSelections(category, firstWord.getSubcategory(), firstWord.getDifficulty());
                    showNextFlashcard();
                    saveSelectionsToPrefs();
                });
            }
        });
    }

    private void saveSelectionsToPrefs() {
        SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
        editor.putString("lastCategory", selectedCategory);
        editor.putString("lastSubcat", subcategory);
        editor.putString("lastDifficulty", difficulty);
        editor.apply();
    }

    private void updateSelections(String category, String subcat, String diff) {
        this.selectedCategory = category;
        this.subcategory = subcat;
        this.difficulty = diff;
    }
    private void handleSelectedSubcat(String subcat) {
        performDatabaseOperation(() -> {
            Word firstWord = wordDao.getFirstWordForSubcategory(subcat);
            if (firstWord != null) {
                runOnUiThread(() -> {
                    updateSelections(selectedCategory, subcat, firstWord.getDifficulty());
                    updateButtonStates(difficulty);
                    showNextFlashcard();
                    saveSelectionsToPrefs();
                });
            }
        });
    }

    private void handleSelectedDifficulty(String selectedDifficulty) {
        difficulty = selectedDifficulty;
        updateCatSubcatDiffLabelsandCounts();
        updateButtonStates(selectedDifficulty);
        updateUI();
        refreshBasedOnCurrentSelections();
    }

    private void refreshBasedOnCurrentSelections() {

        if ("All".equals(difficulty)) {
            loadWordsIfNeeded();
        } else {
            loadWordsForSpecificDifficulty();
        }
    }
    private void loadWordsForSpecificDifficulty() {
        executor.execute(() -> {
            wordsList = wordDao.getAllWordsForCategorySubcatAndDifficulty(selectedCategory, subcategory, difficulty);
            //currentIndex = 0;
        });
    }


    private void playAudio(String audioFilePath) {
        if (audioFilePath == null) {
            Log.d("FLAG", "audioFilePath is null");
            //setAudioCheckButtonColor(2131099734);
            audioCheckButton.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_red));

            return;
        }

        File file = new File(audioFilePath);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (file.length() != 0L) {
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                });
                mediaPlayer.setDataSource(audioFilePath);
                mediaPlayer.prepare();
                mediaPlayer.start();

                Log.d("FLAG", "Seems it found the word @...: " + audioFilePath + "File Size: " + file.length());
                //setAudioCheckButtonColor(2131099733);
                audioCheckButton.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_green));

                return;
            } catch (IOException e) {
                Log.e("AUDIO PLAYBACK", "Failed to start audio playback", e);
            }
        }
        //setAudioCheckButtonColor(2131099734);
        audioCheckButton.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_red));

    }

    private void setAudioCheckButtonColor(int colorResId) {
        int color = ContextCompat.getColor(this, colorResId);
        runOnUiThread(() -> audioCheckButton.setBackgroundColor(color));
    }

    private void fetchDataAndSetupUI() {
        executor.execute(() -> {
            this.allListWords = wordDao.getAllListWords();

            runOnUiThread(() -> {
                if (!checkForDefaultRecord())
                    addDefaultRecord();
                goToFirstRecord();
            });
        });
    }


    private void setupCardFlip(View container, final View frontView, final View backView, final BooleanHolder isFlippedHolder) {
        // Initial setup based on the current status of the card
        if (!isFlippedHolder.value) {
            frontView.setVisibility(View.INVISIBLE);
            backView.setVisibility(View.VISIBLE);
            frontView.setScaleX(-1);   // Mirrored
            container.setRotationY(180.0F);
        } else {
            frontView.setVisibility(View.VISIBLE);
            backView.setVisibility(View.INVISIBLE);
            container.setRotationY(0.0F);
        }

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObjectAnimator firstHalfAnimator;
                ObjectAnimator secondHalfAnimator;
                if (!isFlippedHolder.value) {
                    firstHalfAnimator = ObjectAnimator.ofFloat(v, "rotationY", 0f, 90f);
                    secondHalfAnimator = ObjectAnimator.ofFloat(v, "rotationY", 90f, 180f);
                } else {
                    firstHalfAnimator = ObjectAnimator.ofFloat(v, "rotationY", 180f, 90f);
                    secondHalfAnimator = ObjectAnimator.ofFloat(v, "rotationY", 90f, 0f);
                }
                setupAnimator(firstHalfAnimator, secondHalfAnimator, frontView, backView, isFlippedHolder);
                firstHalfAnimator.start();
            }

        });
    }

    private void setupAnimator(ObjectAnimator firstAnimator, ObjectAnimator secondAnimator,
                               final View frontView, final View backView,
                               final BooleanHolder isFlippedHolder) {

        final long DURATION = 150L;
        Interpolator interpolator = new AccelerateDecelerateInterpolator();

        firstAnimator.setDuration(DURATION);
        secondAnimator.setDuration(DURATION);

        firstAnimator.setInterpolator(interpolator);
        secondAnimator.setInterpolator(interpolator);

        firstAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                secondAnimator.start();
            }
        });

        secondAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isFlippedHolder.value) {
                    frontView.setVisibility(View.VISIBLE);
                    backView.setVisibility(View.GONE);
                } else {
                    frontView.setVisibility(View.GONE);
                    backView.setVisibility(View.VISIBLE);
                }
                isFlippedHolder.value = !isFlippedHolder.value;
            }
        });
    }

    //private void showNextFlashcard() {
    //    showNextFlashcard(selectedCategory, subcategory, difficulty);
    //}

    private void showNextFlashcard() {
        Log.d("FLAG", "Fetching next flashcard for - Category: " + selectedCategory + ", Subcategory: " + subcategory + ", Difficulty: " + difficulty);

        fetchWord();
        // No need to handle the nextWord here, as it will be managed within the fetchWord method.
    }
    private void fetchWord() {
        Log.d("FLAG", "fetchWord called with difficulty: " + difficulty + ", currentIndex: " + currentIndex);

        executor.execute(() -> {
            Word word = null;

            // If the difficulty is "All", we fetch from the preloaded list
            if ("All".equals(difficulty)) {
                Log.d("FLAG", "fetchWord called with difficulty: " + difficulty + ", currentIndex: " + currentIndex + ", Wordlist size: " + wordsList.size());

                if (currentIndex > wordsList.size() - 1) {
                    currentIndex = 0;  // Reset index if at the end of list
                }
                word = wordsList.get(currentIndex++);
            }
            // For specific difficulties, query the next unseen word from the database
            else {
                word = wordDao.getNextWordNotIncludingId(difficulty, selectedCategory, subcategory, lastWordId);
                if (word != null) {
                    lastWordId = word.getId();
                }
            }

            // Switch back to the main thread to update the UI
            final Word finalWord = word;  // Need a final variable to be accessed inside the runOnUiThread
            runOnUiThread(() -> {
                Log.d("FLAG", "Displaying word on UI: " + finalWord);
                displayNextWord(finalWord);
            });
        });
    }


    private void displayNextWord(Word word) {
        if(word != null) {
            processFoundWord(word);
            // Your logic to display the word
        } else {
            displayEmptyWordMessage();
            // Logic if no word is returned
        }
    }
    private void performDatabaseOperation(Runnable operation) {
        new Thread(operation).start();
    }
    private void processFoundWord(Word word) {
        executor.execute(() -> {
            // Update the word in the database
            wordDao.updateWord(word);

            // Compute the audio file path and check for the existence of the file
            String audioFilePath = getAudioFilePath(word.getItem());
            File audioFile = new File(audioFilePath);

            // Switch back to the main thread to update the UI
            runOnUiThread(() -> {
                updateAudioButtonColor(audioFile);
                currentWord = word;
                updateUIComponents();
            });
        });
    }

    private void displayEmptyWordMessage() {
        runOnUiThread(() -> {
            frontTextView.setText("No word");
            backTextView.setText("For This Category");
            detailsTextView.setText("Please choose another difficulty or Random");
        });
    }

    // Retrieve Preferences
    private void retrievePreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        selectedCategory = sharedPreferences.getString(KEY_LAST_CATEGORY, "Category");
        subcategory = sharedPreferences.getString(KEY_LAST_SUBCAT, "Subcategory");
        difficulty = sharedPreferences.getString(KEY_LAST_DIFFICULTY, "Difficulty");
        updateUI();
    }

    private void updateButtonLabels() {
        buttonCategory.setText(selectedCategory + " (" + categoryCount + ")");
        buttonSubcat.setText(subcategory + " (" + subcategoryCount + ")");
        buttonDifficulty.setText(difficulty + " (" + difficultyCount + ")");
    }

    private void updateUI() {
        updateButtonLabels();
        //buttonSubcat.setEnabled(categoryToSubcatMap.containsKey(selectedCategory));
        buttonDifficulty.setEnabled(categorySubcatToDifficultyMap.containsKey(new Pair<>(selectedCategory, subcategory)));
        updateUIComponents();
    }

    private void updateUIComponents() {
        updateCatSubcatDiffLabelsandCounts();
        updateWordDisplay(currentWord);
    }

    // Save Preferences
    private void savePreferences() {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_LAST_CATEGORY, selectedCategory);
        editor.putString(KEY_LAST_SUBCAT, subcategory);
        editor.putString(KEY_LAST_DIFFICULTY, difficulty);
        editor.apply();
    }

    // Generalized Dialog Method
    private <T> void showSelectionDialog(String title, List<T> items, Consumer<T> onItemSelected) {
        CharSequence[] itemsArray = items.stream()
                .filter(Objects::nonNull)  // This filters out null values
                .map(Object::toString)
                .toArray(CharSequence[]::new);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(itemsArray, -1, (dialog, which) -> {
                    onItemSelected.accept(items.get(which)); // Directly use items list here
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    // Use the Generalized Dialog Method for Category, Subcategory, and Difficulty
    private void showCategorySelectionDialog() {
        showSelectionDialog("Choose a Category", categoriesList, this::handleSelectedCategory);
    }

    private void showSubcatSelectionDialog() {
        List<String> subcatList = categoryToSubcatMap.getOrDefault(selectedCategory, new ArrayList<>());
        showSelectionDialog("Choose a Subcategory", subcatList, this::handleSelectedSubcat);
    }

    private void showDifficultySelectionDialog() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            refreshDataFromDAO();
            // Ensure that "All" is at the top of the list
            List<String> difficultyList = new ArrayList<>();
            difficultyList.add("All");
            difficultyList.addAll(categorySubcatToDifficultyMap.getOrDefault(new Pair<>(selectedCategory, subcategory), new ArrayList<>()));

            runOnUiThread(() -> {
                showSelectionDialog("Choose a Difficulty", difficultyList, this::handleSelectedDifficulty);
            });
        });
    }


    private void refreshDataFromDAO() {
        // Here, fetch the relevant data from the DAO and update categorySubcatToDifficultyMap
        // For instance, if you have a DAO method that fetches the difficulties for a specific category and subcategory:
        List<String> difficulties = wordDao.getDifficultiesForCategoryAndSubcat(selectedCategory, subcategory);
        categorySubcatToDifficultyMap.put(new Pair<>(selectedCategory, subcategory), difficulties);
    }

    private void loadMappingsFromDatabase() {
        new Thread(() -> {
            try {
                List<String> categories = db.wordDao().getAllCategories();
                for (String category : categories) {
                    List<String> subcategories = db.wordDao().getSubcategoriesForCategory(category);
                    categoryToSubcatMap.put(category, subcategories);
                    for (String subcategory : subcategories) {
                        List<String> difficulties = db.wordDao().getDifficultiesForCategoryAndSubcat(category, subcategory);
                        categorySubcatToDifficultyMap.put(new Pair<>(category, subcategory), difficulties);
                    }
                }

                // Now, update the UI on the main thread.
                runOnUiThread(this::updateUI);
            } catch (Exception e) {
                Log.e("FLAG", "Error while loading mappings from database: ", e);
            }
        }).start();
    }
    private void startRecording() {
        if (isInvalidCurrentWord()) return;

        String audioFilePath = getAudioFilePath(currentWord.getItem());
        File audioFile = new File(audioFilePath);

        if (!audioFile.exists()) {
            createNewAudioFile(audioFile);
        }

        handleAudioFile(audioFile, audioFilePath);
    }

    private boolean isInvalidCurrentWord() {
        if (currentWord == null || currentWord.getItem() == null) {
            Log.d("FLAG", "Invalid current word");
            return true;
        }
        return false;
    }

    private void createNewAudioFile(File audioFile) {
        try {
            if (audioFile.createNewFile()) {
                Log.d("FLAG", "File created @" + audioFile.getPath());
            } else {
                Log.d("FLAG", "File not created, something went wrong");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAudioFile(File audioFile, String audioFilePath) {
        if (audioFile.length() == 0L) {
            startRecordingToFile(audioFilePath);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("File exists")
                    .setMessage("A file with the same name already exists. Do you want to overwrite it?")
                    .setPositiveButton("Yes", (dialog, which) -> startRecordingToFile(audioFilePath))
                    .setNegativeButton("No", null)
                    .show();
        }
    }
    private void handlePermissionAndRecord(String audioFilePath) {
        if (hasRecordAudioPermission()) {
            startRecordingToFile(audioFilePath);
        } else {
            requestRecordAudioPermission();
        }
    }

    private void startRecordingToFile(String audioFilePath) {

        Log.d("FLAG", "startRecordingToFile called with filePath: " + audioFilePath);
        this.isRecording = true;
        Button button = (Button)findViewById(R.id.record_button);
        button.setBackgroundColor(ContextCompat.getColor((Context)this, R.color.dark_green));
        MediaRecorder mediaRecorder2 = this.recorder;
        if (mediaRecorder2 != null) {
            mediaRecorder2.reset();
            this.recorder.release();
            this.recorder = null;
        }
        File file = new File((new File(audioFilePath)).getParentFile().getAbsolutePath());
        Log.d("FLAG", "FLAG Crashing here?");
        if (!file.exists() && !file.mkdirs()) {
            Log.e("FLAG", "Failed to create directory: " + file.getAbsolutePath());
            return;
        }
        MediaRecorder mediaRecorder1 = new MediaRecorder();
        this.recorder = mediaRecorder1;
        mediaRecorder1.setAudioSource(MediaRecorder.AudioSource.MIC);
        this.recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        this.recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        this.recorder.setOutputFile(audioFilePath);
        try {
            this.recorder.prepare();
            this.recorder.start();
            Toast toast = Toast.makeText((Context)this, "Recording Started", Toast.LENGTH_SHORT);
            toast.setGravity(49, 0, 700);
            toast.show();
        } catch (IOException iOException) {
            Log.e("FLAG", "Preparing or starting recorder failed", iOException);
            this.isRecording = false;
            button.setBackgroundColor(ContextCompat.getColor((Context)this, R.color.dark_red));
        }
    }

    private void stopRecording() {
        if (this.isRecording) {
            MediaRecorder mediaRecorder = this.recorder;
            if (mediaRecorder != null) {
                this.isRecording = false;
                mediaRecorder.stop();
                this.recorder.release();
                this.recorder = null;
                ((Button)findViewById(R.id.record_button)).setBackgroundColor(ContextCompat.getColor((Context)this, R.color.dark_red));
                Toast toast = Toast.makeText((Context)this, "Recording Stopped", Toast.LENGTH_SHORT);
                toast.setGravity(49, 0, 700);
                toast.show();
            }
        }
    }

    private void toggleRecording() {
        Log.d("FLAG", "toggleRecording called");
        if (this.isRecording) {
            Log.d("FLAG", "isRecording true so should go to Stop");
            stopRecording();
        } else {
            Log.d("FLAG", "isRecording false so should go to Start");
            startRecording();
        }
    }

    private void updateAudioButtonColor(File paramFile) {
        if (paramFile != null && paramFile.length() != 0L) {
            this.audioCheckButton.setBackgroundColor(ContextCompat.getColor((Context)this, R.color.dark_green));
            this.audioCheckButton.setText("Audio Exists");
        } else {
            this.audioCheckButton.setBackgroundColor(ContextCompat.getColor((Context)this, R.color.dark_red));
            this.audioCheckButton.setText("No Audio");
        }
    }

    private void updateCatSubcatDiffLabelsandCounts() {
        new Thread(() -> {
            int categoryCount = getSelectedCategoryCount();
            int subcategoryCount = getSelectedSubcategoryCount();
            int difficultyCount = getSelectedDifficultyCount();

            runOnUiThread(() -> {
                updateCategoryCountUI(categoryCount);
                updateSubcategoryCountUI(subcategoryCount);
                updatedifficultyCountUI(difficultyCount);
            });
        }, "UpdateLabelsAndCountsThread").start();
    }

    private void updateCategoryCountUI(int categoryCount) {
        this.buttonCategory.setText(this.selectedCategory + " (" + categoryCount + ")");
    }

    private void updateSubcategoryCountUI(int subcategoryCount) {
        this.buttonSubcat.setText(this.subcategory + " (" + subcategoryCount + ")");
    }

    private void updatedifficultyCountUI(int difficultyCount) {
        this.buttonDifficulty.setText(this.difficulty + " (" + difficultyCount + ")");
    }

    private void updateWordDisplay(Word word) {
        Log.d("FLAG", "DisplayWord Words: " + word);
        //loadWordsIfNeeded();

        if (word == null) {
            Log.d("FLAG", "updateWordDisplay Words: word should be updating " + word);
            clearWordDisplay();
            return;
        }
        Log.d("FLAG", "DisplayWord Words: should be updating text fields " + word);

        this.detailsTextView.setText(word.getDetails());
        this.backTextView.setText(word.getDescription());
        this.frontTextView.setText(word.getItem());
        this.frontTextView.invalidate();

        updateCatSubcatDiffLabelsandCounts();

        loadImageForWord(word);
    }

    private void clearWordDisplay() {
        this.backTextView.setText("");
        this.detailsTextView.setText("");
        this.frontTextView.setText("");
        Glide.with((FragmentActivity) this).load(R.drawable.smartflashnoimage).into(this.imageImageView);
        Log.d("FLAG", "Displayword setting null records: ");
    }

    protected void onActivityResult(int paramInt1, int paramInt2, Intent paramIntent) {
        super.onActivityResult(paramInt1, paramInt2, paramIntent);
        if (paramInt1 == 1 && paramInt2 == -1)
            paramIntent.getStringExtra("Word");
    }

    /*
    public void onBackPressed() {
        if (this.drawer.isDrawerOpen(8388611)) {
            this.drawer.closeDrawer(8388611);
        } else {
            super.onBackPressed();
        }
    }
*/

    private void initializeUIComponents() {
        this.externalDir = getExternalFilesDir(null);

        // ... other UI components initialization ...
        // Note: Consider removing duplicate initializations (e.g., buttonCategory is initialized multiple times).
    }

    private void handleFirebaseUser(Bundle paramBundle) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            // ... handle Firebase user ...
        } else {
            startActivity(new Intent((Context) this, RegisterActivity.class));
            finish();
        }

        if (paramBundle != null) {
            int i = paramBundle.getInt("CurrentRecordIndex");
            this.currentRecordIndex = i;
            goToRecord(i);
        } else {
            if (!checkForDefaultRecord())
                addDefaultRecord();
            goToFirstRecord();
        }
        // ... more logic related to Firebase user ...
    }

    protected void onDestroy() {
        super.onDestroy();
        MediaPlayer mediaPlayer = this.mediaPlayer;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            this.mediaPlayer = null;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("DEBUG", "Item clicked: " + item.getItemId());

        // This will check if the clicked item was the hamburger icon
        if (toggle.onOptionsItemSelected(item)) {
            Log.d("DEBUG", "Toggle handled the click");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_edit_db) {
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
        } else if (id == R.id.nav_editai_activity) {
            // handle action for 'Edit AI Cards'
        } else if (id == R.id.nav_getpaipairs_activity) {
            Intent intent = new Intent(this, CardPairListActivity.class);
            startActivity(intent);
        }else if (id == R.id.user_registration) {
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

    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", 0).edit();
        editor.putInt("currentRecordIndex", this.currentRecordIndex);
        editor.apply();
    }

    protected void onResume() {
        super.onResume();
        this.currentRecordIndex = getSharedPreferences("MyPrefs", 0).getInt("currentRecordIndex", 0);
        Log.d("FLAG", "onResume - currentRecordIndex: " + this.currentRecordIndex);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String str1 = firebaseUser.getEmail();
            if (str1 != null && str1.length() >= 2) {
                str1 = str1.substring(0, 2);
                Log.d("FLAG", "onResume - firstTwoLetters: " + str1);
                ((TextView)findViewById(R.id.icon_text_view)).setText(str1.toUpperCase());
                String str2 = firebaseUser.getDisplayName();
                Log.d("FLAG", "onResume - user.getDisplayName: " + str2);
            }
        }

        initializeDatabase(); //need to do this when return to the main card screen tio make sure that any new cards added are searchable
        setupImages();
        retrievePreferences();
        initializeDataObservers();
        handleSelectedCategory(selectedCategory);
        updateUI();
    }

    private void setImageToView(ImageView imageView, String filename) {
        if (imageView != null) {
            File directory = getExternalFilesDir(null);
            if (directory == null) {
                Log.e("FLAG", "Error: External storage directory not accessible.");
                return;
            }

            File file = new File(directory, filename);
            if (file.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                imageView.setImageBitmap(myBitmap);
            } else {
                Log.d("FLAG", filename + " doesn't exist");
            }
        } else {
            Log.d("FLAG", filename + " ImageView is null");
        }
    }

    public void setupImages() {
        setImageToView(imageViewPart1, "part1.png");
        setImageToView(imageViewPart2, "part2.png");
        setImageToView(imageViewPart3, "part3.png");
        setImageToView(imageViewPart4, "part4.png");
    }


    public void onSaveInstanceState(Bundle paramBundle) {
        super.onSaveInstanceState(paramBundle);
        paramBundle.putInt("CurrentRecordIndex", this.currentRecordIndex);
    }

    public void onTaskCompleted() {
        updateButtonStates(difficulty);
        Log.d("FLAG", "showNextFlashcard 1: " + difficulty + selectedCategory + subcategory);
        showNextFlashcard();
    }

    private void showSnackbar(String snackbarText) {
        View contextView = findViewById(android.R.id.content); // use the root view
        Snackbar snackbar = Snackbar.make(contextView, snackbarText, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public void updateButtonStates(String difficulty) {
        resetButtonsToDefault();

        if ("All".equals(difficulty)) {
            String currentWordDifficulty = currentWord.getDifficulty();
            // Set the color of the button that matches the currentWordDifficulty to green
            setButtonToSelected(currentWordDifficulty);
        } else {
            setButtonToSelected(difficulty);
        }
    }

    private void resetButtonsToDefault() {
        int defaultColor = ContextCompat.getColor(this, R.color.dark_red);
        btnEasy.setBackgroundColor(defaultColor);
        btnMedium.setBackgroundColor(defaultColor);
        btnHard.setBackgroundColor(defaultColor);
    }

    private void setAllButtonsToSelected() {
        int selectedColor = ContextCompat.getColor(this, R.color.dark_green);
        btnEasy.setBackgroundColor(selectedColor);
        btnMedium.setBackgroundColor(selectedColor);
        btnHard.setBackgroundColor(selectedColor);
    }

    private void setButtonToSelected(String difficulty) {
        int selectedColor = ContextCompat.getColor(this, R.color.dark_green);
        Button targetButton = getButtonForDifficulty(difficulty);
        if (targetButton != null) {
            targetButton.setBackgroundColor(selectedColor);
        }
    }

    private Button getButtonForDifficulty(String difficulty) {
        Map<String, Button> difficultyButtonMap = new HashMap<>();
        difficultyButtonMap.put("Easy", btnEasy);
        difficultyButtonMap.put("Medium", btnMedium);
        difficultyButtonMap.put("Hard", btnHard);

        return difficultyButtonMap.get(difficulty);
    }
    private void checkAuthenticationStatus() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, navigate to main app screen
            navigateToMainAppScreen();
        } else {
            // Try auto login or navigate to registration
            tryAutoLoginOrRegister();
        }
    }
    private Pair<String, String> getSavedCredentials() {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER_NAME, MODE_PRIVATE);
        String email = prefs.getString(PREF_EMAIL, "");
        String password = prefs.getString(PREF_PASSWORD, "");
        return new Pair<>(email, password);
    }

    private void tryAutoLoginOrRegister() {
        Pair<String, String> credentials = getSavedCredentials();
        String savedEmail = credentials.first;
        String savedPassword = credentials.second;

        if (TextUtils.isEmpty(savedEmail) || TextUtils.isEmpty(savedPassword)) {
            navigateToRegistrationScreen();
            return;
        }

        // Try to sign in with stored credentials
        auth.signInWithEmailAndPassword(savedEmail, savedPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Auto login successful, navigate to main app screen
                        navigateToMainAppScreen();
                    } else {
                        // Auto login failed, navigate to registration screen
                        navigateToRegistrationScreen();
                    }
                });
    }

    private void navigateToMainAppScreen() {
        // Navigation code to main app screen
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    private void navigateToRegistrationScreen() {
        // Navigation code to registration screen
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        finish();
    }

    public static class BooleanHolder {
        public boolean value;

        public BooleanHolder(boolean param1Boolean) {
            this.value = param1Boolean;
        }
    }
}
