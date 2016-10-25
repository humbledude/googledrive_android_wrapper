package com.humbledude.googledriveandroidwrapper;

import android.content.Context;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;


/**
 * Created by keunhui.park on 2016. 10. 21..
 */

public class GoogleDriveAndroidWrapper {
    private static final String TAG = "GDAWrapper";

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;


    public GoogleDriveAndroidWrapper(Context context,
                                     GoogleApiClient.ConnectionCallbacks callback1,
                                     GoogleApiClient.OnConnectionFailedListener callback2) {
        mContext = context.getApplicationContext();
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(callback1)
                .addOnConnectionFailedListener(callback2)
                .build();
        mGoogleApiClient.connect();
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void createNewFolder(final String folderName) {
        new AsyncTask<Void, Void, DriveFolder.DriveFolderResult>() {
            @Override
            protected DriveFolder.DriveFolderResult doInBackground(Void... params) {
                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(folderName)
                        .build();
                return Drive.DriveApi.getRootFolder(mGoogleApiClient)
                        .createFolder(mGoogleApiClient, changeSet).await();

            }

            @Override
            protected void onPostExecute(DriveFolder.DriveFolderResult result) {
                super.onPostExecute(result);
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "create folder error : " + result.getStatus().getStatusMessage());
                } else {
                    Toast.makeText(mContext, "Create New Folder Success", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    public void createNewEmptyFile(String fileName) {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(fileName)
                .build();
        Drive.DriveApi.getRootFolder(mGoogleApiClient)
                .createFile(mGoogleApiClient, changeSet, null)
                .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                    @Override
                    public void onResult(@NonNull DriveFolder.DriveFileResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e(TAG, "create empty file error : " + result.getStatus().getStatusMessage());
                        } else {
                            Toast.makeText(mContext, "Create New Empty File Success", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public IntentSender createNewEmptyFile2(String fileName) {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(fileName)
                .build();
        IntentSender intentSender = Drive.DriveApi
                .newCreateFileActivityBuilder()
                .setInitialMetadata(changeSet)
                .setInitialDriveContents(null)
                .build(mGoogleApiClient);
        return intentSender;
    }

    public void createNewTextFile(final String fileName) {
        new AsyncTask<Void, Void, DriveFolder.DriveFileResult>() {

            @Override
            protected DriveFolder.DriveFileResult doInBackground(Void... params) {
                DriveApi.DriveContentsResult contentsResult = Drive.DriveApi.newDriveContents(mGoogleApiClient).await();

                OutputStream outputStream = contentsResult.getDriveContents().getOutputStream();
                Writer writer = new OutputStreamWriter(outputStream);
                try {
                    writer.write("Hello world!!");
                    writer.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(fileName)
                        .setMimeType("plain/text")
                        .build();

                return Drive.DriveApi.getRootFolder(mGoogleApiClient)
                        .createFile(mGoogleApiClient,changeSet, contentsResult.getDriveContents()).await();
            }

            @Override
            protected void onPostExecute(DriveFolder.DriveFileResult driveFileResult) {
                super.onPostExecute(driveFileResult);

                if (!driveFileResult.getStatus().isSuccess()) {
                    Log.e(TAG, "create empty file error : " + driveFileResult.getStatus().getStatusMessage());
                } else {
                    Toast.makeText(mContext, "Create New Text File Success", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    public void queryFileName(final String fileName) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Query query = new Query.Builder()
                        .addFilter(Filters.eq(SearchableField.TITLE, fileName))
                        .build();
                MetadataBuffer result = Drive.DriveApi.query(mGoogleApiClient, query).await().getMetadataBuffer();
                logMetadataBuffer(result);

                return null;
            }
        }.execute();
    }

    public void createFileInFolder(final String fileName, final String path) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                DriveId rootId = Drive.DriveApi.getRootFolder(mGoogleApiClient).getDriveId();
                DriveFolder rootFolder = rootId.asDriveFolder();

                DriveFolder targetFolder = stepIntoFolder(rootFolder, path);
                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(fileName)
                        .build();

                DriveFolder.DriveFileResult result = targetFolder.createFile(mGoogleApiClient, changeSet, null).await();

                return null;
            }
        }.execute();
    }


    private DriveFolder stepIntoFolder(DriveFolder currFolder, String path) {
        String[] nextFolder = splitFirstFolder(path);

        if ("".equals(nextFolder[0])) {
            return currFolder;
        }

        DriveFolder next = null;

        MetadataBuffer result = currFolder.listChildren(mGoogleApiClient).await().getMetadataBuffer();
        logMetadataBuffer(result);

        Iterator<Metadata> i = result.iterator();
        while (i.hasNext()) {
            Metadata data = i.next();
            if (data.getTitle().equals(nextFolder[0])
                    && (data.getDriveId().getResourceType() == DriveId.RESOURCE_TYPE_FOLDER)) {
                next = data.getDriveId().asDriveFolder();
            }
        }

        if (next == null) {
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(nextFolder[0])
                    .build();
            next = currFolder.createFolder(mGoogleApiClient, changeSet).await().getDriveFolder();
        }

        return stepIntoFolder(next, nextFolder[1]);
    }

    private String[] splitFirstFolder(String path) {
        String[] ret = new String[2];

        if ("".equals(path)) {
            ret[0] = "";
            ret[1] = "";
            return ret;
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        int index = path.indexOf("/");
        if (index > 0) {

            ret[0] = path.substring(0, index);
            ret[1] = path.substring(index, path.length());
        } else {
            ret[0] = path;
            ret[1] = "";
        }

        return ret;
    }

    private void logMetadataBuffer(MetadataBuffer result) {
        Iterator<Metadata> i = result.iterator();
        while (i.hasNext()) {
            Metadata data = i.next();
            Log.i(TAG, "title: " + data.getTitle() + " ID:" + data.getDriveId() );
        }
    }

}
