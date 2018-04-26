package com.example.barsa.photograph;

import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    ArrayList<String> photoLocations;

    PhotoAdapter(ArrayList<String> list){
        photoLocations=list;
    }

    @NonNull
    @Override
    public PhotoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoAdapter.ViewHolder holder, int position) {
        holder.mTextview.setText(PhotoGraph.getDate(photoLocations.get(position)));
        Picasso.get().load(new File(photoLocations.get(position))).resize(200,200).centerCrop().into(holder.mImageview);
    }

    @Override
    public int getItemCount() {
        return photoLocations.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView mTextview;
        ImageView mImageview;
        ViewHolder(View v){
            super(v);
            mTextview=(TextView)v.findViewById(R.id.item_text);
            mImageview=(ImageView)v.findViewById(R.id.item_image);
        }
    }
}

