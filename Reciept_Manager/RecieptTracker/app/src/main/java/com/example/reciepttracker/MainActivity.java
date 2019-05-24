package com.example.reciepttracker;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    public static Button button1;
    public static Button button2;
    public static Button button3;
    public static Button button4;
    public static TextView textView;

    public static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "Reciept_Tracker";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button1 = (Button) findViewById(R.id.button1 );
        button2 = (Button) findViewById(R.id.button2 );
        button3 = (Button) findViewById(R.id.button3 );
        button4 = (Button) findViewById(R.id.button4 );
        textView = (TextView) findViewById(R.id.textView);

        AndroidAuthSession session = buildSession();
        dropboxAPI = new DropboxAPI<AndroidAuthSession>(session);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static DropboxAPI<AndroidAuthSession> dropboxAPI;
    private static final String APP_KEY = "9mkov45qzp73lwc";
    private static final String APP_SECRET = "zvcscw58tdh6npk";
    private static final String ACCESSTOKEN = "khr9EwKtTSAAAAAAAAAADSAJBb65LGa1veqVLoJA8bkB76CuCsW8awLo1inQW7cI";
    private DropboxAPI.UploadRequest request;
    private AndroidAuthSession buildSession()
    {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        session.setOAuth2AccessToken(ACCESSTOKEN);
        return session;
    }
    static final int UploadFromSelectApp = 9501;
    static final int UploadFromFilemanager = 9502;
    public static String DropboxUploadPathFrom = "";
    public static String DropboxUploadName = "";
    public static String DropboxDownloadPathFrom = "";
    public static String DropboxDownloadPathTo = "";

    private void UploadToDropboxFromPath (String uploadPathFrom, String uploadPathTo)
    {
        Toast.makeText(getApplicationContext(), "Upload file ...", Toast.LENGTH_SHORT).show();
        final String uploadPathF = uploadPathFrom;
        final String uploadPathT = uploadPathTo;
        Thread th = new Thread(new Runnable()
        {
            public void run()
            {
                File tmpFile = null;
                try
                {
                    tmpFile = new File(uploadPathF);
                }
                catch (Exception e) {e.printStackTrace();}
                FileInputStream fis = null;
                try
                {
                    fis = new FileInputStream(tmpFile);
                }
                catch (FileNotFoundException e) {e.printStackTrace();}
                try
                {
                    dropboxAPI.putFileOverwrite(uploadPathT, fis, tmpFile.length(), null);
                }
                catch (Exception e) {}
                getMain().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "File successfully uploaded.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        th.start();
    }

    private void UploadToDropboxFromSelectedApp (String uploadName)
    {
        DropboxUploadName = uploadName;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Upload from ..."), UploadFromSelectApp);
    }

    private void UploadToDropboxFromFilemanager (String uploadName)
    {
        DropboxUploadName = uploadName;
        Intent intent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        intent.putExtra("CONTENT_TYPE", "*/*");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent, UploadFromFilemanager);
    }

    private void DownloadFromDropboxFromPath (String downloadPathTo, String downloadPathFrom)
    {
        DropboxDownloadPathTo = downloadPathTo;
        DropboxDownloadPathFrom = downloadPathFrom;
        runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Download file ...", Toast.LENGTH_SHORT).show();
                Thread th = new Thread(new Runnable()
                {
                    public void run()
                    {
                        File file = new File(DropboxDownloadPathTo + DropboxDownloadPathFrom.substring(DropboxDownloadPathFrom.lastIndexOf('.')));
                        if (file.exists()) file.delete();
                        try
                        {
                            FileOutputStream outputStream = new FileOutputStream(file);
                            MainActivity.dropboxAPI.getFile(DropboxDownloadPathFrom, null, outputStream, null);
                            getMain().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Toast.makeText(getApplicationContext(), "File successfully downloaded.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
                th.start();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (requestCode == UploadFromFilemanager)
        {
            final Uri currFileURI = intent.getData();
            final String pathFrom = currFileURI.getPath();
            Toast.makeText(getApplicationContext(), "Upload file ...", Toast.LENGTH_SHORT).show();
            Thread th = new Thread(new Runnable()
            {
                public void run()
                {
                    getMain().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            UploadToDropboxFromPath(pathFrom, "/db-test/" + DropboxUploadName + pathFrom.substring(pathFrom.lastIndexOf('.')));
                            Toast.makeText(getApplicationContext(), "File successfully uploaded.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            th.start();
        }
        if (requestCode == UploadFromSelectApp)
        {
            Toast.makeText(getApplicationContext(), "Upload file ...", Toast.LENGTH_SHORT).show();
            final Uri uri = intent.getData();

            DropboxUploadPathFrom = getPath(getApplicationContext(), uri);
            if(DropboxUploadPathFrom == null) {
                DropboxUploadPathFrom = uri.getPath();
            }
            Thread th = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        final File file = new File(DropboxUploadPathFrom);
                        InputStream inputStream = getContentResolver().openInputStream(uri);

                        dropboxAPI.putFile("/db-test/" + DropboxUploadName + file.getName().substring(file.getName().lastIndexOf("."),
                                file.getName().length()), inputStream, file.length(), null, new ProgressListener()
                        {
                            @Override
                            public long progressInterval() {return 100;}
                            @Override
                            public void onProgress(long arg0, long arg1){}
                        });
                        getMain().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Toast.makeText(getApplicationContext(), "File successfully uploaded.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    catch (Exception e) {e.printStackTrace();}
                }
            });
            th.start();
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    public String getPath(Context context, Uri contentUri) {
        Cursor cursor = null;
        try
        {
            String[] proj = { MediaStore.Images.Media.DATA, MediaStore.Video.Media.DATA, MediaStore.Audio.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String s = cursor.getString(column_index);
            if(s!=null)
            {
                cursor.close();
                return s;
            }
        }
        catch(Exception e){}
        try
        {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            String s = cursor.getString(column_index);
            if(s!=null)
            {
                cursor.close();
                return s;
            }
        }
        catch(Exception e){}
        try
        {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            String s = cursor.getString(column_index);
            cursor.close();
            return s;
        }
        finally
        {
            if (cursor != null)
                cursor.close();
        }
    }

    public MainActivity getMain()
    {
        return this;
    }
}
