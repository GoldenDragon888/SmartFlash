package au.smartflash.smartflash;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


public class CardPairListActivity extends AppCompatActivity implements CardPairsAdapter.OnCardClickListener {
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


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_pairs);

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

        // Initialize onClickListeners
        findViewById(R.id.btnDeleteSelected).setOnClickListener(v -> deleteSelectedCardPairs());
        findViewById(R.id.btnDownloadEdit).setOnClickListener(v -> downloadandedit());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> refreshCardPairList());

        Button btnHome = findViewById(R.id.home_button);

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
        this.listOfCardPairs.clear();
        FirebaseUser firebaseUser = this.user;
        if (firebaseUser != null) {
            String username = firebaseUser.getDisplayName();
            Query query = this.db.collection("cards").whereEqualTo("Username", username).limit(100L);
            Log.d("FLAG", "CardPair Fetchdata: " + username + " : ");
            query.get().addOnCompleteListener(this::handleFetchDataCompletion);
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

    private void updateAdapterWithData() {
        if (adapter != null) {
            adapter.updateList(uniqueCardPairs, cardCountMap);
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
                                processQuerySnapshot(querySnapshot, localDb, latch); // Updated method call
                            });
                        })
                        .addOnFailureListener(e -> {
                            latch.countDown();
                        });
                }

            // Run waiting for latch in a separate thread to avoid blocking the executor
            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                progressDialog.dismiss();
                runOnUiThread(() -> {
                    Intent intent = new Intent(CardPairListActivity.this, EditDBActivity.class);
                    startActivity(intent);
                });
            }).start();
        } else {
            progressDialog.dismiss();
        }
    }
    private void processQuerySnapshot(QuerySnapshot querySnapshot, AppDatabase localDb, CountDownLatch latch) {
        Log.d("FLAG", "In processQuerySnapshot");

        if (querySnapshot.isEmpty()) {
            latch.countDown(); // Immediately count down if there are no documents
            return;
        }

        for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
            AICard aiCard = new AICard();
            //AICard aiCard = documentSnapshot.toObject(AICard.class);
            aiCard.setCategoryAi(documentSnapshot.getString("Category"));
            aiCard.setSubcategoryAi(documentSnapshot.getString("Subcategory"));
            aiCard.setItemAi(documentSnapshot.getString("Item"));
            aiCard.setDescriptionAi(documentSnapshot.getString("Description"));
            aiCard.setDetailsAi(documentSnapshot.getString("Details"));
            aiCard.setImageUrlAi(documentSnapshot.getString("Image"));
            //aiCard.setItemAi(documentSnapshot.getString("Item"));

            Log.d("FLAG", "AICard Data: " + aiCard.toString());

            if (aiCard != null) {
                boolean isDuplicate = localDb.wordDao().wordExists(aiCard.getItemAi(), aiCard.getDescriptionAi()) > 0;
                if (!isDuplicate) {
                    Log.d("FLAG", "AICard Data Item: " + aiCard.getItemAi());
                    Word wordEntry = new Word();
                    wordEntry.setCategory(aiCard.getCategoryAi());
                    wordEntry.setSubcategory(aiCard.getSubcategoryAi());
                    wordEntry.setItem(aiCard.getItemAi());
                    wordEntry.setDescription(aiCard.getDescriptionAi());
                    wordEntry.setDetails(aiCard.getDetailsAi());
                    wordEntry.setDifficulty("Easy");
                    // Set other fields from aiCard...

                    File imageFile = new File(getExternalFilesDir(null), "Images/" + aiCard.getCategoryAi() + "/" + aiCard.getSubcategoryAi() + "/" + aiCard.getItemAi() + ".png");
                    if (!imageFile.getParentFile().exists()) {
                        imageFile.getParentFile().mkdirs(); // Create directories if they do not exist
                    }

                    // Download the image in the background and then insert the word into the database
                    downloadImage(aiCard.getImageUrlAi(), imageFile, () -> {
                        localDb.wordDao().insertWord(wordEntry);
                        Log.d("FLAG", "processQuerySnapshot Inserted: " + wordEntry.getItem());
                        if (querySnapshot.getDocuments().indexOf(documentSnapshot) == querySnapshot.size() - 1) {
                            exportDataToCSV(aiCard.getCategoryAi(), aiCard.getSubcategoryAi());
                        }
                        latch.countDown(); // Count down after processing each document
                    });
                } else {
                    if (querySnapshot.getDocuments().indexOf(documentSnapshot) == querySnapshot.size() - 1) {
                        exportDataToCSV(aiCard.getCategoryAi(), aiCard.getSubcategoryAi());
                    }
                    latch.countDown(); // Count down if it's a duplicate
                }
            }
        }
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
    private void downloadImage(String imageUrl, File imageFile, Runnable callback) {
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
                if (callback != null) {
                    callback.run();
                }
            } catch (IOException e) {
                Log.e("Image Download", "Error while downloading the image.", e);
            }
        });
    }
    private void refreshCardPairList() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Refreshing data...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        Log.d("FLAG", "Refreshing CardPairList");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CardPairsAdapter adapter = (CardPairsAdapter) rvCardPairs.getAdapter();
        if (adapter != null) {
            List<CategorySubcategoryPair> currentPairs = adapter.getPairs();

            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                CountDownLatch latch = new CountDownLatch(currentPairs.size());

                for (CategorySubcategoryPair pair : currentPairs) {
                    // Logic to fetch new data for each pair...
                    db.collection("cards")
                            .whereEqualTo("Category", pair.getCategory())
                            .whereEqualTo("Subcategory", pair.getSubcategory())
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<CategorySubcategoryPair> newPairs = processQuerySnapshotToPairs(querySnapshot); // You need to implement this
                                runOnUiThread(() -> adapter.updatePairs(newPairs));
                                latch.countDown();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FLAG", "Error refreshing data", e);
                                latch.countDown();
                            });
                }

                try {
                    latch.await();
                    // Final UI changes should be run on the main thread
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        // You do not need to call notifyDataSetChanged here since it's called inside updatePairs method.
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    runOnUiThread(progressDialog::dismiss);
                }
            });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "Adapter not initialized", Toast.LENGTH_SHORT).show();
        }
    }
    private List<CategorySubcategoryPair> processQuerySnapshotToPairs(QuerySnapshot querySnapshot) {
        List<CategorySubcategoryPair> pairs = new ArrayList<>();

        if (querySnapshot != null) {
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                // Assuming the document contains fields 'category' and 'subcategory'
                String category = document.getString("Category");
                String subcategory = document.getString("Subcategory");
                if (category != null && subcategory != null) {
                    pairs.add(new CategorySubcategoryPair(category, subcategory));
                    // If you have a count or other fields, add them here.
                }
            }
        }
        return pairs;
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
