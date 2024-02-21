package au.smartflash.smartflash;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.room.Room;

import com.google.android.gms.common.util.IOUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//import au.smartflash.smartflash.db.AIDatabase;
import au.smartflash.smartflash.db.AppDatabase;
import au.smartflash.smartflash.model.AICard;
import au.smartflash.smartflash.model.CategorySubcategoryPair;
import au.smartflash.smartflash.model.Word;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import android.os.Environment;


public class GetAICardsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AppDatabase appDb;
    private List<CategorySubcategoryPair> categorySubcategoryPairs;
    private OkHttpClient client;
    private int currentRetryCount = 0;
    private FirebaseFirestore db;
    EditText editToLanguage;
    private EditText editgetAICategory, editgetAISubcat;
    private ScheduledExecutorService executorService;
    private Button fetchDataButton, btnHome;
    Button informationButton;
    Button languageButton;
    private AppDatabase localdb;
    //private Handler messageHandler = new Handler();
    //private int messageIndex = 0;
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private AlertDialog progressDialog;
    private Button requestCardsButton;
    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private Spinner spinnerNumCards;
    private static final int MESSAGE_DELAY_MILLIS = 5000; // 5 seconds delay
    private int messageIndex = 0;
    private final Handler messageHandler = new Handler(Looper.getMainLooper());
    private String textOption = "Information"; // default value
    // Create a logging interceptor at the class level
    private HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();

    private final Runnable statusChecker = new Runnable() {
        @Override
        public void run() {
            updateProgressDialogMessage();
            messageHandler.postDelayed(statusChecker, MESSAGE_DELAY_MILLIS);
        }
    };
    private final List<String> waitingMessages = Arrays.asList(
            "Cards are being prepared by OpenAI",
            "Hold tight while OpenAI does its magic!",
            "This may take a few minutes",
            "You can click anywhere and come back later to download card pairs"
            // Add more messages as needed
    );
    private AppDatabase localDb;
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
        setContentView(R.layout.activity_get_aicards);

        initializeDatabase();
        setupViews();
        setupListeners();
        initializeClient();
    }

    private void initializeDatabase() {
        if (this.localDb == null) {
            this.localDb = Room.databaseBuilder(
                    getApplicationContext(),
                    AppDatabase.class,
                    "SMARTFLASHDB.sqlite"
            ).fallbackToDestructiveMigration().build();
        }
    }
    private void setupViews() {
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

        editgetAICategory = findViewById(R.id.editgetAICategory);
        editgetAISubcat = findViewById(R.id.editgetAISubcat);
        spinnerNumCards = findViewById(R.id.spinnerNumCards);
        editToLanguage = findViewById(R.id.editToLanguage);

        informationButton = findViewById(R.id.informationButton);
        languageButton = findViewById(R.id.languageButton);

        requestCardsButton = findViewById(R.id.requestCardsButton);
        requestCardsButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));

        fetchDataButton = findViewById(R.id.fetchDataButton);
        fetchDataButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));

        btnHome = findViewById(R.id.buttongetAIHome);
        btnHome.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));

        Button btnSync = findViewById(R.id.syncCardsButton);
        btnSync.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));

        btnSync.setOnClickListener(v -> {
            //test_gcs_upload();
            showSyncConfirmationDialog(btnSync);
        });

        setInitialButtonState();
        setupSpinner();
    }
    // Method to show progress dialog and start the sync process
    private void showSyncConfirmationDialog(Button syncButton) {
        new AlertDialog.Builder(this)
                .setTitle("Sync to Cloud")
                .setMessage("WARNING - any changes made Locally using this app will overwrite the Cloud Card including any updated images. Do you wish to proceed?")
                .setPositiveButton("Yes", (dialog, which) -> showSyncProgressDialog(syncButton))
                .setNegativeButton("No", null)
                .show();
    }

    private void showSyncProgressDialog(Button syncButton) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Syncing local data to cloud...");
        progressDialog.setCancelable(false); // set to true if you want to allow cancel
        progressDialog.show();

        // Start the sync process
        startSyncProcess(progressDialog, syncButton);
    }

    private void startSyncProcess(ProgressDialog progressDialog, Button syncButton) {
        new Thread(() -> {
            if (localDb == null) {
                initializeDatabase();
            }

            List<Word> localWords = localDb.wordDao().getAllWords();
            AtomicInteger wordsProcessed = new AtomicInteger();

            for (Word word : localWords) {
                // Directly sync each word to the cloud without checking for duplicates
                uploadImageToGCSAndCreateDocument(word, () -> {
                    if (wordsProcessed.incrementAndGet() == localWords.size()) {
                        runOnUiThread(() -> {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            syncButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.dark_green));
                        });
                    }
                });
            }
        }).start();
    }
    private void checkForDuplicatesAndSync(Word word, Runnable onComplete) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("cards")
                .whereEqualTo("Item", word.getItem())
                .whereEqualTo("Category", word.getCategory())
                .whereEqualTo("Subcategory", word.getSubcategory())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        uploadImageToGCSAndCreateDocument(word, onComplete);
                    } else {
                        Log.d("FLAG", "Duplicate found, skipping: " + word.getItem());
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FLAG", "Error checking duplicates", e);
                    onComplete.run();
                });
    }

    private void uploadImageToGCSAndCreateDocument(Word word, Runnable onComplete) {
        File imageFile = getImageFile(word);
        if (imageFile.exists()) {
            uploadImageToGCS(imageFile, word, onComplete);
        } else {
            Log.d("FLAG", "Image file does not exist, proceeding without image for word: " + word.getItem());
            createFirestoreDocument(word, null, onComplete);
        }
    }

    private File getImageFile(Word word) {
        return new File(getExternalFilesDir(null), "images/" + word.getCategory() + "/" + word.getSubcategory() + "/" + word.getItem() + ".png");
    }

    private void test_gcs_upload() {
        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageRef = firebaseStorage.getReference();

        // Replace with your actual file path
        File imagePath = new File(getExternalFilesDir(null), "/Images/Fish/Tropical");
        File newFile = new File(imagePath, "Goldfish.png");

        if (!newFile.exists()) {
            Log.e("UploadTest", "File does not exist: " + newFile.getAbsolutePath());
            return;
        }

        Uri fileUri = Uri.fromFile(newFile);
        Log.d("UploadTest", "File URI: " + fileUri.toString());

        StorageReference fileRef = storageRef.child("/images/AAA/" + fileUri.getLastPathSegment());
        Log.d("UploadTest", "Firebase Storage Path: " + fileRef.getPath());

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> Log.d("UploadTest", "Upload successful"))
                .addOnFailureListener(e -> {
                    Log.e("UploadTest", "Upload failed", e);
                    // Log more detailed information about the exception
                    Log.e("UploadTest", "Exception details: ", e);
                    if (e instanceof StorageException) {
                        StorageException storageException = (StorageException) e;
                        Log.e("UploadTest", "Error Code: " + storageException.getErrorCode());
                        Log.e("UploadTest", "HTTP Result Code: " + storageException.getHttpResultCode());
                        Log.e("UploadTest", "Detailed Error Message: " + storageException.getMessage());
                    }
                });
    }

    private void uploadImageToGCS(File imageFile, Word word, Runnable onComplete) {
        if (!imageFile.exists() || !imageFile.canRead()) {
            Log.e("FLAG", "Image file does not exist or cannot be read: " + imageFile.getAbsolutePath());
            onComplete.run();
            return;
        }

        // Initialize Firebase, if not already initialized
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        // Constructing the GCS image path
        String gcsImagePath = "images/" + word.getCategory() + "/" +
                word.getSubcategory() + "/" +
                word.getItem() + ".png";

        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference imageRef = storageRef.child(gcsImagePath);

        Uri fileUri = Uri.fromFile(imageFile);
        Log.d("FLAG", "File URI: " + fileUri.toString());
        Log.d("FLAG", "Firebase Storage Path: " + imageRef.getPath());

        imageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("FLAG", "Upload successful");
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        Log.d("FLAG", "Image uploaded successfully: " + downloadUri);
                        createFirestoreDocument(word, downloadUri.toString(), onComplete);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FLAG", "Upload failed", e);
                    // Log more detailed information about the exception
                    Log.e("FLAG", "Exception details: ", e);
                    if (e instanceof StorageException) {
                        StorageException storageException = (StorageException) e;
                        Log.e("FLAG", "Error Code: " + storageException.getErrorCode());
                        Log.e("FLAG", "HTTP Result Code: " + storageException.getHttpResultCode());
                        Log.e("FLAG", "Detailed Error Message: " + storageException.getMessage());
                    }
                    onComplete.run();
                });
    }

    private String normalizePath(String pathComponent) {
        return pathComponent.replaceAll("\\s+", "_");
    }

    private byte[] readImageFileToByteArray(File file) {
        try (InputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteStream.write(buffer, 0, len);
            }
            return byteStream.toByteArray();
        } catch (IOException e) {
            Log.e("FLAG", "Error reading image file", e);
            return null;
        }
    }
    private void createFirestoreDocument(Word word, String imageUrl, Runnable onComplete) {
        // Get the current Firebase User
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("FLAG", "No signed-in user found.");
            onComplete.run();
            return;
        }

        // Get email and username (username might be part of the displayName or a separate field)
        String email = user.getEmail();
        String username = user.getDisplayName(); // This could be null if not set

        Map<String, Object> wordData = new HashMap<>();
        wordData.put("Category", word.getCategory());
        wordData.put("Subcategory", word.getSubcategory());
        wordData.put("Item", word.getItem());
        wordData.put("Description", word.getDescription());
        wordData.put("Details", word.getDetails());
        wordData.put("Difficulty", word.getDifficulty());
        wordData.put("Image", imageUrl);
        wordData.put("Email", email);
        wordData.put("Username", username != null ? username : "Unknown");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("cards").add(wordData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FLAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("FLAG", "Error adding document", e);
                    onComplete.run();
                });
    }

    private void setInitialButtonState() {
        toggleButtonColors(informationButton, languageButton, R.color.dark_green, R.color.dark_red);
        editToLanguage.setEnabled(false);
        editToLanguage.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray));
        editToLanguage.setTextColor(ContextCompat.getColor(this, R.color.light_gray));
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> arrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.card_numbers)) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                // Set text color for selected item
                view.setTextColor(getResources().getColor(R.color.black));
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(getResources().getColor(R.color.black)); // Set text color for dropdown items
                view.setBackgroundColor(getResources().getColor(R.color.white)); // Set background color for dropdown items
                return view;
            }
        };
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNumCards.setAdapter(arrayAdapter);
        spinnerNumCards.setSelection(0, false);

    }

    private void setupListeners() {
        setupFocusChangeListeners();
        setupItemSelectedListener();
        setupButtonListeners();
    }

    private void setupFocusChangeListeners() {
        editgetAICategory.setOnFocusChangeListener((v, hasFocus) -> {
            editgetAICategory.setHint(hasFocus ? "" : getString(R.string.original_category_hint));
        });

        editgetAISubcat.setOnFocusChangeListener((v, hasFocus) -> {
            editgetAISubcat.setHint(hasFocus ? "" : getString(R.string.original_subcategory_hint));
        });
    }

    private void setupItemSelectedListener() {
        spinnerNumCards.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Your item selection logic here...
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where nothing is selected, if necessary
            }
        });
    }

    private void setupButtonListeners() {
        requestCardsButton.setOnClickListener(v -> {
            //requestCardsButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
            requestCards();
        });
        fetchDataButton.setOnClickListener(v -> {
            //fetchDataButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));
            navigateToCardPairListActivity();
        });
        btnHome.setOnClickListener(v -> {
            //btnHome.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));
            navigateToMainActivity();
        });

        informationButton.setOnClickListener(v -> onInformationButtonClick());
        languageButton.setOnClickListener(v -> onLanguageButtonClick());
    }
    private void onInformationButtonClick() {
        Log.d("FLAG", "Information Button Clicked");
        textOption = "Information";
        toggleButtonColors(informationButton, languageButton, R.color.dark_green, R.color.dark_red);
        editToLanguage.setEnabled(false);
        editToLanguage.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray));
        editToLanguage.setTextColor(ContextCompat.getColor(this, R.color.light_gray));
        editToLanguage.setHintTextColor(ContextCompat.getColor(this, R.color.light_gray)); // Set hint text color to light gray

        editgetAICategory.setHint("Choose Category"); // Set the hint text for Category
        editgetAISubcat.setHint("Choose Subcategory"); // Set the hint text for Category


        Log.d("FLAG", "Set text option to Information");
    }

    private void onLanguageButtonClick() {
        Log.d("FLAG", "Language Button Clicked");
        textOption = "Languages";
        toggleButtonColors(languageButton, informationButton, R.color.dark_green, R.color.dark_red);
        editToLanguage.setEnabled(true);
        editToLanguage.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        editToLanguage.setTextColor(ContextCompat.getColor(this, R.color.black));
        editToLanguage.setHintTextColor(ContextCompat.getColor(this, R.color.black)); // Set hint text color

        editgetAICategory.setHint("Choose from Language"); // Set the hint text for Category
        editgetAISubcat.setHint("Choose Category"); // Chang ethe hint for subcat to cat in language case.


        Log.d("FLAG", "Set text option to Languages");
    }

    private void toggleButtonColors(Button activeButton, Button inactiveButton, int activeColorResId, int inactiveColorResId) {
        activeButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, activeColorResId)));
        inactiveButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, inactiveColorResId)));
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(GetAICardsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToCardPairListActivity() {
        Intent intent = new Intent(GetAICardsActivity.this, CardPairListActivity.class);
        startActivity(intent);
    }

    private void initializeClient() {
        client = new OkHttpClient.Builder()
                .readTimeout(3L, TimeUnit.MINUTES)
                .connectTimeout(60L, TimeUnit.SECONDS)
                .build();
    }
    // Your methods like `configureButton`, `requestCards` and `fetchData` would go below
    private String taskId;
    private FirebaseUser user;
    String username = "";
    String email = "";


    private String latestPollingStatus = "Starting the preparation of cards..."; // Default initial status


    private AlertDialog createProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogue_progress, null);
        TextView messageTextView = view.findViewById(R.id.tv_progress_message);
        messageTextView.setText(waitingMessages.get(0)); // Set initial message

        builder.setView(view);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setCancelable(true);

        alertDialog.setOnCancelListener(dialog -> {
            // Handle the cancellation (e.g., stop polling, log a message, etc.)
            stopPolling();
            Log.d("ProgressDialog", "Dialog was cancelled by user.");
        });

        return alertDialog;
    }

    private boolean isPollingActive = false;

    private void stopPolling() {
        this.messageHandler.removeCallbacks(this.statusChecker);
    }

    private void fetchData() {
        if (this.db == null) this.db = FirebaseFirestore.getInstance();
        if (this.user == null) this.user = FirebaseAuth.getInstance().getCurrentUser();

        // Use AppDatabase instead of AIDatabase
        if (this.appDb == null) this.appDb = AppDatabase.getInstance(getApplicationContext());

        if (this.user != null) {
            String userEmail = user.getEmail(); // Retrieve user email
            if (userEmail != null && !userEmail.isEmpty()) {
                Query query = this.db.collection("cards").whereEqualTo("Email", userEmail).limit(100L);

                showProgressDialog(); // Start showing the progress dialog

                query.get().addOnCompleteListener(task -> {
                    stopPolling(); // Ensure to stop polling once data fetch is complete

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        List<AICard> aiCards = new ArrayList<>();

                        for (DocumentSnapshot doc : documents) {
                            AICard card = doc.toObject(AICard.class);
                            if (card != null) aiCards.add(card);
                        }

                        // Here, you can save these aiCards to your local Room database
                        new Thread(() -> {
                            appDb.aiCardDao().insertAll(aiCards.toArray(new AICard[0])); // Assuming you have an insertAll method in AICardDao
                        }).start();
                    }

                    // Handle any other onComplete logic if needed
                });
            } else {
                Toast.makeText(this, "Email address is not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "User not found. Please log in again!", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateJson(String email, String username, String category, String subcategory, int numCards, String cardType) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("username", username);
        data.put("category", category);
        data.put("subcategory", subcategory);
        data.put("num_cards", numCards);
        data.put("card_type", cardType);

        Log.d("FLAG", "request json textOption: " + cardType);
        return new Gson().toJson(data);
    }

    private boolean isNullOrBlank(String input) {
        return (input == null || input.trim().isEmpty());
    }

    private boolean isValidInput(String input, String errorMessage) {
        if (input.split(" ").length > 5) {
            showSnackbar(errorMessage);
            return false;
        }
        return true;
    }

    private boolean isValidNumber(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private void prepareAndSendRequest(String email, String username, String category, String subCategory, String textOption) {
        String numCardsSelected = this.spinnerNumCards.getSelectedItem().toString();
        // ... existing validation checks ...

        String jsonString = generateJson(email, username, category, subCategory, Integer.parseInt(numCardsSelected), textOption);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonString);

        showProgressDialog(); // Show progress dialog before sending the request
        sendServerRequest(requestBody);
    }

    private void requestCards() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("FLAG", "Requesting Cards - user: " + firebaseUser);
        if (firebaseUser == null) return;

        String userId = firebaseUser.getUid(); // Get the unique user ID
        String email = firebaseUser.getEmail(); // Get the user email
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "User email not available", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String userName = document.getString("username"); // Fetch the userName from Firestore
                    Log.d("FLAG", "Requesting Cards - user: " + userName + ", email: " + email);
                    continueRequest(email, userName);
                } else {
                    Toast.makeText(this, "User data not found in Firestore", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void continueRequest(String email, String userName) {
        Log.d("FLAG", "Continuing Request - User: " + userName + ", Email: " + email);
        String category = getTextFromEditText(R.id.editgetAICategory);
        String subCategory = getTextFromEditText(R.id.editgetAISubcat);
        String numCardsSelected = spinnerNumCards.getSelectedItem().toString();

        if (!isValidNumber(numCardsSelected)) {
            Toast.makeText(this, "Please select a valid number of cards.", Toast.LENGTH_SHORT).show();
            Log.d("FLAG", "Invalid number of cards selected");
            return;
        }
        Log.d("FLAG", "Text Option: " + textOption);

        if ("Information".equals(textOption)) {
            Log.d("FLAG", "Information option selected");
            // If Information is selected, use category and subcategory as they are
        } else if ("Languages".equals(textOption)) {
            Log.d("FLAG", "Languages option selected");
            // If Languages is selected, modify category
            category = "From " + category + " to " + getTextFromEditText(R.id.editToLanguage);
            if (isNullOrBlank(category) || isNullOrBlank(subCategory)) {
                showSnackbar("Please fill in both language fields.");
                Log.d("FLAG", "Language fields validation failed");
                return;
            }
        } else {
            showSnackbar("Invalid text option selected.");
            Log.d("FLAG", "Invalid text option selected");
            return;
        }

        if (!isValidInput(category, "Maximum 5 words allowed for Category") || !isValidInput(subCategory, "Maximum 5 words allowed for Subcategory")) {
            Log.d("FLAG", "Category or subcategory input validation failed");
            return;
        }

        Log.d("FLAG", "Preparing and sending request");
        prepareAndSendRequest(email, userName, category, subCategory, textOption);
    }

    private String getTextFromEditText(int editTextId) {
        EditText editText = findViewById(editTextId);
        if (editText != null) {
            return editText.getText().toString().trim();
        }
        return "";
    }

    private File saveDatabaseFile(InputStream inputStream) throws IOException {
        File file = new File(getFilesDir(), "database.db");
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return file;
    }

    private File saveImageToFile(byte[] imageData) {
        File directory = new File(getFilesDir(), "images");
        if (!directory.exists()) {
            directory.mkdir();
        }

        File imageFile = new File(directory, "image_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            outputStream.write(imageData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFile;
    }

    private void sendServerRequest(RequestBody requestBody) {
        Request request = new Request.Builder()
                .url("http://34.125.121.101:5000/run-script")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("FLAG", "Request failed", e);
                runOnUiThread(() -> {
                    dismissProgressDialog();
                    Toast.makeText(GetAICardsActivity.this, "Request failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    String status = extractStatus(responseBody);
                    latestPollingStatus = status;

                    if (status.equalsIgnoreCase("pending")) {
                        taskId = extractTaskId(responseBody);
                        if (taskId != null && !taskId.isEmpty()) {
                            pollingHandler.postDelayed(pollingRunnable, POLL_INTERVAL);
                        }
                    } else if (status.equalsIgnoreCase("SUCCESS")) {
                        runOnUiThread(() -> {
                            dismissProgressDialog();
                            navigateToCardPairListActivity();
                        });
                    }
                } else {
                    latestPollingStatus = "Error during initial request";
                    runOnUiThread(() -> updateProgressDialogMessage());
                }
            }

        });
    }
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (taskId != null && !taskId.isEmpty()) {
                String taskStatusUrl = "http://34.125.121.101:5000/task/" + taskId;
                Request request = new Request.Builder()
                        .url(taskStatusUrl)
                        .build();

                client.newCall(request).enqueue(pollingCallback);
            }
        }
    };


    //private void navigateToCardPairListActivity() {
   //     Intent intent = new Intent(GetAICardsActivity.this, CardPairListActivity.class);
    //    startActivity(intent);
    //}


    private String extractStatus(String responseBody) {
        // Extract status from responseBody
        try {
            JSONObject jsonObject = new JSONObject(responseBody);
            return jsonObject.optString("status", "UNKNOWN");
        } catch (JSONException e) {
            Log.e("ExtractStatus", "Failed to parse status", e);
            return "ERROR";
        }
    }

    private void updateProgressDialogMessage() {
        if (progressDialog != null && progressDialog.isShowing()) {
            TextView messageTextView = progressDialog.findViewById(R.id.tv_progress_message);
            if (messageTextView != null) {
                String newMessage = getNextProgressDialogMessage() + "\nStatus: " + latestPollingStatus;
                messageTextView.setText(newMessage);
            }
        }
        if (isPollingActive) {
            messageHandler.postDelayed(messageUpdateRunnable, 5000); // Schedule next update
        }
    }
     private void shutdownExecutorService() {
        if (this.executorService != null && !this.executorService.isShutdown()) {
            this.executorService.shutdown();
            try {
                if (!this.executorService.awaitTermination(10L, TimeUnit.SECONDS)) {
                    this.executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private final int POLL_INTERVAL = 5000; // Poll every 5 seconds

    private final Callback pollingCallback = new Callback() {
        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            Log.e("FLAG", "Failed to poll for task completion", e);
            latestPollingStatus = "Error: " + e.getMessage();
            pollingHandler.postDelayed(pollingRunnable, POLL_INTERVAL);
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Log.d("FLAG", "Polling response: " + responseBody);
                String status = extractStatus(responseBody);
                latestPollingStatus = status;

                if ("SUCCESS".equals(status)) {
                    runOnUiThread(() -> {
                        dismissProgressDialog();
                        navigateToCardPairListActivity();
                    });
                } else {
                    pollingHandler.postDelayed(pollingRunnable, POLL_INTERVAL);
                }
            } else {
                Log.e("FLAG", "Error during polling: " + response.message());
                latestPollingStatus = "Server Error: " + response.message();
                pollingHandler.postDelayed(pollingRunnable, POLL_INTERVAL);
            }
        }
    };

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = createProgressDialog();
            progressDialog.show();
            startMessageCycling(); // Start message cycling
        }
    }
    private void startMessageCycling() {
        if (!isPollingActive) {
            isPollingActive = true;
            messageHandler.postDelayed(messageUpdateRunnable, 5000); // Start with a delay of 5 seconds
        }
    }

    private void stopMessageCycling() {
        isPollingActive = false;
        messageHandler.removeCallbacks(messageUpdateRunnable);
    }

    private Runnable messageUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (progressDialog != null && progressDialog.isShowing()) {
                TextView messageTextView = progressDialog.findViewById(R.id.tv_progress_message);
                if (messageTextView != null) {
                    String newMessage = getNextProgressDialogMessage();
                    messageTextView.setText(newMessage);
                }
                if (isPollingActive) {
                    messageHandler.postDelayed(this, 5000); // Schedule next update after 5 seconds
                }
            }
        }
    };

    private String getNextProgressDialogMessage() {
        String message = waitingMessages.get(messageIndex) + "\nStatus: " + latestPollingStatus;
        messageIndex = (messageIndex + 1) % waitingMessages.size();
        return message;
    }
    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        stopMessageCycling(); // Stop cycling messages when dialog is dismissed
        stopPolling(); // Stop polling when dialog is dismissed
    }

    private String extractTaskId(String responseBody) {
        // Implement logic to extract the task ID from the response body
        // Example: assuming the response is JSON and has a field "task_id"
        try {
            JSONObject jsonObject = new JSONObject(responseBody);
            return jsonObject.optString("task_id", "");
        } catch (JSONException e) {
            Log.e("ExtractTaskId", "Failed to parse task ID", e);
            return "";
        }
    }

    private static final int GREEN_COLOR = -16711936;
    private static final int RED_COLOR = -65536;
    private static final float SNACKBAR_HEIGHT_PERCENTAGE = 0.3F;
    private static final int SNACKBAR_MARGIN = 10;

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        stylizeSnackbar(snackbar);
        snackbar.show();
    }

    private void stylizeSnackbar(Snackbar snackbar) {
        View view = snackbar.getView();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        int displayHeight = getWindowManager().getDefaultDisplay().getHeight();

        layoutParams.gravity = Gravity.CENTER;
        layoutParams.topMargin = (int) (displayHeight * SNACKBAR_HEIGHT_PERCENTAGE);
        layoutParams.setMargins(SNACKBAR_MARGIN, layoutParams.topMargin, SNACKBAR_MARGIN, SNACKBAR_MARGIN);
        view.setLayoutParams(layoutParams);

        TextView snackbarText = view.findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

        int color = ContextCompat.getColor(getApplicationContext(), R.color.light_blue);
        view.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutdownExecutorService();
    }
}