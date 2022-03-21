package com.example.imagepro;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

public class SampleImagesAdapter extends RecyclerView.Adapter<SampleImagesAdapter.MyViewHolder> {

    int[] imagesList;
    Activity context;


    public SampleImagesAdapter(Activity context, int[] imagesList) {
        this.imagesList = imagesList;
        this.context  = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Glide.with(context).load(imagesList[position]).into(holder.sampleImage);

        holder.sampleImage.setOnClickListener(view -> {
            Intent intent = new Intent(context, SampleImagePredictionActivity.class);
            intent.putExtra("image",imagesList[position]); // passing selected image to new activity
           context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return imagesList.length;
    }



   static class MyViewHolder extends RecyclerView.ViewHolder{

        ImageView sampleImage;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            sampleImage = itemView.findViewById(R.id.sample_image);
        }
    }
}


