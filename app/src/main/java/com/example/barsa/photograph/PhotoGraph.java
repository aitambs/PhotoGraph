package com.example.barsa.photograph;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class PhotoGraph extends AppCompatActivity {
    RecyclerView recyclerView;
    CheckBox checkBox;
    static boolean exifOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_graph);
        recyclerView=findViewById(R.id.rv);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this,4);
        recyclerView.setLayoutManager(layoutManager);
        checkBox=findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                verifyPermission();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        verifyPermission();
    }

    private  void verifyPermission(){
        if (Build.VERSION.SDK_INT>=23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //Show reason why permission is needed.
                    new AlertDialog.Builder(this).setMessage("Must have permission for this feature!").setCancelable(true)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @SuppressLint("NewApi")
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},3);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
                    return;
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},3);
                    return;
                }
            }
        }
        loadImages();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadImages();
        }
    }



    private void loadImages(){
        exifOnly=checkBox.isChecked();
        Thread background= new Thread(new Runnable(){
            @Override
            public void run() {
                Cursor cursor;
                int column_index_data;
                final ArrayList<String> imagesArray = new ArrayList<String>();
                String imagePath=null;
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String[] projection = { MediaStore.MediaColumns.DATA,
                        MediaStore.Images.Media.DATE_MODIFIED };

                cursor = getContentResolver().query(uri, projection, null, null, "date_modified DESC");

                column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                TreeMap<String,Integer> dateData = new TreeMap<>();
                String fileLocation, fileDate;
                Integer amount=1;
                while (cursor.moveToNext()) {
                    fileLocation=cursor.getString(column_index_data);
                    imagesArray.add(fileLocation);
                    fileDate=getDate(fileLocation);
                    if (fileDate == null) {
                        continue;
                    }
                    amount= dateData.get(fileDate);
                    if (amount != null){
                        dateData.put(fileDate,amount+1);
                    } else dateData.put(fileDate,1);
                }
                final BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
                double counter=0, numOfPhotos=0;
                for (Map.Entry<String, Integer> entry: dateData.entrySet()){
                    counter++;
                    if (numOfPhotos<entry.getValue()) numOfPhotos=entry.getValue();
                    series.appendData(new DataPoint(counter,entry.getValue()),false,dateData.size(),true);
                }
                final double maxX=counter+1;
                final double maxY=numOfPhotos;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PhotoGraph.this, "Done: "+imagesArray.size()+" photos loaded", Toast.LENGTH_SHORT).show();
                        PhotoAdapter photoAdapter=new PhotoAdapter(imagesArray);
                        recyclerView.setAdapter(photoAdapter);
                        GraphView graph = (GraphView) findViewById(R.id.graph);
                        graph.onDataChanged(true,false);
                        graph.removeAllSeries();
                        graph.addSeries(series);
                        graph.getViewport().setXAxisBoundsManual(true);
                        graph.getViewport().setYAxisBoundsManual(true);
                        graph.getViewport().setMinY(0);
                        graph.getViewport().setMaxY(maxY);
                        graph.getViewport().setMinX(0);
                        graph.getViewport().setMaxX(maxX);

                        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
                        staticLabelsFormatter.setHorizontalLabels(new String[] {"old", "middle", "new"});
                        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
                    }
                });

            }
        });
        background.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }



    static String getDate(String fileName){
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(fileName);
        } catch (IOException e){}

        if (exifInterface == null || exifInterface.getAttribute(ExifInterface.TAG_DATETIME)==null) {
            if (exifOnly) return null;
            File file = new File(fileName);
            Date date= new Date(file.lastModified());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd");
            return sdf.format(date);
        } else {
            return exifInterface.getAttribute(ExifInterface.TAG_DATETIME).substring(0,10);
        }
    }
}
