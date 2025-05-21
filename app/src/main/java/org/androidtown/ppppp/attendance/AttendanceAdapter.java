package org.androidtown.ppppp.attendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final List<String> attendanceDates;

    public AttendanceAdapter(List<String> attendanceDates) {
        this.attendanceDates = attendanceDates;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.dateText.setText(attendanceDates.get(position));
    }

    @Override
    public int getItemCount() {
        return attendanceDates.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(android.R.id.text1);
        }
    }
}
