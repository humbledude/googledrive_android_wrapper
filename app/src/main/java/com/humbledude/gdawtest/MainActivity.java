package com.humbledude.gdawtest;

import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.humbledude.googledriveandroidwrapper.GoogleDriveAndroidWrapper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = "GDTest";

    private ListView mListView;
    private ArrayAdapter<String> mArrayAdapter;

    private GoogleDriveAndroidWrapper gdaw;

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 0;
    private static final int CREATE_NEW_FILE2 = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.list_item);
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mListView.setAdapter(mArrayAdapter);

        setFunctions();

    }

    private void setFunctions() {
        List<String> functions = new ArrayList<>();
        functions.add("create new folder");
        functions.add("create new empty file");
        functions.add("create new empty file 2");
        functions.add("create new text file");
        functions.add("query with file name");
        functions.add("create file in folder");

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (gdaw == null) {
                    Log.d(TAG, "Google Drive has not been connected");
                    return;
                }
                Log.d(TAG, "position : " + position + " id : " + id);
                switch (position) {
                    case 0:
                        gdaw.createNewFolder("new folder");
                        break;
                    case 1:
                        gdaw.createNewEmptyFile("new file");
                        break;
                    case 2:
                        IntentSender intentSender = gdaw.createNewEmptyFile2("new file2");
                        try {
                            startIntentSenderForResult(intentSender, CREATE_NEW_FILE2, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            // Handle the exception
                        }
                        break;
                    case 3:
                        gdaw.createNewTextFile("test.txt");
                        break;
                    case 4:
                        gdaw.queryFileName("new file2");
                        break;
                    case 5:
                        gdaw.createFileInFolder("file", "folder1/folder2");
                        break;
                }

            }
        });
        mArrayAdapter.addAll(functions);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        gdaw = new GoogleDriveAndroidWrapper(this, this, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    gdaw.connect();
                }
                break;
            case CREATE_NEW_FILE2:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "create new file2 success", Toast.LENGTH_SHORT).show();
                }
        }
    }

    // GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected()");
        Toast.makeText(this, "google drive connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    // GoogleApiClient.OnConnectionFailedListener
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed()");
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }

    }

}
