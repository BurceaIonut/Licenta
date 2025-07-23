package atm.licenta.cy.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import atm.licenta.cy.Activities.ChatActivity;
import atm.licenta.cy.Database.DBClient;
import atm.licenta.cy.Models.ContactModel;
import atm.licenta.cy.R;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder>{
    private final List<ContactModel> contactList;
    private final OnContactClickListener listener;
    public interface OnContactClickListener {
        void onContactClick(ContactModel contact);
    }
    public ContactAdapter(List<ContactModel> contactList, OnContactClickListener listener) {
        this.contactList = contactList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactModel contact = contactList.get(position);
        holder.name.setText(contact.getName());
        holder.status.setText(contact.getStatus());
        holder.image.setImageResource(R.drawable.profile_picture_placeholder);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatActivity.class);
            intent.putExtra("contact_uid", contact.getUid());
            intent.putExtra("contact_name", contact.getName());
            v.getContext().startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            Context context = v.getContext();
            new AlertDialog.Builder(context)
                    .setTitle("Delete Contact")
                    .setMessage("Are you sure you want to delete this contact?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        new Thread(() -> {
                            DBClient dbClient = DBClient.getInstance(context);

                            String uid = contact.getUid();
                            dbClient.getAppDatabase().contactDao().deleteByUid(uid);
                            dbClient.getAppDatabase().conversationDao().deleteConversationByUid(uid);
                            dbClient.getAppDatabase().messageDao().deleteMessagesForConversation(uid);
                            dbClient.getAppDatabase().keyStateDao().deleteKeyStateForContact(uid);
                            dbClient.getAppDatabase().dhRatchetStateDao().deleteStateForContact(uid);
                            dbClient.getAppDatabase().skippedMessageKeyDao().deleteAllKeysForUID(uid);

                            ((android.app.Activity) context).runOnUiThread(() -> {
                                contactList.remove(position);
                                notifyItemRemoved(position);
                                Toast.makeText(context, "Contact and conversation deleted", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView name, status;
        ImageView image;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contact_name);
            status = itemView.findViewById(R.id.contact_status);
            image = itemView.findViewById(R.id.contact_image);
        }
    }
}
