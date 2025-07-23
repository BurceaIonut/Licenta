package atm.licenta.cy.Database.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages", indices = {@Index("conversationUid")})
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "conversationUid")
    public String conversationUid;
    @ColumnInfo(name = "fromMe")
    public boolean fromMe;
    @ColumnInfo(name = "message")
    public String message;
    @ColumnInfo(name = "timestamp")
    public String timestamp;
    @ColumnInfo(name = "messageIndex")
    public int messageIndex;


    public MessageEntity(){}
    public MessageEntity(String conversationUid, boolean fromMe, String message, String timestamp, int index) {
        this.conversationUid = conversationUid;
        this.fromMe = fromMe;
        this.message = message;
        this.timestamp = timestamp;
        this.messageIndex = index;
    }

}