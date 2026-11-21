package com.sitech.easytracker.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sitech.easytracker.R;
import com.sitech.easytracker.model.StockTransaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends ArrayAdapter<StockTransaction> {
    public TransactionAdapter(Context context) {
        super(context, 0, new ArrayList<StockTransaction>());
    }

    public void setItems(List<StockTransaction> items) {
        clear();
        addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row = convertView != null ? convertView
                : LayoutInflater.from(getContext()).inflate(R.layout.item_transaction, parent, false);
        StockTransaction t = getItem(position);
        if (t != null) UiHelper.bindTransaction(getContext(), row, t);
        return row;
    }
}
