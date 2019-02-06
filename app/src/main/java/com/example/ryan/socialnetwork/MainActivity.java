package com.example.ryan.socialnetwork;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity {


    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private RecyclerView postList;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Toolbar mToolbar;
    private CircleImageView NavProfileImage;
    private TextView NavProfileUsername;
    private ImageButton AddNewPostButton;


    private FirebaseAuth mAuth;
   private DatabaseReference UsersRef, PostsRef, LikesRef;

   String currentUserID;
  Boolean LikeChecker = false;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
         PostsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

         drawerLayout = (DrawerLayout)findViewById(R.id.drawable_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);


       postList = (RecyclerView) findViewById(R.id.all_user_post_list);
       postList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        postList.setLayoutManager(linearLayoutManager);


        View navView = navigationView.inflateHeaderView(R.layout.navigation_header);
        NavProfileImage = (CircleImageView) navView.findViewById(R.id.nav_profile_image);
        NavProfileUsername = (TextView) navView.findViewById(R.id.nav_user_full_name);




        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {

                    if(dataSnapshot.hasChild("fullname"))
                    {
                        String fullname = dataSnapshot.child("fullname").getValue().toString();
                        NavProfileUsername.setText(fullname);
                    }
                    if(dataSnapshot.hasChild("profileimage")){
                        String image = dataSnapshot.child("profileimage").getValue().toString();


                        Picasso.get().load(image).placeholder(R.drawable.profile).into(NavProfileImage);
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this,"Profile Name do not exist..",Toast.LENGTH_SHORT).show();
                    }





                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mToolbar = (Toolbar) findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Home");

        AddNewPostButton = (ImageButton) findViewById(R.id.add_new_post_button);


        actionBarDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, drawerLayout, R.string.draw_open, R.string.draw_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

               UserMenuSelector(item);

                return false;
            }
        });

        AddNewPostButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                SendUserToPostActivity();
            }
        });

        DisplayAllUserPosts();
    }

    public void updateUserStatus(String state)
    {
        String  saveCurrentDate, saveCurrentTime;
        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calForDate.getTime());

        Calendar calForTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calForTime.getTime());

         Map currentStateMap = new HashMap();
        currentStateMap.put("time", saveCurrentTime);
        currentStateMap.put("date", saveCurrentDate);
        currentStateMap.put("type", state);

        UsersRef.child(currentUserID).child("userState")
                .updateChildren(currentStateMap);


    }



    private void DisplayAllUserPosts() {

        Query SortPostsDescendingOrder = PostsRef.orderByChild("counter");




        FirebaseRecyclerAdapter<Posts,PostsViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<Posts, PostsViewHolder>(
                        Posts.class,
                        R.layout.all_post_layout,
                        PostsViewHolder.class,
                        SortPostsDescendingOrder

                ) {
                    @Override
                    protected void populateViewHolder(PostsViewHolder viewHolder, Posts model, int position) {

                        final String PostKey = getRef(position).getKey();

                        viewHolder.setFullname(model.getFullname());
                        viewHolder.setTime(model.getTime());
                        viewHolder.setDate(model.getDate());
                        viewHolder.setDescription(model.getDescription());
                        viewHolder.setProfileimage(getApplicationContext(), model.getProfileimage());
                        viewHolder.setPostimage(getApplicationContext(), model.getPostimage());

                        viewHolder.setLikeButtonStatus(PostKey);


                         viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 Intent clickPostIntent = new Intent(MainActivity.this, ClickPostActivity.class);
                                 clickPostIntent.putExtra("PostKey",PostKey);
                                 startActivity(clickPostIntent);
                             }
                         });

                         viewHolder.CommentPostButton.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                 Intent commentsIntent = new Intent(MainActivity.this, CommentActivity.class);
                                 commentsIntent.putExtra("PostKey",PostKey);
                                 startActivity(commentsIntent);
                             }
                         });

                         viewHolder.LikePostButton.setOnClickListener(new View.OnClickListener() {
                             @Override
                             public void onClick(View v) {
                                LikeChecker = true;
                                LikesRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                           if(LikeChecker.equals(true))
                                           {
                                               if(dataSnapshot.child(PostKey).hasChild(currentUserID))
                                               {
                                                   LikesRef.child(PostKey).child(currentUserID).removeValue();
                                                   LikeChecker = false;
                                               }
                                               else
                                               {
                                                   LikesRef.child(PostKey).child(currentUserID).setValue(true);
                                                   LikeChecker = false;
                                               }
                                           }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                             }
                         });

                    }
                };

        postList.setAdapter(firebaseRecyclerAdapter);

        updateUserStatus("online");

    }

    public static class PostsViewHolder extends RecyclerView.ViewHolder
    {

        ImageButton LikePostButton, CommentPostButton;
        TextView DisplayNoOfLikes;
        int countLikes;
        String currentUserId;
        DatabaseReference LikesRef;

        View mView;
        public PostsViewHolder(View itemView)
        {
            super(itemView);
            mView = itemView;
            LikePostButton = (ImageButton)mView.findViewById(R.id.like_button);
            CommentPostButton = (ImageButton)mView.findViewById(R.id.comment_button);
            DisplayNoOfLikes = (TextView)mView.findViewById(R.id.display_no_of_likes);

            LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        }

        public void setLikeButtonStatus(final String PostKey)
        {
            LikesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                   if(dataSnapshot.child(PostKey).hasChild(currentUserId))
                   {
                       countLikes = (int) dataSnapshot.child(PostKey).getChildrenCount();
                       LikePostButton.setImageResource(R.drawable.like);
                       DisplayNoOfLikes.setText(Integer.toString(countLikes)+ " Likes");
                   }
                   else
                   {
                       countLikes = (int) dataSnapshot.child(PostKey).getChildrenCount();
                       LikePostButton.setImageResource(R.drawable.dislike);
                       DisplayNoOfLikes.setText(Integer.toString(countLikes)+" Likes");
                   }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        public void setFullname(String fullname)
        {
            TextView username = (TextView) mView.findViewById(R.id.post_user_name);
            username.setText(fullname);
        }
        public void setProfileimage(Context ctx, String profileimage)
        {
            CircleImageView image = (CircleImageView) mView.findViewById(R.id.post_profile_image);
            Picasso.get().load(profileimage).into(image);
        }
        public void setTime(String time)
        {
            TextView PostTime = (TextView) mView.findViewById(R.id.post_time);
            PostTime.setText(" " +time);
        }
        public void setDate(String date)
        {
            TextView PostDate = (TextView) mView.findViewById(R.id.post_date);
            PostDate.setText(" "+date);
        }
        public void setDescription(String description)
        {
            TextView PostDescription = (TextView) mView.findViewById(R.id.post_description);
            PostDescription.setText(description);
        }
        public void setPostimage(Context ctx,String postimage)
        {
           ImageView PostImage = (ImageView) mView.findViewById(R.id.post_image);
            Picasso.get().load(postimage).into(PostImage);
        }

    }


    private void SendUserToPostActivity() {
        Intent addNewPostIntent = new Intent(MainActivity.this, PostActivity.class);
        startActivity(addNewPostIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null)
        {
            SendUserToLoginActivity();
        }
        else
        {
            CheckUserExistence();
        }
    }

    private void CheckUserExistence() {
        final String current_user_id = mAuth.getCurrentUser().getUid();

        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChild(current_user_id))
                {
                    SendUserToSetupActivity();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void SendUserToSetupActivity() {
        Intent SetupIntent = new Intent(MainActivity.this,SetupActivity.class);
        SetupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(SetupIntent);
        finish();
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this,LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(actionBarDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void UserMenuSelector(MenuItem item) {

        switch(item.getItemId())
        {
            case R.id.nav_post:
                SendUserToPostActivity();
                break;
            case R.id.nav_profile:
                SendUserToProfileActivity();
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_home:
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_friends:
                SendUserToFriendsActivity();
                Toast.makeText(this, "Friend List", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_find_friends:
                SendUserToFindFriendsActivity();
                Toast.makeText(this, "Find Friends", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_messages:
                SendUserToFriendsActivity();
                Toast.makeText(this, "Message", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_settings:
                SendUserToSettingActivity();
                Toast.makeText(this, "Setting", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_logout:
                updateUserStatus("offline");
                mAuth.signOut();
                SendUserToLoginActivity();
                break;
        }
    }
    private void SendUserToFriendsActivity() {
        Intent friendsIntent = new Intent(MainActivity.this,FriendsActivity.class);
        startActivity(friendsIntent);

    }
    private void SendUserToSettingActivity() {
        Intent loginIntent = new Intent(MainActivity.this,SettingActivity.class);
        startActivity(loginIntent);

    }
    private void SendUserToProfileActivity() {
        Intent loginIntent = new Intent(MainActivity.this,ProfileActivity.class);
        startActivity(loginIntent);

    }
    private void SendUserToFindFriendsActivity() {
        Intent loginIntent = new Intent(MainActivity.this,FindFriendsActivity.class);
        startActivity(loginIntent);

    }
}
