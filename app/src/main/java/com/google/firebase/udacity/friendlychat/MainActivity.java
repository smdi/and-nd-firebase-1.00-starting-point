/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.udacity.friendlychat.FriendlyMessage;
import com.google.firebase.udacity.friendlychat.MessageAdapter;
import com.google.firebase.udacity.friendlychat.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "Imran";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;

    private static final int RC_PHOTO_PICKER = 2 ;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private ChildEventListener childEventListener;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private FirebaseStorage mfireStr;
    private StorageReference mstorgeRef;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    private String mUsername;

    private String uri ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;



        firebaseDatabase =FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        mfireStr = FirebaseStorage.getInstance();


        databaseReference = firebaseDatabase.getReference().child("child");
        mstorgeRef = mfireStr.getReference().child("files");

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);


        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);


        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click

                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(),ANONYMOUS,  null);
                databaseReference.push().setValue(friendlyMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });


        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                if(firebaseUser!=null)
                {

                    onSignedIn(firebaseUser.getDisplayName());

                }
                else
                {
                    startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                                                                .setIsSmartLockEnabled(false)
                                                            .setProviders(
                                                                   AuthUI.EMAIL_PROVIDER, AuthUI.GOOGLE_PROVIDER)
                                                           .build(), RC_SIGN_IN);


                    onSignedOut();
                }
            }
        };

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){

            if(resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "signed in", Toast.LENGTH_SHORT).show();
            }

        }
        if(requestCode == RESULT_CANCELED)
        {

            Toast.makeText(getApplicationContext(),"sign in cancelled",Toast.LENGTH_SHORT).show();
            finish();
        }

        else  if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK)
        {

            Uri photouri = data.getData();

            StorageReference mref= mstorgeRef.child(photouri.getLastPathSegment());
            mref.putFile(photouri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                    Toast.makeText(getApplicationContext(),"Retreval part" ,Toast.LENGTH_SHORT);
                    Uri downloadurl = taskSnapshot.getDownloadUrl();

                    Uri downloadUri = taskSnapshot.getMetadata().getDownloadUrl();

//                    uri = ""+downloadUri;

//                    FriendlyMessage friendlyMessage1 = new FriendlyMessage(downloadUri.toString(), mUsername ,null);
//                    databaseReference.push().setValue(friendlyMessage1);

                    FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername ,downloadurl.toString());
                    databaseReference.push().setValue(friendlyMessage);

                }
            });
        }


    }

    private void onSignedIn(String displayName) {
        mUsername = displayName;
       attchDatabaseListener();
    }

    private void onSignedOut() {
     mUsername = ANONYMOUS;
     mMessageAdapter.clear();
     detachlistener();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (authStateListener!=null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
        detachlistener();
        mMessageAdapter.clear();
    }
    @Override
    protected void onResume() {
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
               return  true;
            default:
                finish();
                return super.onOptionsItemSelected(item);

        }

    }

    public void attchDatabaseListener()
    {


        if(childEventListener==null) {

            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            databaseReference.addChildEventListener(childEventListener);

        }
    }

    public void  detachlistener()
    {

        if(childEventListener!=null) {
            databaseReference.removeEventListener(childEventListener);
            childEventListener=null;
        }
    }
}
