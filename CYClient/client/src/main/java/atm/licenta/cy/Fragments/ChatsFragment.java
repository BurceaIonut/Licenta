package atm.licenta.cy.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import atm.licenta.cy.Activities.ChatActivity;
import atm.licenta.cy.Adapters.ChatAdapter;
import atm.licenta.cy.Database.DBClient;
import atm.licenta.cy.Database.Entities.ConversationEntity;
import atm.licenta.cy.Helpers.IntentKeys;
import atm.licenta.cy.Models.MessageItemModel;
import atm.licenta.cy.R;

public class ChatsFragment extends Fragment {
    private List<MessageItemModel> messages;
    private ChatAdapter adapter;
    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadConversations();
        }
    };
    public ChatsFragment(){}

    @Override
    public void onResume() {
        super.onResume();
        reloadConversations();
    }

    public void forceReload() {
        if (isAdded()) {
            reloadConversations();
        }
    }

    private void reloadConversations() {
        new Thread(() -> {
            List<ConversationEntity> conversationEntities = DBClient.getInstance(requireContext())
                    .getAppDatabase()
                    .conversationDao()
                    .getAllConversations();

            List<MessageItemModel> items = new ArrayList<>();
            for (ConversationEntity conv : conversationEntities) {
                items.add(new MessageItemModel(
                        requireContext(),
                        conv.uid,
                        conv.name,
                        conv.lastMessage,
                        conv.timestamp,
                        R.drawable.profile_picture_placeholder
                ));
            }

            items.sort((m1, m2) -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd:MM:yyyy", Locale.getDefault());
                    Date d1 = sdf.parse(m1.getRawTimestamp());
                    Date d2 = sdf.parse(m2.getRawTimestamp());
                    return d2.compareTo(d1);
                } catch (ParseException e) {
                    return 0;
                }
            });

            requireActivity().runOnUiThread(() -> {
                messages.clear();
                messages.addAll(items);
                adapter.updateData(messages);
                adapter.notifyDataSetChanged();
            });

        }).start();
    }


    @SuppressLint("NotifyDataSetChanged")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversations, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.conversations_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        messages = new ArrayList<>();
        adapter = new ChatAdapter(messages);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(message -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra(IntentKeys.CONTACT_UID, message.getUid());
            intent.putExtra(IntentKeys.CONTACT_NAME, message.getFrom());
            startActivity(intent);
            message.setUnreadCount(0);
        });

        adapter.setOnItemLongClickListener(message -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete conversation")
                    .setMessage("Are you sure you want to delete this conversation?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        new Thread(() -> {
                            DBClient dbClient = DBClient.getInstance(requireContext());
                            String uid = message.getUid();

                            dbClient.getAppDatabase().messageDao().deleteMessagesForConversation(uid);
                            dbClient.getAppDatabase().conversationDao().deleteConversationByUid(uid);

                            requireActivity().runOnUiThread(() -> {
                                int index = -1;
                                for (int i = 0; i < messages.size(); i++) {
                                    if (messages.get(i).getUid().equals(uid)) {
                                        index = i;
                                        break;
                                    }
                                }
                                if (index != -1) {
                                    messages.remove(index);
                                    adapter.notifyItemRemoved(index);
                                } else {
                                    adapter.notifyDataSetChanged();
                                }
                                reloadConversations();
                            });
                        }).start();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        SearchView searchView = requireActivity().findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                newMessageReceiver,
                new IntentFilter("INCOMING_CHAT_MESSAGE")
        );

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(newMessageReceiver);
    }
}
