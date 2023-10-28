package au.smartflash.smartflash;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import au.smartflash.smartflash.db.AIDatabase;
import au.smartflash.smartflash.db.AppDatabase;
import au.smartflash.smartflash.model.ImportedWord;

public class CardPairListActivity extends AppCompatActivity implements CardPairsAdapter.OnCardClickListener {
    private CardPairsAdapter adapter;

    private AIDatabase appDb;

    private CardPairsAdapter.OnCardClickListener cardClickListener;

    private FirebaseFirestore db;

    private ImportedWord importedWord;

    private List<ImportedWord> listOfCardPairs = new ArrayList<ImportedWord>();

    private FirebaseAuth mAuth;

    private List<ImportedWord> processedWords = new ArrayList<ImportedWord>();

    private ProgressDialog progressDialogFetch;

    private RecyclerView rvCardPairs;

    private List<ImportedWord> selectedCards = new ArrayList<ImportedWord>();

    private FirebaseUser user;

    private String userId;

    private Map<String, Integer> yourCardCountMap;

    private void checkIfSavedLocally(List<ImportedWord> importedWordsFromFirestore, final EditAIDBActivity.Callback callback) {
        new AsyncTask<List<ImportedWord>, Void, Boolean>() {
            @Override protected Boolean doInBackground(List<ImportedWord>... lists) {
                List<ImportedWord> words = lists[0]; AppDatabase localDb = AppDatabase.getInstance(CardPairListActivity.this); for (ImportedWord importedWord : words) {
                    au.smartflash.smartflash.model.Word localWord = localDb.wordDao().getWordByItem(importedWord.getItem());
                    if (localWord == null) {
                        return false; }
                    else { File internalStorageDir = getFilesDir(); String directoryPath = new File(internalStorageDir, "Smartflash/Images/" + importedWord.getCategory() + "/" + importedWord.getSubcategory()).getAbsolutePath(); String imageFile = new File(directoryPath, importedWord.getItem() + ".png").getAbsolutePath();
                        File image = new File(imageFile); if (!image.exists()) {
                            return false; } } }
                return true; }
            @Override protected void onPostExecute(Boolean result) { callback.onResult(result); } }
                .execute(importedWordsFromFirestore); }

    private void deleteAssociatedImage(ImportedWord card) {
        String str = card.getImageURL();
        if (str != null && !str.trim().isEmpty()) {
            String imageReferencePath = "smartflash_objects/images/" + str.substring(str.lastIndexOf("/") + 1);
            FirebaseStorage.getInstance().getReference(imageReferencePath).delete() .addOnSuccessListener(aVoid -> Log.d("FLAG", "Image deleted successfully.")) .addOnFailureListener(e -> Log.e("FLAG", "Error deleting image.", e)); } }

    private void deleteFromCloud() {
        CollectionReference collectionReference = FirebaseFirestore.getInstance().collection("cards");
        View view = findViewById(android.R.id.content);
        for (ImportedWord importedWord : this.selectedCards) {
            String str;
            Log.d("FLAG", "CardPairList delete pairs SelectedCards" + importedWord.toString());
            if (importedWord.getId() != null) {
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
                    });        }
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
            Toast.makeText((Context)this, "User not authenticated", 0).show();
            dismissProgressDialog();
        }
    }
    private void handleFetchDataCompletion(@NonNull Task<QuerySnapshot> task) {
        if (task.isSuccessful()) {
            // Handle the successful fetching of data. Update your list and UI accordingly.
            QuerySnapshot querySnapshot = task.getResult();
            if (querySnapshot != null) {
                // Convert the fetched documents to 'ImportedWord' or whatever is required
                // Update your listOfCardPairs or whatever list you use
            }
        } else {
            // Handle the error.
            Log.w("Firestore", "Error fetching documents.", task.getException());
        }
        dismissProgressDialog();
    }

    private void showProgressDialog(String paramString) {
        if (this.progressDialogFetch == null) {
            ProgressDialog progressDialog = new ProgressDialog((Context)this);
            this.progressDialogFetch = progressDialog;
            progressDialog.setCancelable(false);
        }
        this.progressDialogFetch.setMessage(paramString);
        this.progressDialogFetch.show();
    }

    public void onCardClick(int paramInt) {
        ImportedWord importedWord = this.listOfCardPairs.get(paramInt);
        importedWord.setSelected(importedWord.isSelected() ^ true);
        if (importedWord.isSelected()) {
            this.selectedCards.add(importedWord);
        } else {
            this.selectedCards.remove(importedWord);
        }
        this.adapter.notifyItemChanged(paramInt);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_pairs);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        appDb = AIDatabase.getInstance(getApplicationContext());

        rvCardPairs = findViewById(R.id.rvCardPairs);
        rvCardPairs.setLayoutManager(new LinearLayoutManager(this));

        if (getIntent().hasExtra("userId")) {
            userId = getIntent().getStringExtra("userId");
        }

        //Need to fix the methods for these. Should be OK.
        findViewById(R.id.btnDeleteSelected).setOnClickListener(v -> yourMethodFor2131296606());
        findViewById(R.id.btnDownloadEdit).setOnClickListener(v -> yourMethodFor2131296392());
        findViewById(R.id.home_button).setOnClickListener(v -> yourMethodFor2131296391());

        fetchData();
    }

    public void showDeleteAIConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Confirmation")
                .setMessage("Do you really want to delete all cards and associated images from the cloud? (They will be saved locally if imported previously).")
                .setPositiveButton("Yes, Delete", (dialog, id) -> handlePositiveDelete())
                .setNegativeButton("Cancel", (dialog, id) -> {})
                .create().show();
    }

    // Assuming your onClick methods for the buttons look something like this:
    private void yourMethodFor2131296606() {
        // Your code
    }

    private void yourMethodFor2131296392() {
        // Your code
    }

    private void yourMethodFor2131296391() {
        // Your code
    }

    private void handlePositiveDelete() {
        // Handle deletion logic here
    }

}
