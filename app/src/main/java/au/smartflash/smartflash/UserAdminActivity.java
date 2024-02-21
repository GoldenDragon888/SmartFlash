package au.smartflash.smartflash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.facebook.AccessToken;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserAdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private static final int GOOGLE_SIGN_IN_REQUEST_CODE = 9001;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private EditText etUsername, etEmail, etPassword;
    private static final String PREFS_USER_NAME = "UserPrefs";
    private static final String PREF_EMAIL = "Email";
    private static final String PREF_PASSWORD = "Password";
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
        setContentView(R.layout.activity_useradmin); // Assuming the resource ID you provided corresponds to this name

        initializeViews();
        setupAuth();
        handleExistingUser();
    }
    private void saveCredentials(String email, String password) {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_EMAIL, email);
        editor.putString(PREF_PASSWORD, password);
        editor.apply();
    }
    private void initializeViews() {
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

        etUsername = findViewById(R.id.et_username); // Changed the resource IDs to more standard naming
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);

        Button btn_login = findViewById(R.id.btn_login);
        btn_login.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
        btn_login.setOnClickListener(v -> {
            registerUser();
        });
        Button btn_register = findViewById(R.id.btn_register);
        btn_register.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
        btn_register.setOnClickListener(v -> {
            registerUser();
        });
        Button btn_change_user = findViewById(R.id.btn_change_user);
        btn_change_user.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));
        btn_change_user.setOnClickListener(v -> {
            signOut();
        });
        Button btn_sign_in = findViewById(R.id.btn_sign_in);
        btn_sign_in.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
        btn_sign_in.setOnClickListener(v -> {
            signInWithEmail();
        });
        Button btn_sign_out = findViewById(R.id.btn_sign_out);
        btn_sign_out.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
        btn_sign_out.setOnClickListener(v -> {
            signOut();
        });
        Button btn_google_login = findViewById(R.id.btn_google_login);
        btn_google_login.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));
        btn_google_login.setOnClickListener(v -> {
            signInWithGoogle();
        });
        Button homeButton = findViewById(R.id.buttongetAIHome);
        if (homeButton != null) {
            homeButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));
            homeButton.setOnClickListener(v -> {
                Intent intent = new Intent(UserAdminActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        } else {
            Log.e("UserAdminActivity", "Button not found in the layout");
        }
    }
    private void login(View view) {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Code when user login is successful
            }
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim(); // Assuming you have a field for username
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username)) {
            showSnackbar("Email, username, and password must not be empty.");
            return;
        }

        // Firebase call to create a new user
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Add a new document with user's UID
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("username", username);
                            userData.put("role", "user");

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("users").document(user.getUid()).set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        showSnackbar("User registration and Firestore record creation successful");
                                    })
                                    .addOnFailureListener(e -> {
                                        showSnackbar("Firestore record creation failed");
                                    });
                        }
                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        showSnackbar("Registration failed: " + task.getException().getMessage());
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // User is signed in
            etEmail.setText(user.getEmail());
            etUsername.setText(user.getDisplayName());
            showSnackbar("Registration success");
            // Navigate to home or main activity, if required
            Intent intent = new Intent(UserAdminActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            // User is signed out
            etEmail.setText("");
            etUsername.setText("");
            etPassword.setText("");
        }
    }
    private void setupAuth() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Assuming you've named your string this way
                .requestEmail()
                .build());
    }
    private void checkCurrentUser() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            etEmail.setText(currentUser.getEmail());
            showSnackbar("You are now signed in.");
        } else {
            showSnackbar("No user signed in.");
        }
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
                 }
        } else {
            showSnackbar("You need to sign in first");
        }
    }
    private void signInWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showSnackbar("Username, Email and password must not be empty.");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        showSnackbar("Sign-in successful!");
                        checkCurrentUser();
                    } else {
                        showSnackbar("Authentication failed. Please check your email and password.");
                    }
                });
    }
    private void signOut() {
        auth.signOut();
        showSnackbar("Signed out successfully");
        checkCurrentUser();
    }
    private void changeUser() {
        // Clear the input fields or add logic to switch user
        etEmail.setText("");
        etUsername.setText("");
        etPassword.setText("");
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
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null && task.getResult().getAdditionalUserInfo().isNewUser()) {
                            updateUserRole(user);
                        }
                        showSnackbar("Google sign-in successful!");
                        // Navigate to another activity if required
                        navigateToMainActivity();
                    } else {
                        showSnackbar("Google sign-in failed.");
                    }
                });
    }
    private void navigateToMainActivity() {
        Intent intent = new Intent(UserAdminActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: If you want to remove this activity from the back stack
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
    private void updateUserRole(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("role", "user");

        db.collection("users").document(user.getUid()).set(userData)
                .addOnSuccessListener(aVoid -> Log.d("UserRole", "User role set to 'user'"))
                .addOnFailureListener(e -> Log.w("UserRole", "Error setting user role", e));
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

        snackbarText.setTextColor(Color.BLACK);

        int color = ContextCompat.getColor(getApplicationContext(), R.color.light_blue);
        view.setBackgroundTintList(ColorStateList.valueOf(color));
    }

}
