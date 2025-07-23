package atm.licenta.cy.Database.Entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class ConversationEntity {
    @PrimaryKey
    @NonNull
    public String uid;
    public String name;
    public String lastMessage;
    public String timestamp;
    public ConversationEntity(@NonNull String uid, String name, String lastMessage, String timestamp) {
        this.uid = uid;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }
}
