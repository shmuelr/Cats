package com.orbitdesign.cats;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity{

    private static final String TAG = "MainActivity";
    private static final String API_URL = "http://thecatapi.com/api/images/get?format=src&results_per_page=1";

    private static final String APP_DIRECTORY = "/Cats";

    private ProgressBar progressBar;
    private static Bitmap largeBitmap = null;

    private SubsamplingScaleImageView mPhotoImageView;
    private RelativeLayout relativeLayout;

    private int backgroundColor = 0;
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");

    private static String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpGui();
    }

    private void setUpGui() {
        mPhotoImageView = (SubsamplingScaleImageView)findViewById(R.id.photoImageView);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        relativeLayout = (RelativeLayout)findViewById(R.id.layoutBack);

        if(largeBitmap != null ){
            useBitmap(largeBitmap);
        }else{
            refreshImage();
        }


    }

    public void refreshImage(){
        new AsycLoadBitmapFromURL(new AsycLoadBitmapFromURL.LoadingCallback() {
            @Override
            public void onStartLoad() {
                setLoading(true);
            }

            @Override
            public void onFinishLoad(Bitmap bitmap) {
                if (bitmap != null) {
                    largeBitmap = bitmap;
                    updateFileName();
                    useBitmap(largeBitmap);

                }else{
                    Log.e(TAG, "Bitmap is null");
                    Toast.makeText(MainActivity.this, "Oops, there was a mistake. Please contact me so I can fix it. Thanks!", Toast.LENGTH_LONG).show();
                }
                setLoading(false);
            }
        }).execute(API_URL);

    }

    public void useBitmap(Bitmap bitmap){

        mPhotoImageView.setImage(ImageSource.bitmap(bitmap));
        usePalette(Palette.from(bitmap).generate());
    }

    public void usePalette(Palette palette){
        //Get swatch. If one is null try the next one
        Palette.Swatch swatch = palette.getMutedSwatch();
        if(swatch == null) swatch = palette.getDarkMutedSwatch();
        if(swatch == null) swatch = palette.getLightMutedSwatch();
        if(swatch == null) return;

        ColorDrawable[] color = {new ColorDrawable(backgroundColor != 0? backgroundColor : Color.WHITE), new ColorDrawable(swatch.getRgb())};
        backgroundColor = swatch.getRgb();
        TransitionDrawable trans = new TransitionDrawable(color);
        //This will work also on old devices. The latest API says you have to use setBackground instead.
        relativeLayout.setBackground(trans);
        trans.startTransition(500);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            Log.d(TAG, "Refresh pressed");

            refreshImage();
            return true;
        }else if (id == R.id.action_share) {
            sharePhoto();
            return true;
        }else if (id == R.id.action_set_as_wallpaper) {
            promptAndSetPhotoAsWallpaper();
            return true;
        }else if (id == R.id.action_save_to_phone) {
            Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show();
            savePhoto();
            return true;
        }else if (id == android.R.id.home) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Uri getPhotoUri(){
        File path = new File(Environment.getExternalStorageDirectory().toString()+ APP_DIRECTORY);
        File file = new File(path, getImageTitle()+".jpg");
        Uri photoLocation;
        if(file.exists()){
            photoLocation = Uri.fromFile(file);
        }else {
            photoLocation = savePhoto();
        }
        return photoLocation;
    }

    private void sharePhoto() {

        final Intent shareIntent = new Intent(     android.content.Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.putExtra(Intent.EXTRA_STREAM, getPhotoUri());
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
        shareIntent.setType("image/jpg");
        startActivity(shareIntent);
    }

    private Uri savePhoto() {

        File path = new File(Environment.getExternalStorageDirectory().toString()+APP_DIRECTORY);

        if(!path.exists()){
            if(!path.mkdir()) Toast.makeText(this, "Oops. Could not create folder. Please send us an email so we can fix this.", Toast.LENGTH_LONG).show();
        }

        File file = new File(path, getImageTitle()+".jpg"); // the File to save to

        Log.d(TAG, "File name = " + file.getName());
        Log.d(TAG, "File path = " + file.getAbsolutePath());
        Log.d(TAG, "Path name = " + path);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            getPhotoBitmap().compress(Bitmap.CompressFormat.JPEG, 85, out); // bmp is your Bitmap instance



        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "Oops. There was an error. Please send us an email so we can fix this.", Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(file.exists()){
            Uri uri = addImageToGallery(this, file.toString(), getImageTitle(), "from Cats app");

            return uri;
        }else{
            return null;
        }

    }

    public Uri addImageToGallery(Context context, String filepath, String title, String description) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, description);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filepath);

        return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void promptAndSetPhotoAsWallpaper() {

        Intent setAs = new Intent(Intent.ACTION_ATTACH_DATA);
        setAs.setDataAndType(getPhotoUri(), "image/jpg");
        setAs.putExtra("mimeType", "image/jpg");
        startActivity(Intent.createChooser(setAs, "Set Image As"));
    }

    public void setLoading(boolean isLoading){
        if(isLoading){
            progressBar.setVisibility(View.VISIBLE);
            mPhotoImageView.setVisibility(View.GONE);
        }else{
            progressBar.setVisibility(View.GONE);
            mPhotoImageView.setVisibility(View.VISIBLE);
        }
    }


    public Bitmap getPhotoBitmap() {
        return largeBitmap;
    }

    public String getImageTitle(){
        if(fileName == null){
            fileName = "Cats-" + SIMPLE_DATE_FORMAT.format(Calendar.getInstance().getTime());
        }
        return fileName;
    }

    public void updateFileName(){
        fileName = "Cats-" + SIMPLE_DATE_FORMAT.format(Calendar.getInstance().getTime());
    }
}
