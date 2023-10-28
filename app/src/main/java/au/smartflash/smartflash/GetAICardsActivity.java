package au.smartflash.smartflash;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.stream.Collectors;

//import au.smartflash.smartflash.db.AIDatabase;
import au.smartflash.smartflash.db.AppDatabase;
import au.smartflash.smartflash.model.AICard;
import au.smartflash.smartflash.model.CategorySubcategoryPair;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;

public class GetAICardsActivity extends AppCompatActivity {
    private static final int MAX_RETRIES = 10;
    private static final long POLLING_INTERVAL = 10000L;
    private static final long POLLING_INTERVAL_MS = 8000L;
    private AppDatabase appDb;
    private List<CategorySubcategoryPair> categorySubcategoryPairs;
    private OkHttpClient client;
    private int currentRetryCount = 0;
    private FirebaseFirestore db;
    private EditText editAITextCategory;
    private EditText editAITextDescription;
    private EditText editAITextDetails;

    private EditText editAITextDifficulty;
    private EditText editAITextItem;
    private EditText editAITextSubcategory;
    EditText editToLanguage;
    private EditText editgetAICategory;
    private ScheduledExecutorService executorService;
    private Button fetchDataButton;
    Button informationButton;
    Button languageButton;
    private AppDatabase localdb;
    //private Handler messageHandler = new Handler();
    //private int messageIndex = 0;
    private Handler pollingHandler = new Handler();
    private AlertDialog progressDialog;
    private ProgressDialog progressDialogFetch;

