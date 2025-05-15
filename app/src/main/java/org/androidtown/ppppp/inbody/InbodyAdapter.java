package org.androidtown.ppppp.inbody;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.androidtown.ppppp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class InbodyAdapter extends RecyclerView.Adapter<InbodyAdapter.ViewHolder> {

    private ArrayList<InbodyRecord> inbodyList;

    public InbodyAdapter(ArrayList<InbodyRecord> inbodyList) {
        this.inbodyList = inbodyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inbody, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InbodyRecord record = inbodyList.get(position);

        holder.txtDate.setText(formatDate(record.getTimestamp()));
        holder.txtWeight.setText("체중: " + record.getWeight() + "kg");
        holder.txtMuscle.setText("골격근량: " + record.getMuscleMass() + "kg");
        holder.txtFatMass.setText("체지방량: " + record.getBodyFatMass() + "kg");
        holder.txtFatPercent.setText("체지방률: " + record.getBodyFatPercent() + "%");
        holder.txtBmi.setText("BMI: " + record.getBmi());
    }

    @Override
    public int getItemCount() {
        return inbodyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtWeight, txtMuscle, txtFatMass, txtFatPercent, txtBmi;

        public ViewHolder(View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtMuscle = itemView.findViewById(R.id.txtMuscleMass);
            txtFatMass = itemView.findViewById(R.id.txtBodyFatMass);
            txtFatPercent = itemView.findViewById(R.id.txtBodyFatPercent);
            txtBmi = itemView.findViewById(R.id.txtBmi);
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
