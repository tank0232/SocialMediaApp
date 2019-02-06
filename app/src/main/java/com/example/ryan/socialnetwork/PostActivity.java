package com.example.ryan.socialnetwork;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.SimpleTimeZone;

public class PostActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ImageButton SelectPostImage;
    private Button UpdatePostButton;
    private EditText PostDescription;
    private static  final  int Gallery_Pick = 1;
    private Uri ImageUri;
    private String Description;
    private StorageReference PostImageReference;
    private DatabaseReference usersRef, PostRef;
    private FirebaseAuth mAuth;
    private String saveCurrentDate, saveCurrentTime, postRandomName;
    private String downloadUrl, current_user_id;
     private ProgressDialog loadingBar;

     private long countPosts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        SelectPostImage = (ImageButton) findViewById(R.id.select_post_image);
        UpdatePostButton = (Button) findViewById(R.id.update_post_button);
        PostDescription = (EditText) findViewById(R.id.post_description);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        PostRef = FirebaseDatabase.getInstance().getReference().child("Posts");

        loadingBar = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
        current_user_id = mAuth.getCurrentUser().getUid();


        mToolbar = (Toolbar) findViewById(R.id.update_post_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Update Post");

        PostImageReference = FirebaseStorage.getInstance().getReference();
        
        SelectPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenGallery();
            }
        });
        UpdatePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validatePostInfo();
            }
        });
    }

    private void OpenGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType(("image/*"));
        startActivityForResult(galleryIntent,Gallery_Pick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==Gallery_Pick && resultCode== RESULT_OK && data != null)
        {
            ImageUri = data.getData();
            SelectPostImage.setImageURI(ImageUri);
        }


    }

    private void validatePostInfo() {
        Description = PostDescription.getText().toString();
        if(ImageUri == null)
        {
            Toast.makeText(this,"Please select post image...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(Description))
        {
            Toast.makeText(this,"Please say something about your image...", Toast.LENGTH_SHORT).show();
        }
        else
        {

            loadingBar.setTitle("Add new Post");
            loadingBar.setMessage("Please wait, while we are updating your new post...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);
            StoringImageToFirebaseStorage();
        }
    }

    private void StoringImageToFirebaseStorage() {
        Calendar calFordDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyyy");
        saveCurrentDate = currentDate.format(calFordDate.getTime());

        Calendar calFordTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calFordDate.getTime());

        postRandomName = saveCurrentDate + saveCurrentTime;

         StorageReference filepath = PostImageReference.child("Post Images")
                 .child(ImageUri.getLastPathSegment()+postRandomName+ ".jpg");

         filepath.putFile(ImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
             @Override
             public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                 if(task.isSuccessful())
                 {

                     Toast.makeText(PostActivity.this, "image uploaded successfully to Storage", Toast.LENGTH_SHORT).show();
                     downloadUrl = task.getResult().getDownloadUrl().toString();
                     SavingPostInformationToDatabase();

                 }
                 else
                 {
                     String message = task.getException().getMessage();
                     Toast.makeText(PostActivity.this, "Error Occured:" + message, Toast.LENGTH_SHORT).show();
                 }
             }
         });
    }

    private void SavingPostInformationToDatabase() {

         PostRef.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(DataSnapshot dataSnapshot) {
                 if(dataSnapshot.exists())
                 {
                     countPosts = dataSnapshot.getChildrenCount();
                 }
                 else
                 {
                     countPosts = 0;
                 }
             }

             @Override
             public void onCancelled(DatabaseError databaseError) {

             }
         });

      usersRef.child(current_user_id).addValueEventListener(new ValueEventListener() {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
              if(dataSnapshot.exists())
              {
                  String userFullName = dataSnapshot.child("fullname").getValue().toString();
                  String userProfileImage = dataSnapshot.child("profileimage").getValue().toString();
                  HashMap postsmap = new HashMap();
                  postsmap.put("uid",current_user_id);
                  postsmap.put("date",saveCurrentDate);
                  postsmap.put("time",saveCurrentTime);
                  postsmap.put("description",Description);
                  postsmap.put("postimage",downloadUrl);
                  postsmap.put("profileimage",userProfileImage);
                  postsmap.put("fullname",userFullName);
                  postsmap.put("counter", countPosts);

                  PostRef.child(current_user_id+postRandomName).updateChildren(postsmap)
                          .addOnCompleteListener(new OnCompleteListener() {
                              @Override
                              public void onComplete(@NonNull Task task) {
                                   if(task.isSuccessful())
                                   {
                                       SendUserToMainActivity();
                                       Toast.makeText(PostActivity.this, "New Post is updated successfully", Toast.LENGTH_SHORT ).show();
                                       loadingBar.dismiss();
                                   }
                                   else
                                   {
                                       Toast.makeText(PostActivity.this, "Error occured while updating", Toast.LENGTH_SHORT ).show();
                                       loadingBar.dismiss();
                                   }
                              }
                          });

              }
          }

          @Override
          public void onCancelled(DatabaseError databaseError) {

          }
      });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id== android.R.id.home)
        {
            SendUserToMainActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    private void SendUserToMainActivity(){
        Intent mainIntent = new Intent(PostActivity.this, MainActivity.class);
        startActivity(mainIntent);
    }
}
