package au.smartflash.smartflash;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import au.smartflash.smartflash.db.AppDatabase;
import au.smartflash.smartflash.db.AppDatabase;
import au.smartflash.smartflash.model.AICard;
import au.smartflash.smartflash.model.Word;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
//import au.smartflash.smartflash.model.AICard;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class CardPairListActivity extends AppCompatActivity
        implements CardPairsAdapter.OnCardClickListener, NavigationView.OnNavigationItemSelectedListener {
    private CardPairsAdapter adapter;
    private AppDatabase appDb;
    private CardPairsAdapter.OnCardClickListener cardClickListener;
    private FirebaseFirestore db;
    private AICard importedWord;
    private List<AICard> listOfCardPairs = new ArrayList<AICard>();
    private FirebaseAuth mAuth;
    private List<AICard> processedWords = new ArrayList<AICard>();
    private ProgressDialog progressDialogFetch;
    private RecyclerView rvCardPairs;
    private List<AICard> selectedCards = new ArrayList<AICard>();
    private FirebaseUser user;
    private String userId;
    private Map<String, Integer> yourCardCountMap;
    private RecyclerView recyclerView;
    private List<CategorySubcategoryPair> uniqueCardPairs = new ArrayList<>();
    private Map<CategorySubcategoryPair, Integer> cardCountMap = new HashMap<>();

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_pairs);
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

        recyclerView = findViewById(R.id.rvCardPairs); // Initialize the RecyclerView

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        appDb = AppDatabase.getInstance(getApplicationContext());

        // Initialize the RecyclerView
        rvCardPairs = findViewById(R.id.rvCardPairs);
        rvCardPairs.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with empty lists; these will be updated later
        adapter = new CardPairsAdapter(new ArrayList<>(), createOnCardClickListener(), new HashMap<>());
        rvCardPairs.setAdapter(adapter);

        if (getIntent().hasExtra("userId")) {
            userId = getIntent().getStringExtra("userId");
        }

        Button btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        btnDeleteSelected.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
        btnDeleteSelected.setOnClickListener(v -> {
            deleteSelectedCardPairs();
        });
        Button btnDownloadEdit = findViewById(R.id.btnDownloadEdit);
        btnDownloadEdit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
        btnDownloadEdit.setOnClickListener(v -> {
            downloadandedit();
        });
        Button btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));
        btnRefresh.setOnClickListener(v -> {
            refreshCardPairList();
        });
        // Initialize onClickListeners
        //findViewById(R.id.btnDeleteSelected).setOnClickListener(v -> deleteSelectedCardPairs());
        //findViewById(R.id.btnDownloadEdit).setOnClickListener(v -> downloadandedit());
        //findViewById(R.id.btnRefresh).setOnClickListener(v -> refreshCardPairList());

        Button btnHome = findViewById(R.id.home_button);
        btnHome.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));

        btnHome.setOnClickListener(view -> {
            Intent intent = new Intent(CardPairListActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // This flag ensures that if MainActivity is already open, it will be brought to the front
            startActivity(intent);
            finish(); // Optionally, if you want to remove the current activity from the back stack
        });


        // Fetch data and populate the adapter
        fetchData();
    }

    private void checkIfSavedLocally(List<AICard> importedWordsFromFirestore, final EditDBActivity.Callback callback) {
        new AsyncTask<List<AICard>, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(List<AICard>... lists) {
                List<AICard> words = lists[0];
                AppDatabase localDb = AppDatabase.getInstance(CardPairListActivity.this);
                for (AICard importedWord : words) {
                    au.smartflash.smartflash.model.Word localWord = localDb.wordDao().getWordByItem(importedWord.getItemAi());
                    if (localWord == null) {
                        return false;
                    } else {
                        File internalStorageDir = getFilesDir();
                        String directoryPath = new File(internalStorageDir, "Smartflash/Images/" + importedWord.getCategoryAi() + "/" + importedWord.getSubcategoryAi()).getAbsolutePath();
                        String imageFile = new File(directoryPath, importedWord.getItemAi() + ".png").getAbsolutePath();
                        File image = new File(imageFile);
                        if (!image.exists()) {
                            return false;
                        }
                    }
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                callback.onResult(result);
            }
        }
                .execute(importedWordsFromFirestore);
    }

    private void deleteAssociatedImage(AICard card) {
        String str = card.getImageUrlAi();
        if (str != null && !str.trim().isEmpty()) {
            String imageReferencePath = "smartflash_objects/images/" + str.substring(str.lastIndexOf("/") + 1);
            FirebaseStorage.getInstance().getReference(imageReferencePath).delete().addOnSuccessListener(aVoid -> Log.d("FLAG", "Image deleted successfully.")).addOnFailureListener(e -> Log.e("FLAG", "Error deleting image.", e));
        }
    }

    private void deleteFromCloud() {
        CollectionReference collectionReference = FirebaseFirestore.getInstance().collection("cards");
        View view = findViewById(android.R.id.content);
        for (AICard importedWord : this.selectedCards) {
            String str;
            Log.d("FLAG", "CardPairList delete pairs SelectedCards" + importedWord.toString());
            if (importedWord.getId() != null) {  // Corrected to use object
                str = importedWord.getId().toString();
            } else {
                str = "DefaultValue";
            }
            collectionReference.document(str).delete()
                    .addOnSuccessListener(aVoid -> {
                        // Handle successful delete operation.
                        // For instance, using 'importedWord' for some logic or UI update.
                        Log.d("Firestore", "Document successfully deleted!");
                    })
                    .addOnFailureListener(e -> {
                        // Handle the error.
                        Log.w("Firestore", "Error deleting document", e);
                    });
        }
        Snackbar.make(view, "Selected cards and images deleted!", -1).show();
    }

    private void dismissProgressDialog() {
        ProgressDialog progressDialog = this.progressDialogFetch;
        if (progressDialog != null && progressDialog.isShowing())
            this.progressDialogFetch.dismiss();
    }

    private void fetchData() {
        showProgressDialog("Fetching Data...");
        cardCountMap.clear();
        uniqueCardPairs.clear();
        this.listOfCardPairs.clear();
        FirebaseUser firebaseUser = this.user;
        if (firebaseUser != null) {
            String email = firebaseUser.getEmail(); // Get user email
            if (email != null && !email.isEmpty()) {
                Query query = this.db.collection("cards").whereEqualTo("Email", email).limit(100L); // Update query to use email
                Log.d("FLAG", "CardPair Fetchdata: " + email + " : ");
                query.get().addOnCompleteListener(this::handleFetchDataCompletion);
            } else {
                Toast.makeText((Context) this, "Email address is not available", Toast.LENGTH_LONG).show();
                dismissProgressDialog();
            }
        } else {
            Toast.makeText((Context) this, "User not authenticated", Toast.LENGTH_LONG).show();
            dismissProgressDialog();
        }
    }

    private void handleFetchDataCompletion(@NonNull Task<QuerySnapshot> task) {
        dismissProgressDialog();
        if (task.isSuccessful() && task.getResult() != null) {
            Log.d("FLAG", "handleFetchDataCompletion: ");
            processQuerySnapshot(task.getResult());
        } else {
            logFetchError(task.getException());
        }
    }

    private void processQuerySnapshot(QuerySnapshot querySnapshot) {
        // Clear existing data
        cardCountMap.clear();
        uniqueCardPairs.clear();

        Log.d("FLAG", "processQuerySnapshot: ");

        for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
            processDocumentSnapshot(documentSnapshot, cardCountMap, uniqueCardPairs);
        }

        logUniqueCardPairs(cardCountMap, uniqueCardPairs);
        updateAdapterWithData(); // Update the adapter with the fetched data
    }
    private void updateAdapterWithPairsAndCounts(List<CategorySubcategoryPair> pairs, Map<CategorySubcategoryPair, Integer> pairCountMap) {
        // Assuming your adapter can accept and process the pair count map
        runOnUiThread(() -> {
            CardPairsAdapter adapter = (CardPairsAdapter) rvCardPairs.getAdapter();
            if (adapter != null) {
                adapter.updateList(pairs, pairCountMap);
            }
        });
    }
    private void updateAdapterWithData() {
        if (adapter != null) {
            // Ensure that uniqueCardPairs and cardCountMap are updated correctly
            // before calling this method.
            adapter.updateList(new ArrayList<>(uniqueCardPairs), cardCountMap);
        }
    }
    private void processDocumentSnapshot(DocumentSnapshot documentSnapshot,
                                         Map<CategorySubcategoryPair, Integer> cardCountMap,
                                         List<CategorySubcategoryPair> uniqueCardPairs) {

        Log.d("FLAG", "DocumentSnapshot Data: " + documentSnapshot.getData());

        // Create a new AICard instance
        AICard aiCard = new AICard();

        // Manually set each field from the DocumentSnapshot
        aiCard.setCategoryAi(documentSnapshot.getString("Category"));
        aiCard.setSubcategoryAi(documentSnapshot.getString("Subcategory"));
        // Set other fields similarly...

        Log.d("FLAG", "processDocumentSnapshot: " + aiCard);

        if (aiCard != null) {
            Log.d("FLAG", "AICard Category: " + aiCard.getCategoryAi() + ", Subcategory: " + aiCard.getSubcategoryAi());

            // Handle null values for category and subcategory
            String category = aiCard.getCategoryAi() != null ? aiCard.getCategoryAi() : "";
            String subcategory = aiCard.getSubcategoryAi() != null ? aiCard.getSubcategoryAi() : "";
            CategorySubcategoryPair pair = new CategorySubcategoryPair(category, subcategory);
            updateCardCountsAndPairs(cardCountMap, uniqueCardPairs, pair);
        } else {
            Log.d("FLAG", "Received null AICard from documentSnapshot: " + documentSnapshot.getId());
        }
    }

    private void updateCardCountsAndPairs(Map<CategorySubcategoryPair, Integer> cardCountMap,
                                          List<CategorySubcategoryPair> uniqueCardPairs,
                                          CategorySubcategoryPair pair) {
        if (pair.getCategory() != null && pair.getSubcategory() != null) {
            cardCountMap.put(pair, cardCountMap.getOrDefault(pair, 0) + 1);
            if (!uniqueCardPairs.contains(pair)) {
                uniqueCardPairs.add(pair);
            }
        } else {
            Log.d("FLAG", "Invalid pair with null values: " + pair);
        }
    }

    private void logUniqueCardPairs(Map<CategorySubcategoryPair, Integer> cardCountMap,
                                    List<CategorySubcategoryPair> uniqueCardPairs) {
        for (CategorySubcategoryPair pair : uniqueCardPairs) {
            int count = cardCountMap.getOrDefault(pair, 0);
            Log.d("FLAG", "Pair: " + pair + ", Count: " + count);
        }
    }


    private void logFetchError(Exception exception) {
        Log.d("FLAG", "Firestore - Error fetching documents.", exception);
        Toast.makeText(this, "Error fetching data.", Toast.LENGTH_LONG).show();
    }

    private void updateRecyclerView() {
        if (adapter != null) {
            adapter.updateList(uniqueCardPairs, cardCountMap);
        } else {
            adapter = new CardPairsAdapter(uniqueCardPairs, createOnCardClickListener(), cardCountMap);
            rvCardPairs.setAdapter(adapter);
        }
    }

    private CardPairsAdapter.OnCardClickListener createOnCardClickListener() {
        return new CardPairsAdapter.OnCardClickListener() {
            @Override
            public void onCardClick(int position) {
                // Toggle selection in the adapter
                CardPairsAdapter adapter = (CardPairsAdapter) rvCardPairs.getAdapter();
                if (adapter != null) {
                    adapter.toggleSelection(position);
                }
            }
        };
    }


    private void showProgressDialog(String paramString) {
        if (this.progressDialogFetch == null) {
            ProgressDialog progressDialog = new ProgressDialog((Context) this);
            this.progressDialogFetch = progressDialog;
            progressDialog.setCancelable(false);
        }
        this.progressDialogFetch.setMessage(paramString);
        this.progressDialogFetch.show();
    }

    public void onCardClick(int paramInt) {
        AICard importedWord = this.listOfCardPairs.get(paramInt);
        importedWord.setSelected(importedWord.isSelected() ^ true);
        if (importedWord.isSelected()) {
            this.selectedCards.add(importedWord);
        } else {
            this.selectedCards.remove(importedWord);
        }
        this.adapter.notifyItemChanged(paramInt);
    }

    public void showDeleteAIConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Confirmation")
                .setMessage("Do you really want to delete all cards and associated images from the cloud? (They will be saved locally if imported previously).")
                .setPositiveButton("Yes, Delete", (dialog, id) -> handlePositiveDelete())
                .setNegativeButton("Cancel", (dialog, id) -> {
                })
                .create().show();
    }

    // Assuming your onClick methods for the buttons look something like this:
    private void yourMethodFor2131296606() {
        // Your code
    }
    public void downloadandedit() {
        // Show a warning dialog before starting the download process
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("WARNING - any changes made in the Cloud using the Web interface will overwrite the local card. Do you wish to proceed?")
                .setPositiveButton("Yes", (dialog, which) -> startDownloadAndEditProcess())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startDownloadAndEditProcess() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Downloading data...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        ExecutorService executor = Executors.newCachedThreadPool();
        CardPairsAdapter adapter = (CardPairsAdapter) rvCardPairs.getAdapter();
        if (adapter != null) {
            List<CategorySubcategoryPair> selectedPairs = adapter.getSelectedPairs();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            AppDatabase localDb = AppDatabase.getInstance(getApplicationContext());
            CountDownLatch latch = new CountDownLatch(selectedPairs.size());

            Log.d("FLAG", "Starting download and edit process for " + selectedPairs.size() + " pairs.");

            for (CategorySubcategoryPair pair : selectedPairs) {
                if (pair.getCategory() == null || pair.getSubcategory() == null) {
                    Log.w("FLAG", "Category or Subcategory is null, skipping Firestore query for this pair.");
                    latch.countDown(); // Ensure the latch is counted down for skipped pairs
                    continue;
                }

                Log.d("FLAG", "Querying Firestore for: Category=" + pair.getCategory() + ", Subcategory=" + pair.getSubcategory());
                db.collection("cards")
                        .whereEqualTo("Category", pair.getCategory())
                        .whereEqualTo("Subcategory", pair.getSubcategory())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            executor.execute(() -> {
                                Log.d("FLAG", "Processing query snapshot for Category=" + pair.getCategory() + ", Subcategory=" + pair.getSubcategory());
                                processQuerySnapshot(querySnapshot, localDb, latch);
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FLAG", "Error querying Firestore: " + e.getMessage());
                            latch.countDown();
                        });
            }

            new Thread(() -> {
                try {
                    Log.d("FLAG", "Awaiting latch...");
                    latch.await();
                    Log.d("FLAG", "Latch released, continuing execution.");
                } catch (InterruptedException e) {
                    Log.e("FLAG", "Thread interrupted while waiting on latch: " + e.getMessage());
                    Thread.currentThread().interrupt();
                } finally {
                    // Ensure dialog is dismissed even if an exception occurs
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                            Log.d("FLAG", "ProgressDialog dismissed.");
                        }
                    });
                }

                runOnUiThread(() -> {
                    Log.d("FLAG", "Starting EditDBActivity");
                    Intent intent = new Intent(CardPairListActivity.this, EditDBActivity.class);
                    startActivity(intent);
                });
            }).start();
        }
    }

    private void processQuerySnapshot(QuerySnapshot querySnapshot, AppDatabase localDb, CountDownLatch latch) {
        Log.d("FLAG", "In processQuerySnapshot with " + querySnapshot.size() + " documents.");

        if (querySnapshot.isEmpty()) {
            Log.d("FLAG", "QuerySnapshot is empty, counting down latch and returning.");
            latch.countDown();
            return;
        }

        for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
            Word newWord = convertDocumentToWord(documentSnapshot);
            if (newWord != null) {
                localDb.wordDao().insert(newWord); // Insert the word without checking for duplicates
                Log.d("FLAG", "Word inserted: " + newWord.getItem());
            }
        }

        latch.countDown();
        Log.d("FLAG", "Exiting processQuerySnapshot");
    }


    private Word convertDocumentToWord(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) {
            Log.e("FLAG", "Invalid DocumentSnapshot");
            return null;
        }

        Word word = new Word();
        // Set fields from the document snapshot
        word.setCategory(documentSnapshot.getString("Category"));
        word.setSubcategory(documentSnapshot.getString("Subcategory"));
        word.setItem(documentSnapshot.getString("Item"));
        word.setDescription(documentSnapshot.getString("Description"));
        word.setDetails(documentSnapshot.getString("Details"));
        word.setDifficulty(documentSnapshot.getString("Difficulty"));

        String imageUrl = documentSnapshot.getString("Image");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            word.setImage(downloadAndSaveImage(imageUrl, word));
        }

        return word;
    }

    private byte[] downloadAndSaveImage(String imageUrl, Word word) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            InputStream input = connection.getInputStream();
            File destinationFile = generateImagePath(word);

            if (!destinationFile.getParentFile().exists()) {
                destinationFile.getParentFile().mkdirs();
            }

            OutputStream output = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            output.close();
            input.close();

            return convertFileToByteArray(destinationFile);
        } catch (IOException e) {
            Log.e("FLAG", "Error downloading image: " + e.getMessage(), e);
            return null;
        }
    }

    private byte[] convertFileToByteArray(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            Log.e("FLAG", "Error converting file to byte array", e);
            return null;
        }
    }

    private boolean hasDuplicate(AppDatabase localDb, Word newWord) {
        int count = localDb.wordDao().countWordsByCategorySubcategoryItemetc(
                newWord.getCategory(),
                newWord.getSubcategory(),
                newWord.getItem(),
                newWord.getDescription(),
                newWord.getDetails()
        );
        return count > 0;
    }
    private File generateImagePath(Word word) {
        String category = word.getCategory() != null ? word.getCategory() : "unknown";
        String subcategory = word.getSubcategory() != null ? word.getSubcategory() : "unknown";
        return new File(getExternalFilesDir(null), "Images/" + category + "/" + subcategory + "/" + word.getItem() + ".png");
    }

    private void checkForExportAndCountDown(AICard aiCard, int documentIndex, int totalDocuments, CountDownLatch latch) {
        if (documentIndex == totalDocuments - 1) {
            exportDataToCSV(aiCard.getCategoryAi(), aiCard.getSubcategoryAi());
        }
        latch.countDown();
    }

    private void exportDataToCSV(String category, String subcategory) {
        File folder = new File(getExternalFilesDir(null), "CSV_DB_Backup");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        AppDatabase localDb = AppDatabase.getInstance(getApplicationContext());
        String fileName = category + "_" + subcategory + ".csv";
        File csvFile = new File(folder, fileName);
        Log.d("FLAG", "CSV Export - Before File written to " + csvFile.getAbsolutePath());

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.append("\"Category\",\"Subcategory\",\"Item\",\"Description\",\"Details\",\"Difficulty\"\n");

            List<Word> words = localDb.wordDao().getWordsByCategoryAndSubcategory(category, subcategory);
            for (Word word : words) {
                writer.append(quoted(word.getCategory())).append(",");
                writer.append(quoted(word.getSubcategory())).append(",");
                writer.append(quoted(word.getItem())).append(",");
                writer.append(quoted(word.getDescription())).append(",");
                writer.append(quoted(word.getDetails())).append(",");
                writer.append(quoted(word.getDifficulty())).append("\n");
            }

            writer.flush();
            String successMessage = "Selected pairs successfully backed up to " + csvFile.getAbsolutePath();
            runOnUiThread(() -> Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show());
        } catch (IOException e) {
            Log.e("FLAG", "CSV Export - Error", e);
            String errorMessage = "Error exporting CSV: " + e.getMessage();
            runOnUiThread(() -> Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show());
        }
    }
    private String quoted(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    public void deleteSelectedCardPairs() {
        CardPairsAdapter adapter = (CardPairsAdapter) rvCardPairs.getAdapter();
        if (adapter != null) {
            List<CategorySubcategoryPair> selectedPairs = adapter.getSelectedPairs();
            if (selectedPairs.isEmpty()) {
                Toast.makeText(this, "No pairs selected for deletion.", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete Confirmation")
                    .setMessage("Are you sure you want to delete these card pairs? This action is irreversible.")
                    .setPositiveButton("Delete", (dialog, which) -> executeDeletion(selectedPairs))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            Toast.makeText(this, "Adapter is null, cannot delete.", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeDeletion(List<CategorySubcategoryPair> selectedPairs) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Deleting selected card pairs...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            CountDownLatch latch = new CountDownLatch(selectedPairs.size());

            for (CategorySubcategoryPair pair : selectedPairs) {
                db.collection("cards")
                        .whereEqualTo("Category", pair.getCategory())
                        .whereEqualTo("Subcategory", pair.getSubcategory())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                                db.collection("cards").document(documentSnapshot.getId()).delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("FLAG", "DocumentSnapshot successfully deleted!");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w("FLAG", "Error deleting document", e);
                                        });
                            }
                            latch.countDown();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FLAG", "Error fetching document for deletion", e);
                            latch.countDown();
                        });
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e("FLAG", "Latch awaiting was interrupted", e);
            }

            progressDialog.dismiss();
            runOnUiThread(() -> {
                // Refresh the UI or list to indicate that the items have been deleted
                Toast.makeText(this, "Selected pairs deleted successfully.", Toast.LENGTH_SHORT).show();
                // Update your adapter here if needed
                adapter.removeSelectedPairs();
                //adapter.updatePairs(newCards);
                adapter.notifyDataSetChanged();
            });
        });
    }
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private void downloadImage(String imageUrl, File imageFile, Runnable onComplete) {
        Log.d("FLAG", "Starting image download: " + imageUrl);

        executorService.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + connection.getResponseCode());
                }
                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(imageFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.close();
                input.close();
                Log.d("Image Download", "Image saved to " + imageFile.getPath());
            } catch (IOException e) {
                Log.e("Image Download", "Error while downloading the image: " + imageUrl, e);
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
    //private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private boolean isRefreshing = false;
    private ProgressDialog progressDialog;

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Refreshing data...");
        progressDialog.setCancelable(false);
    }

    private void refreshCardPairList() {
        if (isRefreshing) {
            Log.d("FLAG", "Already refreshing, ignoring this call.");
            return;
        }

        isRefreshing = true;
        if (progressDialog == null) {
            initProgressDialog();
        }
        progressDialog.show();
        // Clear existing data before refreshing
        cardCountMap.clear();
        uniqueCardPairs.clear();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            handleRefreshCompletion("User not authenticated");
            return;
        }

        String userEmail = currentUser.getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CardPairsAdapter adapter = (CardPairsAdapter) rvCardPairs.getAdapter();
        if (adapter == null) {
            handleRefreshCompletion("Adapter not initialized");
            return;
        }

        db.collection("cards")
                .whereEqualTo("Email", userEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    cardCountMap.clear();
                    uniqueCardPairs.clear();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        processDocumentSnapshot(document, cardCountMap, uniqueCardPairs);
                    }
                    logUniqueCardPairs(cardCountMap, uniqueCardPairs);
                    runOnUiThread(() -> adapter.updateList(new ArrayList<>(uniqueCardPairs), cardCountMap));
                    handleRefreshCompletion(null); // No error message
                })
                .addOnFailureListener(e -> {
                    Log.e("FLAG", "Error refreshing data", e);
                    handleRefreshCompletion("Error refreshing data");
                });
    }

    private void handleRefreshCompletion(String errorMessage) {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            isRefreshing = false;
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private List<CategorySubcategoryPair> processQuerySnapshotToPairs(QuerySnapshot querySnapshot) {
        Map<CategorySubcategoryPair, Integer> pairCountMap = new HashMap<>();

        if (querySnapshot != null) {
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                String category = document.getString("Category");
                String subcategory = document.getString("Subcategory");
                if (category != null && subcategory != null) {
                    CategorySubcategoryPair newPair = new CategorySubcategoryPair(category, subcategory);
                    pairCountMap.put(newPair, pairCountMap.getOrDefault(newPair, 0) + 1);
                }
            }
        }

        // Now you have unique pairs with their counts in pairCountMap
        // You need to pass this map to your adapter, not just the list of pairs
        updateAdapterWithPairsAndCounts(new ArrayList<>(pairCountMap.keySet()), pairCountMap);
        return new ArrayList<>(pairCountMap.keySet()); // Convert set back to list
    }



    private int generateUniqueId() {
        // Implement the logic to generate a unique ID.
        // This could be based on the highest existing ID in your database.
        return 0;
    }

    // In your database helper or DAO class
    private boolean hasDuplicate(String category, String subcategory, String item) {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        int count = db.wordDao().countWordsByCategorySubcategoryItem(category, subcategory, item);
        return count > 0;
    }

   private void handlePositiveDelete() {
        // Handle deletion logic here
    }
}
