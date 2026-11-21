package com.sitech.easytracker.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sitech.easytracker.R;
import com.sitech.easytracker.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends ArrayAdapter<Product> {
    public ProductAdapter(Context context) {
        super(context, 0, new ArrayList<Product>());
    }

    public void setItems(List<Product> items) {
        clear();
        addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row = convertView != null ? convertView
                : LayoutInflater.from(getContext()).inflate(R.layout.item_product, parent, false);
        Product p = getItem(position);
        if (p != null) UiHelper.bindProduct(getContext(), row, p);
        return row;
    }
}
