package com.example.ryan.socialnetwork;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {


    private Button LoginButton;
    private EditText UserEmail, Userpassword;
    private TextView NeedNewAccountLink, ForgetPasswordlink;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private ImageView googleSignInButton;
    private static final int RC_SIGN_IN = 1;
    private GoogleApiClient mGoogleSigninClient;
    private static final String TAG = "LoginActivity";
    private Boolean emailAddressChecker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);
        NeedNewAccountLink = (TextView) findViewById(R.id.register_account_link);
        UserEmail = (EditText) findViewById(R.id.login_email);
        Userpassword = (EditText)findViewById(R.id.login_password);
        LoginButton = (Button) findViewById(R.id.login_button);
        googleSignInButton = (ImageView) findViewById(R.id.google_signin_button);
        ForgetPasswordlink = (TextView) findViewById(R.id.forget_password_link);
        NeedNewAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToRegisterActivity();
            }
        });

        ForgetPasswordlink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this,ResetPasswordActivity.class));

            }
        });


        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AllowingUserToLogin();
            }
        });

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSigninClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(LoginActivity.this, "Connection to Google Sign In failed...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                .build();

       googleSignInButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               signIn();
           }
       });

    }
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleSigninClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            loadingBar.setTitle("Google Sign In");
            loadingBar.setMessage("Please wait, while we are allowing you to login your Google Account...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
         if(result.isSuccess())
         {
             GoogleSignInAccount account = result.getSignInAccount();
             firebaseAuthWithGoogle(account);
             Toast.makeText(this, "Please wait, we are getting your result...", Toast.LENGTH_SHORT).show();
         }
         else
         {
             Toast.makeText(this, "Can't get Auth Result...", Toast.LENGTH_SHORT).show();
             loadingBar.dismiss();
         }


        }
    }
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            SendUserToMainActivity();
                            loadingBar.dismiss();

                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            String message = task.getException().toString();
                            SendUserToLoginActivity();
                            Toast.makeText(LoginActivity.this, "Not Authenticated: "+ message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }

                        // ...
                    }
                });
    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null)
        {
            SendUserToMainActivity();
        }
    }

    private void AllowingUserToLogin() {
        String email = UserEmail.getText().toString();
        String password = Userpassword.getText().toString();

        if(TextUtils.isEmpty(email))
        {
            Toast.makeText(this, "Please write your email...", Toast.LENGTH_SHORT).show();

        }
        else if(TextUtils.isEmpty(password))
        {
            Toast.makeText(this,"Please write your password..", Toast.LENGTH_SHORT).show();

        }
        else
        {
            loadingBar.setTitle("Login");
            loadingBar.setMessage("Please wait, while we are allowing you to login your new Account...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

          mAuth.signInWithEmailAndPassword(email,password)
                  .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                      @Override
                      public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful())
                            {
                              //VerifyEmailAddress();
                                SendUserToMainActivity();
                              loadingBar.dismiss();
                            }
                            else
                            {
                                String message = task.getException().getMessage();
                                Toast.makeText(LoginActivity.this,"error occured: "+ message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                            }
                      }
                  });
        }
    }

    private void VerifyEmailAddress()
    {
        FirebaseUser user = mAuth.getCurrentUser();
        emailAddressChecker = user.isEmailVerified();
        if(emailAddressChecker)
        {
          SendUserToMainActivity();

        }
        else
        {
            Toast.makeText(this,"Please verify your address first...", Toast.LENGTH_SHORT);
            mAuth.signOut();
        }
    }

    private void SendUserToLoginActivity() {

        Intent mainIntent = new Intent(LoginActivity.this, LoginActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void SendUserToMainActivity() {

        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void SendUserToRegisterActivity() {
        Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(registerIntent);
        finish();
    }
}
