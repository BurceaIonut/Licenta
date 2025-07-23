package atm.licenta.cy.Database.Entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dh_ratchet_state")
public class DHRatchetStateEntity {
    @PrimaryKey
    @NonNull
    public String contactUid;
    @ColumnInfo(name = "dh_s_private")
    public byte[] DHsPrivateKey;
    @ColumnInfo(name = "dh_s_public")
    public byte[] DHsPublicKey;
    @ColumnInfo(name = "dh_r_public")
    public byte[] DHrPublicKey;
    @ColumnInfo(name = "ns")
    public int Ns;
    @ColumnInfo(name = "nr")
    public int Nr;
    @ColumnInfo(name = "pn")
    public int PN;
    @ColumnInfo(name = "last_dhr_used_for_ratchet")
    public byte[] lastDHrUsedForSendingRatchet;

    public DHRatchetStateEntity(@NonNull String contactUid, byte[] DHsPrivateKey, byte[] DHsPublicKey, byte[] DHrPublicKey, int Ns, int Nr, int PN) {
        this.contactUid = contactUid;
        this.DHsPrivateKey = DHsPrivateKey;
        this.DHsPublicKey = DHsPublicKey;
        this.DHrPublicKey = DHrPublicKey;
        this.Ns = Ns;
        this.Nr = Nr;
        this.PN = PN;
        this.lastDHrUsedForSendingRatchet = null;
    }
}
