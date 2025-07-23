package atm.licenta.cy.Database.Entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "skipped_message_keys", primaryKeys = {"contactUid", "messageIndex"})
public class SkippedMessageKeyEntity {
    @NonNull
    public String contactUid;
    @ColumnInfo(name = "messageIndex")
    public int messageIndex;
    @ColumnInfo(name = "dh_r")
    public byte[] DHr;
    @ColumnInfo(name = "message_key")
    public byte[] messageKey;

    public SkippedMessageKeyEntity(@NonNull String contactUid, int messageIndex, byte[] DHr, byte[] messageKey) {
        this.contactUid = contactUid;
        this.messageIndex = messageIndex;
        this.DHr = DHr;
        this.messageKey = messageKey;
    }
}
