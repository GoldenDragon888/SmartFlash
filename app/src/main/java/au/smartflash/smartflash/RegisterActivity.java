package au.smartflash.smartflash;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FacebookAuthProvider;


import java.util.Arrays;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText etEmail, etPassword, etUsername;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);  // Assume 2131492900 refers to R.layout.activity_register

        initViews();
        setupListeners();
        checkIfUserIsAlreadyLoggedIn();
    }

    private void initViews() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        etUsername = findViewById(R.id.et_username);  // Adjusted for clearer ID names
        etPassword = findViewById(R.id.et_password);
        googleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        //.requestIdToken(getString(R.string.default_web_client_id))  // Assume 2131951706 refers to R.string.default_web_client_id
                        .requestEmail()
                        .build());
    }

    private void setupListeners() {
        findViewById(R.id.btn_change_user).setOnClickListener(this::logout);  // Assume 2131296396 refers to R.id.btnLogout
        findViewById(R.id.btn_register).setOnClickListener(this::register);  // Assume 2131296402 refers to R.id.btnRegister
        findViewById(R.id.btn_login).setOnClickListener(this::login);  // Assume 2131296401 refers to R.id.btnLogin
        findViewById(R.id.btn_google_login).setOnClickListener(this::googleSignIn);  // Assume 2131296400 refers to R.id.btnGoogleSignIn
        findViewById(R.id.btn_facebook_login).setOnClickListener(this::facebookSignIn);  // Assume 2131296398 refers to R.id.btnFacebookSignIn
    }

    private void checkIfUserIsAlreadyLoggedIn() {
        if (auth.getCurrentUser() != null) {
            showSnackbar("You are signed in!");
            startActivity(new Intent(this, UserAdminActivity.class));  // Assume you have a UserAdminActivity
            finish();
        } else {
            showSnackbar("You need to sign in first");
        }
    }

    private void logout(View view) {
        auth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void register(View view) {
        String username = etUsername.getText().toString();
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter all the required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Code when user registration is successful
            }
        });
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

    private void googleSignIn(View view) {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void facebookSignIn(View view) {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        CallbackManager callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                // Handle cancel event
            }

            @Override
            public void onError(FacebookException error) {
                // Handle error event
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Code when Google authentication is successful
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Code when Facebook authentication is successful
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                // Handle exception
            }
        }
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.rootView), message, Snackbar.LENGTH_LONG);
        View view = snackbar.getView();

        // Setting up LayoutParams for snackbar view
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.BOTTOM;
        params.setMargins(100, 0, 100, 150);
        view.setLayoutParams(params);

        TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Setting up background color
        view.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));

        snackbar.show();
    }

}
