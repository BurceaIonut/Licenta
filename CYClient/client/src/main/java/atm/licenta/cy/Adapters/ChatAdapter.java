package atm.licenta.cy.Adapters;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import atm.licenta.cy.Models.MessageItemModel;
import atm.licenta.cy.R;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private String currentSearchQuery = "";
    private final List<MessageItemModel> originalList;
    private final List<MessageItemModel> filteredList;

    public ChatAdapter(List<MessageItemModel> itemList) {
        this.originalList = new ArrayList<>(itemList);
        this.filteredList = new ArrayList<>(itemList);
    }

    public interface OnItemClickListener {
        void onItemClick(MessageItemModel message);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(MessageItemModel message);
    }

    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.conversation_item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatAdapter.ViewHolder holder, int position) {
        MessageItemModel item = filteredList.get(position);
        holder.chat_name.setText(item.getFrom());
        holder.chat_preview.setText(highlightText(item.getLastMessage(), currentSearchQuery));
        holder.chat_time.setText(item.getTimestamp());
        int count = item.getUnreadCount();
        if (count > 0) {
            holder.unreadCountView.setVisibility(View.VISIBLE);
            holder.unreadCountView.setText(String.valueOf(count));
            holder.chat_time.setTextColor(Color.parseColor("#4CAF50"));
            holder.unreadCountView.setTextColor(Color.WHITE);
        } else {
            holder.unreadCountView.setVisibility(View.GONE);
            holder.chat_time.setTextColor(Color.BLACK);
        }
        holder.chat_image.setImageResource(item.getImageResId());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<MessageItemModel> newData) {
        originalList.clear();
        originalList.addAll(newData);

        filteredList.clear();
        filteredList.addAll(newData);

        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String query) {
        currentSearchQuery = query == null ? "" : query.trim();
        filteredList.clear();

        if (query == null) query = "";
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);

        if (lowerQuery.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            for (MessageItemModel item : originalList) {
                String name = item.getFrom().toLowerCase(Locale.ROOT);
                String lastMsg = item.getLastMessage().toLowerCase(Locale.ROOT);

                if (name.contains(lowerQuery) || lastMsg.contains(lowerQuery)) {
                    filteredList.add(item);
                }
            }
        }

        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView chat_name, chat_preview, chat_time;
        ImageView chat_image;
        TextView unreadCountView;

        public ViewHolder(View itemView) {
            super(itemView);
            chat_name = itemView.findViewById(R.id.chat_name);
            chat_preview = itemView.findViewById(R.id.chat_preview);
            chat_time = itemView.findViewById(R.id.chat_time);
            chat_image = itemView.findViewById(R.id.chat_image);
            unreadCountView = itemView.findViewById(R.id.unread_count);
        }
    }

    public static Spannable highlightText(String fullText, String query) {
        SpannableString spannable = new SpannableString(fullText);

        if (query == null || query.isEmpty()) return spannable;

        String lowerText = fullText.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        int index = lowerText.indexOf(lowerQuery);
        while (index >= 0) {
            int wordStart = index;
            while (wordStart > 0 && Character.isLetterOrDigit(lowerText.charAt(wordStart - 1))) {
                wordStart--;
            }

            int wordEnd = index + lowerQuery.length();
            while (wordEnd < lowerText.length() && Character.isLetterOrDigit(lowerText.charAt(wordEnd))) {
                wordEnd++;
            }

            spannable.setSpan(
                    new ForegroundColorSpan(Color.BLUE),
                    wordStart, wordEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            index = lowerText.indexOf(lowerQuery, wordEnd);
        }

        return spannable;
    }
}
