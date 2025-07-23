package atm.licenta.cy.Adapters;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.List;

import atm.licenta.cy.Models.AttachmentMessageModel;
import atm.licenta.cy.Models.ChatMessageModel;
import atm.licenta.cy.R;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_DATE = 3;
    private static final int VIEW_TYPE_VOICE_SENT = 4;
    private static final int VIEW_TYPE_VOICE_RECEIVED = 5;
    private static final int VIEW_TYPE_ATTACHMENT_SENT = 6;
    private static final int VIEW_TYPE_ATTACHMENT_RECEIVED = 7;
    private List<Object> messageList;

    public ChatMessagesAdapter(List<Object> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = messageList.get(position);
        if (item instanceof ChatMessageModel) {
            ChatMessageModel message = (ChatMessageModel) item;
            if (message instanceof atm.licenta.cy.Models.VoiceMessageModel) {
                return message.isSentByMe() ? VIEW_TYPE_VOICE_SENT : VIEW_TYPE_VOICE_RECEIVED;
            }else if (message instanceof atm.licenta.cy.Models.AttachmentMessageModel) {
                return message.isSentByMe() ? VIEW_TYPE_ATTACHMENT_SENT : VIEW_TYPE_ATTACHMENT_RECEIVED;
            } else {
                return message.isSentByMe() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
            }
        } else {
            return VIEW_TYPE_DATE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_SENT:
                return new SentMessageViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
            case VIEW_TYPE_RECEIVED:
                return new ReceivedMessageViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
            case VIEW_TYPE_VOICE_SENT:
                return new VoiceMessageViewHolder(inflater.inflate(R.layout.item_voice_sent, parent, false));
            case VIEW_TYPE_VOICE_RECEIVED:
                return new VoiceMessageViewHolder(inflater.inflate(R.layout.item_voice_received, parent, false));
            case VIEW_TYPE_ATTACHMENT_SENT:
                return new AttachmentViewHolder(inflater.inflate(R.layout.item_attachment_sent, parent, false));
            case VIEW_TYPE_ATTACHMENT_RECEIVED:
                return new AttachmentViewHolder(inflater.inflate(R.layout.item_attachment_received, parent, false));
            default:
                return new DateViewHolder(inflater.inflate(R.layout.item_chat_date, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Object item = messageList.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind((ChatMessageModel) item);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind((ChatMessageModel) item);
        } else if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).bind((String) item);
        } else if (holder instanceof VoiceMessageViewHolder) {
            ((VoiceMessageViewHolder) holder).bind((atm.licenta.cy.Models.VoiceMessageModel) item);
        } else if (holder instanceof AttachmentViewHolder) {
            ((AttachmentViewHolder) holder).bind((AttachmentMessageModel) item);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageBody, messageTime;
        ImageView iconSent, iconDelivered, iconRedirected;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.text_message_body);
            messageTime = itemView.findViewById(R.id.text_message_timestamp);
            iconSent = itemView.findViewById(R.id.icon_sent);
            iconDelivered = itemView.findViewById(R.id.icon_delivered);
            iconRedirected = itemView.findViewById(R.id.icon_redirected);
        }

        void bind(ChatMessageModel message) {
            messageBody.setText(message.getMessage_in_plaintext());
            messageTime.setText(message.getTimestamp().substring(0, 5));

            iconSent.setVisibility(View.GONE);
            iconDelivered.setVisibility(View.GONE);
            iconRedirected.setVisibility(View.GONE);

            if (message.isRedirected()) {
                iconRedirected.setVisibility(View.VISIBLE);
                iconRedirected.setColorFilter(Color.parseColor("#4CAF50"));
            } else if (message.isDelivered()) {
                iconDelivered.setVisibility(View.VISIBLE);
                iconDelivered.setColorFilter(Color.parseColor("#2196F3"));
            } else if (message.isSent()) {
                iconSent.setVisibility(View.VISIBLE);
                iconSent.setColorFilter(Color.parseColor("#CCCCCC"));
            }
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageBody, messageTime, senderName;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.text_message_sender);
            messageBody = itemView.findViewById(R.id.text_message_body);
            messageTime = itemView.findViewById(R.id.text_message_time);
        }

        void bind(ChatMessageModel message) {
            messageBody.setText(message.getMessage_in_plaintext());
            messageTime.setText(message.getTimeFromTimestamp());
            if (message.isGroupMessage()) {
                senderName.setVisibility(View.VISIBLE);
                senderName.setText(message.getFrom());
            } else {
                senderName.setVisibility(View.GONE);
            }
        }
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView dateSeparator;

        DateViewHolder(View itemView) {
            super(itemView);
            dateSeparator = itemView.findViewById(R.id.chat_date_separator);
        }

        void bind(String dateText) {
            dateSeparator.setText(dateText);
        }
    }

    static class VoiceMessageViewHolder extends RecyclerView.ViewHolder {
        TextView durationText;
        ImageView playButton;

        VoiceMessageViewHolder(View itemView) {
            super(itemView);
            durationText = itemView.findViewById(R.id.voice_duration);
            playButton = itemView.findViewById(R.id.play_button);
        }

        void bind(atm.licenta.cy.Models.VoiceMessageModel message) {
            durationText.setText(message.getDurationSeconds() + " sec");
            playButton.setOnClickListener(v -> {
                MediaPlayer player = new MediaPlayer();
                try {
                    player.setDataSource(message.getAudioPath());
                    player.setOnPreparedListener(MediaPlayer::start);
                    player.setOnCompletionListener(mp -> {
                        mp.release();
                        Log.d("VoicePlayback", "Play done");
                    });
                    player.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(playButton.getContext(), "Eroare la redare", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    static class AttachmentViewHolder extends RecyclerView.ViewHolder {
        ImageView preview, downloadIcon;
        TextView filename, fileInfo, timeText;

        AttachmentViewHolder(View itemView) {
            super(itemView);
            preview = itemView.findViewById(R.id.file_icon);
            downloadIcon = itemView.findViewById(R.id.download_icon);
            filename = itemView.findViewById(R.id.file_name);
            fileInfo = itemView.findViewById(R.id.file_details);
            timeText = itemView.findViewById(R.id.message_timestamp);
        }

        void bind(AttachmentMessageModel message) {
            filename.setText(message.getFileName());
            fileInfo.setText(message.getFileSize() + " • " + message.getFileExtension());
            timeText.setText(message.getTimestamp().substring(11));

            File file = new File(message.getFilePath());
            if (message.getFileType().equalsIgnoreCase("image")) {
                preview.setImageURI(null);
                preview.setImageURI(Uri.fromFile(file));
            } else {
                preview.setImageResource(R.drawable.file_extension_placeholder);
            }

            downloadIcon.setOnClickListener(v ->
                    Toast.makeText(itemView.getContext(), "Downloading...", Toast.LENGTH_SHORT).show()
            );

            Log.e("DEBUG_ATTACH", "filename=" + message.getFileName() + ", size=" + message.getFileSize() + ", ext=" + message.getFileExtension());
            Log.e("DEBUG_ATTACH", "fileNameView=" + (filename != null ? "OK" : "NULL"));
            Log.e("DEBUG_ATTACH", "fileInfoView=" + (fileInfo != null ? "OK" : "NULL"));
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Object> newList) {
        this.messageList = newList;
        notifyDataSetChanged();
    }
}