    private Button requestCardsButton;
    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private Snackbar snackbarRequest;
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

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_get_aicards); // Your layout name

        initializeDatabase();
        setupViews();
        setupListeners();
        setupButtonListeners();

    }

    private void initializeDatabase() {
        this.localdb = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "SMARTFLASHDB.sqlite"
        ).fallbackToDestructiveMigration().build();
    }

    private void setupViews() {
        // Initialize all views
        this.editgetAICategory = findViewById(R.id.editgetAICategory);
        this.spinnerNumCards = findViewById(R.id.spinnerNumCards);

        this.editToLanguage = findViewById(R.id.editToLanguage);
        this.requestCardsButton = findViewById(R.id.requestCardsButton);
        this.fetchDataButton = findViewById(R.id.fetchDataButton);

        // ArrayAdapter setup
        ArrayAdapter arrayAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.card_numbers,
                android.R.layout.simple_spinner_item
        );
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinnerNumCards.setAdapter(arrayAdapter);
        this.spinnerNumCards.setSelection(0, false); // Setting default without invoking listener

        // Setting background for editToLanguage
        if (this.editToLanguage != null) {
            this.editToLanguage.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_green));
        } else {
            Log.e("Error", "editToLanguage is null");
        }

    }
    private void setupButtonListeners() {
        informationButton.setOnClickListener(view -> {
            textOption = "Information";
            toggleButtons(informationButton, languageButton);
        });

        languageButton.setOnClickListener(view -> {
            textOption = "Languages";
            toggleButtons(languageButton, informationButton);
        });
    }
    private void toggleButtons(Button active, Button inactive) {
        active.setBackgroundColor(getResources().getColor(R.color.dark_green)); // replace R.color.green with your green color resource id
        inactive.setBackgroundColor(getResources().getColor(R.color.dark_red)); // replace R.color.red with your red color resource id
        active.setTag("selected");
        inactive.setTag("");
    }
    private void setupListeners() {
        this.editgetAICategory.setOnClickListener(view -> {
            // Replace with actual functionality
        });
        this.spinnerNumCards.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    ((TextView) parent.getChildAt(0)).setTextColor(Color.GRAY);
                } else {
                    // Your item selection logic here...
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        informationButton = findViewById(R.id.informationButton);
        languageButton = findViewById(R.id.languageButton);
        this.requestCardsButton.setOnClickListener(view -> requestCards());
        this.fetchDataButton.setOnClickListener(view -> fetchData());
        findViewById(R.id.buttongetAIHome).setOnClickListener(view -> finish());
    }
    private void configureButton(Button primaryButton, Button secondaryButton,
                                 int primaryColorResId, int secondaryColorResId,
                                 boolean setClickListener, String primaryText, String secondaryText) {
        if (primaryButton != null) {
            primaryButton.setBackgroundColor(ContextCompat.getColor(this, primaryColorResId));
            primaryButton.setText(primaryText);
            if (setClickListener) {
                primaryButton.setOnClickListener(view -> {
                    // Actual functionality
                });
            }
        }

        if (secondaryButton != null) {
            secondaryButton.setBackgroundColor(ContextCompat.getColor(this, secondaryColorResId));
            secondaryButton.setText(secondaryText);
            if (setClickListener) {
                secondaryButton.setOnClickListener(view -> {
                    // Actual functionality
                });
            }
        }
    }
    // Your methods like `configureButton`, `requestCards` and `fetchData` would go below
    private String taskId;
    private FirebaseUser user;
    String username = "";
    private final List<String> waitingMessages = Arrays.asList(
            "Cards are being prepared by Open AI",
            "This might take a couple of minutes"
            //... other messages
    );
    private void updateProgressDialogMessage() {
        if (this.progressDialog != null && this.progressDialog.isShowing()) {
            TextView messageTextView = this.progressDialog.findViewById(com.google.android.material.R.id.snackbar_text);
            if (messageTextView != null) {
                messageTextView.setText(waitingMessages.get(messageIndex));
                messageIndex = (messageIndex + 1) % waitingMessages.size();
            }
        }
    }
    private void showProgressDialog() {
        if (this.progressDialog == null) {
            this.progressDialog = createProgressDialog(waitingMessages.get(messageIndex));
            this.progressDialog.show();
            startPolling();
        } else {
            updateProgressDialogMessage();
        }
    }

    private AlertDialog createProgressDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.dialogue_progress, null);
        TextView messageTextView = view.findViewById(R.id.tv_progress_message);
        messageTextView.setText(message);

        builder.setView(view);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setCancelable(true);
        return alertDialog;
    }

    private void startPolling() {
        this.statusChecker.run();
    }

    private void stopPolling() {
        this.messageHandler.removeCallbacks(this.statusChecker);
    }

    private void fetchData() {
        if (this.db == null) this.db = FirebaseFirestore.getInstance();
        if (this.user == null) this.user = FirebaseAuth.getInstance().getCurrentUser();

        // Use AppDatabase instead of AIDatabase
        if (this.appDb == null) this.appDb = AppDatabase.getInstance(getApplicationContext());

        if (this.user != null) {
            Query query = this.db.collection("cards").whereEqualTo("Username", user.getDisplayName()).limit(100L);

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
            Toast.makeText(this, "User not found. Please log in again!", Toast.LENGTH_SHORT).show();
        }
    }

    // ... other methods ...

    private String generateJson(String username, String category, String subcategory, int numCards, String cardType) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("category", category);
        data.put("subcategory", subcategory);
        data.put("num_cards", numCards);
        data.put("card_type", cardType);

        Log.d("FLAG", "request json textOption: " + cardType);
        return new Gson().toJson(data);
    }

    // ... other methods ...

    private void handleFailure(Exception exception) {
        stopPolling();

        if (this.progressDialog != null && this.progressDialog.isShowing()) {
            this.progressDialog.dismiss();
            this.messageHandler.removeCallbacksAndMessages(null);
        }
        Toast.makeText(this, "Failed to send request. Please try again!", Toast.LENGTH_SHORT).show();
        Log.e("DEBUG", "Request failure", exception);
    }


    // ... other methods ...

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

    private void prepareAndSendRequest(String username, String category, String subCategory, String textOption) {
        String numCardsSelected = this.spinnerNumCards.getSelectedItem().toString();
        Log.d("FLAG", "prepareAndSendRequest: " + username + " " + category + " " + textOption);
        if (!isValidNumber(numCardsSelected)) {
            showSnackbar("Please select a valid number of cards.");
            return;
        }
        if (isNullOrBlank(username) || isNullOrBlank(category) || isNullOrBlank(subCategory) || isNullOrBlank(textOption)) {
            showSnackbar("All fields are mandatory.");
            return;
        }

        String jsonString = generateJson(username, category, subCategory, Integer.parseInt(numCardsSelected), textOption);
        Log.d("FLAG", "json string: " + jsonString);
        sendServerRequest(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonString));
    }

    private void requestCards() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("FLAG", "Requesting Cards - user: " + firebaseUser);
        if (firebaseUser == null) return;

        String displayName = firebaseUser.getDisplayName();
        Log.d("FLAG", "Requesting Cards - user: " + displayName);
        String category = getTextFromEditText(R.id.editgetAICategory);
        String subCategory = getTextFromEditText(R.id.editgetAISubcat);


        if ("selected".equals(this.informationButton.getTag())) {
            textOption = "Information";
        } else if ("selected".equals(this.languageButton.getTag())) {
            textOption = "Languages";
            category = "From " + category + " to " + getTextFromEditText(R.id.editToLanguage);
            if (isNullOrBlank(category) || isNullOrBlank(subCategory)) {
                showSnackbar("Please fill in both language fields.");
                Log.d("FLAG", "Requesting Cards - return 1: " + displayName);
                return;
            }
        } else {
            setButtonColors(2131099734);
            showSnackbar("Invalid text option selected.");
            Log.d("FLAG", "Requesting Cards - return 2: " + displayName);
            return;
        }

        if (!isValidInput(category, "Maximum 5 words allowed for Category") || !isValidInput(subCategory, "Maximum 5 words allowed for Subcategory"))

            return;

        prepareAndSendRequest(displayName, category, subCategory, textOption);
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

    // Your existing sendServerRequest method with modifications
    private void sendServerRequest(RequestBody requestBody) {
        logRequestBody(requestBody);
        Log.d("FLAG", "sendServerRequest: " + requestBody);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .readTimeout(3L, TimeUnit.MINUTES)
                .connectTimeout(60L, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("http://34.125.121.101:5000/run-script")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(this.requestCallback);
    }

    private void logRequestBody(RequestBody requestBody) {
        try {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            Log.d("FLAG", "request body: " + buffer.readUtf8());
        } catch (IOException e) {
            Log.d("FLAG", "Failed to read request body");
            e.printStackTrace();
        }
    }

    private void setButtonColors(int colorResourceId) {
        int color = ContextCompat.getColor(this, colorResourceId);
        this.requestCardsButton.setBackgroundColor(color);
        this.fetchDataButton.setBackgroundColor(color);
    }

    private void setSelectedButtonColor(Button button, int colorResourceId) {
        button.setBackgroundColor(ContextCompat.getColor(this, colorResourceId));
    }

    //private void showCategorySubcategoryDialog() {
    //    new Thread(new GetAICardsActivity$$ExternalSyntheticLambda2(this, this.localdb.wordDao())).start();
    //}
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

    private static final int GREEN_COLOR = -16711936;
    private static final int RED_COLOR = -65536;
    private static final float SNACKBAR_HEIGHT_PERCENTAGE = 0.3F;
    private static final int SNACKBAR_MARGIN = 100;

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

    private void updateButtonColors(Button button1, Button button2) {
        button1.setBackgroundColor(GREEN_COLOR);
        button2.setBackgroundColor(RED_COLOR);
    }

    private void updateEditTextHints(String hint1, String hint2) {
        //EditText editText1 = findViewById(2131296553);
        //EditText editText2 = findViewById(2131296555);

        //editText1.setHint(hint1);
        //editText2.setHint(hint2);
    }

    private void updateEditToLanguageBackgroundColor() {
        int color;
        if (!this.editToLanguage.isEnabled()) {
            color = ContextCompat.getColor(this, R.color.white);
        } else {
            color = ContextCompat.getColor(this, R.color.gray);
        }
        this.editToLanguage.setBackgroundColor(color);
    }

    private void triggerProgressDialogMessageUpdate() {
        this.messageHandler.postDelayed(() -> updateProgressDialogMessage(), MESSAGE_DELAY_MILLIS);
    }

    private void displayCategorySubcategoryDialog(List<CategorySubcategoryPair> pairs) {
        if (pairs != null && !pairs.isEmpty()) {
            List<String> pairStrings = pairs.stream().map(CategorySubcategoryPair::toString).collect(Collectors.toList());

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Category - Subcategory");

            builder.setItems(pairStrings.toArray(new CharSequence[0]), (dialog, which) -> {
                CategorySubcategoryPair selectedPair = pairs.get(which);
                handlePairSelection(selectedPair);
            });

            builder.show();
        } else {
            Toast.makeText(this, "No categories found", Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePairSelection(CategorySubcategoryPair selectedPair) {
        // ... (the rest of the logic for handling the selection)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutdownExecutorService();
    }
    // Assumed based on provided code, handles the selection of an item from the dialog
    private void handlePairSelection(String pair) {
        // ... (the rest of the logic for handling the selection)
    }
    private final Callback requestCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            runOnUiThread(() -> {
                // Handle the failure, maybe update the UI to show an error message
                handleFailure(e);
            });
        }
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (response.isSuccessful()) {
                runOnUiThread(() -> {
                    // Handle the successful response, maybe update the UI
                    try {
                        handleSuccess(response.body().string()); // You can convert the response body to a string or use it as needed
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                runOnUiThread(() -> {
                    // Handle the unsuccessful response, maybe update the UI to show an error or a message
                    handleUnsuccessfulResponse(response.code()); // handle based on the response code or message
                });
            }
        }
    };

    private void handleSuccess(String responseBody) {
        // Use the responseBody as required
        // Update the UI or process the data
    }

    private void handleUnsuccessfulResponse(int responseCode) {
        // Handle the response code as required, maybe log it or show a message to the user
        Log.e("SERVER_REQUEST", "Unsuccessful request with code: " + responseCode);
        // Update the UI or show a Snackbar/Toast with an appropriate message
    }

}