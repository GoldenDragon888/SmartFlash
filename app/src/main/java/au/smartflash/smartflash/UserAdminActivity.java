package au.smartflash.smartflash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserAdminActivity extends AppCompatActivity {

    private static final int GOOGLE_SIGN_IN_REQUEST_CODE = 9001;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private EditText etUsername, etEmail, etPassword;
    private static final String PREFS_USER_NAME = "UserPrefs";
    private static final String PREF_EMAIL = "Email";
    private static final String PREF_PASSWORD = "Password";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_useradmin); // Assuming the resource ID you provided corresponds to this name

        initializeViews();
        setupAuth();
        handleExistingUser();
        setupButtonClickListeners();
    }
    private void saveCredentials(String email, String password) {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_EMAIL, email);
        editor.putString(PREF_PASSWORD, password);
        editor.apply();
    }
    private void initializeViews() {
        etUsername = findViewById(R.id.et_username); // Changed the resource IDs to more standard naming
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        findViewById(R.id.btn_reregister).setOnClickListener(v -> signInWithEmail());
        Button homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> finish());

    }
    private void signInWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showSnackbar("Email and password must not be empty.");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveCredentials(email, password);
                        showSnackbar("Sign-in successful!");
                        // Navigate to another activity if required or handle the successful sign-in as needed
                    } else {
                        showSnackbar("Authentication failed. Please check your email and password.");
                    }
                });

    }

    private void setupAuth() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Assuming you've named your string this way
                .requestEmail()
                .build());
    }

    private void handleExistingUser() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            showSnackbar("You are signed in!");

            if (getIntent().getBooleanExtra("FROM_STARTUP", false)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                etUsername.setText(currentUser.getDisplayName());
                etEmail.setText(currentUser.getEmail());
                findViewById(R.id.btn_change_user).setVisibility(View.VISIBLE);

            }
        } else {
            showSnackbar("You need to sign in first");
        }
    }

    private void setupButtonClickListeners() {
        // For brevity, I'm only showing one button setup. Follow a similar pattern for other buttons.
        findViewById(R.id.btn_google_login).setOnClickListener(v -> signInWithGoogle());
        //... other button listeners
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        showSnackbar("Google sign-in successful!");
                        // Navigate to another activity if required or handle the successful sign-in as needed
                    } else {
                        // Handle the error
                        showSnackbar("Google sign-in failed.");
                    }
                });
    }


    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        // Handle the error
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                // Handle the error
            }
        }
        //... other request codes
    }

    public void showSnackbar(String message) {
        // Your existing method remains largely unchanged, but maybe define constants for some of the magic numbers.
    }
}
