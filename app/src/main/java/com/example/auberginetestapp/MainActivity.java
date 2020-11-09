package com.example.auberginetestapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private final int REQUEST_EXTERNAL_STORAGE = 435;
    private final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    String randomQuoteUrl, toggledQuoteUrl;
    public static RequestQueue requestQueue;

    private String quoteString;
    private String authorName;
    private String translatedQuote;
    private String id;

    TextView quote;
    TextView author;
    Button generate;
    Button toggle;
    Button getImg;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        quote = findViewById(R.id.quote);
        author = findViewById(R.id.author);
        generate = findViewById(R.id.generate);
        toggle = findViewById(R.id.toggle);
        getImg = findViewById(R.id.downloadImg);

        generate.setEnabled(true);
        toggle.setEnabled(false);
        getImg.setEnabled(false);
        progressBar = findViewById(R.id.progress);


        progressBar.setVisibility(View.GONE);

        //HTTP GET REQUEST -------------------------------------------------------------------------
        requestQueue = Volley.newRequestQueue(this);

        randomQuoteUrl = "https://programming-quotes-api.herokuapp.com/quotes/random";

        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                getQuote();
            }
        });

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                getToggled();
            }
        });

        getImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = quoteString + "\n" + authorName;
                try {
                    // Check if we have write permission
                    int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    if (permission != PackageManager.PERMISSION_GRANTED
                            && this != null) {
                        // We don't have permission so prompt the user
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                PERMISSIONS_STORAGE,
                                REQUEST_EXTERNAL_STORAGE
                        );
                    }

                    else
                        getImgOfQuote(text);

                } catch (IOException e) {
                    Log.i(TAG, "Exception: " + e);
                    e.printStackTrace();
                }
            }
        });

    }

    private void renderUI() {
        progressBar.setVisibility(View.GONE);
        author.setVisibility(View.VISIBLE);
        quote.setText(quoteString);
        author.setText(authorName);
        toggle.setEnabled(true);
        getImg.setEnabled(true);
    }

    private void updateUI() {
        progressBar.setVisibility(View.GONE);
        author.setVisibility(View.VISIBLE);
        quote.setText(quoteString);
    }

    void getImgOfQuote(final String text) throws IOException {


        final Rect bounds = new Rect();
        TextPaint textPaint = new TextPaint() {
            {
                setColor(Color.BLACK);
                setTextAlign(Paint.Align.LEFT);
                setTextSize(20f);
                setAntiAlias(true);
            }
        };
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        StaticLayout mTextLayout = new StaticLayout(text, textPaint,
                bounds.width(), Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
        int maxWidth = -1;
        for (int i = 0; i < mTextLayout.getLineCount(); i++) {
            if (maxWidth < mTextLayout.getLineWidth(i)) {
                maxWidth = (int) mTextLayout.getLineWidth(i);
            }
        }
        final Bitmap bmp = Bitmap.createBitmap(maxWidth , mTextLayout.getHeight(),
                Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.WHITE);// just adding black background
        final Canvas canvas = new Canvas(bmp);
        mTextLayout.draw(canvas);


        File file = new File(getImageFilePath());
        file.getParentFile().mkdirs();


        FileOutputStream stream = new FileOutputStream(file); //create your FileOutputStream here
        bmp.compress(Bitmap.CompressFormat.PNG, 85, stream);
        bmp.recycle();
        stream.close();

        while(!isCompletelyWritten(file)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.i(TAG, "InterruptedException while making thread sleep");
            }
        }
        Toast.makeText(this, "Saved image successfully in pictures folder", Toast.LENGTH_SHORT).show();

        Toast.makeText(this, "Image might take some time to appear in folder", Toast.LENGTH_SHORT).show();

        Log.i(TAG, "saved");
    }


    public boolean isCompletelyWritten(File file) {
        RandomAccessFile stream = null;
        try {
            stream = new RandomAccessFile(file, "rw");
            return true;
        } catch (Exception e) {
            Log.i(TAG, "Skipping file " + file.getName() + " for this iteration due it's not completely written");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.i(TAG, "Exception during closing random file " + file.getName());
                }
            }
        }
        return false;
    }


    public static String getImageFilePath() {
        return getAndroidImageFolder().getAbsolutePath() + "/" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + "_AubergineAutoQuote.png";
    }

    public static File getAndroidImageFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    }


    private void getQuote() {
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, randomQuoteUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(TAG, "Here");
                try {
                    quoteString = response.getString("en");
                    authorName = "-" + response.getString("author");
                    id = response.getString("id");


                    //render to the ui
                    renderUI();

                    //get toggled url
                    toggledQuoteUrl = " https://programming-quotes-api.herokuapp.com/quotes/id/" + id;

                    //if (isEmpty)
                      //  throw new Exception("No value for data");

                    Log.i(TAG, response.toString());

                } catch(JSONException e) {
                    Log.i(TAG, "JSONException in onResponse()" + e);
                    e.printStackTrace();
                } catch (Exception e) {
                    /*if(e.toString().equals("No value for data")) {
                        Log.i(TAG, "No value for data");
                        quote.setText("No data found, Please try again!");
                        author.setVisibility(View.GONE);
                    } else*/
                        Log.i(TAG, "Exception: " + e);
                        e.printStackTrace();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(TAG, "VolleyError: " + error);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headerMap = new HashMap<String, String>();
                headerMap.put("Content-Type", "application/json");
                return headerMap;
            }
        };
        jsonObjectRequest.setRetryPolicy(
                new DefaultRetryPolicy(0, -1,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonObjectRequest);

    }

    private void getToggled() {
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, toggledQuoteUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(TAG, "Here");
                try {
                    if(response.getString("sr").equals(""))
                        Toast.makeText(MainActivity.this, "No translation available!", Toast.LENGTH_LONG).show();
                    else
                        quoteString = response.getString("sr");


                    //render to the ui
                    updateUI();

                    //if (isEmpty)
                      //  throw new Exception("No value for data");

                    Log.i(TAG, response.toString());

                } catch(JSONException e) {
                    Log.i(TAG, "JSONException in onResponse()" + e);
                    e.printStackTrace();
                } catch (Exception e) {
                    /*if(e.toString().equals("No value for data")) {
                        Log.i(TAG, "No value for data");
                        quote.setText("No data found, Please try again!");
                        author.setVisibility(View.GONE);
                    } else*/
                        Log.i(TAG, "Exception: " + e);
                        e.printStackTrace();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(TAG, "VolleyError: " + error);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headerMap = new HashMap<String, String>();
                headerMap.put("Content-Type", "application/json");
                return headerMap;
            }
        };
        jsonObjectRequest.setRetryPolicy(
                new DefaultRetryPolicy(0, -1,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonObjectRequest);

    }
}