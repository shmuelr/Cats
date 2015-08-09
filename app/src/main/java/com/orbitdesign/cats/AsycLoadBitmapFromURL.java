package com.orbitdesign.cats;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by user on 8/9/15.
 */
public class AsycLoadBitmapFromURL extends AsyncTask<String, Void, Bitmap> {

    private static final OkHttpClient okHttpClient = new OkHttpClient();
    private static final String TAG = AsycLoadBitmapFromURL.class.getName();

    private LoadingCallback callback;

    public AsycLoadBitmapFromURL(LoadingCallback callback){
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
