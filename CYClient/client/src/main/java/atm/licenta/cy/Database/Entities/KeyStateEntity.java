package atm.licenta.cy.Database.Entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "key_state")
public class KeyStateEntity {
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "contact_uid")
    @NonNull
    public String contactUid;
    @ColumnInfo(name = "root_key", typeAffinity = ColumnInfo.BLOB)
    public byte[] rootKey;
    @ColumnInfo(name = "sending_chain_key", typeAffinity = ColumnInfo.BLOB)
    public byte[] sendingChainKey;
    @ColumnInfo(name = "receiving_chain_key", typeAffinity = ColumnInfo.BLOB)
    public byte[] receivingChainKey;
    public KeyStateEntity(String contactUid, byte[] rootKey, byte[] sendingChainKey, byte[] receivingChainKey) {
        this.contactUid = contactUid;
        this.rootKey = rootKey;
        this.sendingChainKey = sendingChainKey;
        this.receivingChainKey = receivingChainKey;
    }
}