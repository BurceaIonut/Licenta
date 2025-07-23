package atm.licenta.cy.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import atm.licenta.cy.Models.CallItemModel;
import atm.licenta.cy.R;

public class CallAdapter extends RecyclerView.Adapter<CallAdapter.ViewHolder>{
    private final List<CallItemModel> originalList;
    private final List<CallItemModel> filteredList;

    public CallAdapter(List<CallItemModel> itemList) {
        this.originalList = new ArrayList<>(itemList);
        this.filteredList = new ArrayList<>(itemList);
    }

    @NonNull
    @Override
    public CallAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.call_item_list, parent, false);
        return new CallAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CallAdapter.ViewHolder holder, int position) {
        CallItemModel item = filteredList.get(position);
        Context context = holder.itemView.getContext();

        holder.called_name.setText(item.getFrom());
        holder.call_time.setText(item.getTimestamp());

        if (item.isMissed()) {
            holder.statusImageView.setImageResource(R.drawable.call_received);
            holder.statusImageView.setColorFilter(ContextCompat.getColor(context, R.color.call_red));
        } else {
            holder.statusImageView.setImageResource(R.drawable.call_made);
            holder.statusImageView.setColorFilter(ContextCompat.getColor(context, R.color.call_green));
        }

        if (item.isWasVideo()) {
            holder.recallImageView.setImageResource(R.drawable.videocam);
        } else {
            holder.recallImageView.setImageResource(R.drawable.phone);
        }
    }


    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<CallItemModel> newData) {
        originalList.clear();
        originalList.addAll(newData);

        filteredList.clear();
        filteredList.addAll(newData);

        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String query) {
        //TODO
        filteredList.clear();

        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView called_name, call_time;
        ImageView statusImageView, recallImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            called_name = itemView.findViewById(R.id.called_name);
            call_time = itemView.findViewById(R.id.call_time);
            statusImageView = itemView.findViewById(R.id.statusImageView);
            recallImageView = itemView.findViewById(R.id.recallImageView);
        }
    }
}
