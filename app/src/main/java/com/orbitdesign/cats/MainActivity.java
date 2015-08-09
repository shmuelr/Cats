package com.orbitdesign.cats;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity{

    private static final String TAG = "MainActivity";
    private static final String API_URL = "http://thecatapi.com/api/images/get?format=src&results_per_page=1";

    private ProgressBar progressBar;
    private static Bitmap largeBitmap = null;

    private SubsamplingScaleImageView mPhotoImageView;
    private RelativeLayout relativeLayout;

    private int backgroundColor = 0;

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

        if(largeBitmap !=null ){
            useBitmap(largeBitmap);
        }else{
            refreshImage();
        }


    }

    public void refreshImage(){
        new AsycBitmapFromURL(new AsycBitmapFromURL.LoadingCallback() {
            @Override
            public void onStartLoad() {
                setLoading(true);
            }

            @Override
            public void onFinishLoad(Bitmap bitmap) {
                if (bitmap != null) {
                    largeBitmap = bitmap;
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
        }

        return super.onOptionsItemSelected(item);
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


    private static class AsycBitmapFromURL extends AsyncTask<String, Void, Bitmap>{

        private static final OkHttpClient okHttpClient = new OkHttpClient();

        private LoadingCallback callback;

        public AsycBitmapFromURL(LoadingCallback callback){
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            callback.onStartLoad();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Request request = new Request.Builder().url(params[0]).build();
            Bitmap bitmap = null;
            try {
                InputStream is = okHttpClient.newCall(request).execute().body().byteStream();

                bitmap = BitmapFactory.decodeStream(is);

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }


            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);



           callback.onFinishLoad(bitmap);
           callback = null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            callback = null;
        }

        interface LoadingCallback{
            public void onStartLoad();
            public void onFinishLoad(Bitmap bitmap);
        }
    }

}
