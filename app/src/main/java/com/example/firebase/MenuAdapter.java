package com.example.firebase;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.List;

public class MenuAdapter extends ArrayAdapter<MenuModel> {

    public MenuAdapter(@NonNull Context context, int resource,
                       List<MenuModel> objects) {
        super(context, resource, objects);
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null){
            convertView =
                    ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_menu, parent, false);
        }

        MenuModel menu = getItem(position);

        ImageView photoImageView =
                (ImageView) convertView.findViewById(R.id.photoImageView);
        TextView textView =
                (TextView) convertView.findViewById((R.id.messageTextView));
        Glide.with(photoImageView.getContext())
                .load(menu.getPhotoUrl())
                .into(photoImageView);
        textView.setText(menu.getMenuText());


        return convertView;
        };

}
