package atm.licenta.cy.Database.Entities;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class ContactEntity {
    @PrimaryKey(autoGenerate = false)
    @NonNull
    public String uid;
    public String firstName;
    public String lastName;
    public String status;
    public String identityPublicKey;
    public boolean pending;
    public ContactEntity() {}

    public ContactEntity(@NonNull String uid, String firstName, String lastName, String status, String identityPublicKey) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.identityPublicKey = identityPublicKey;
        this.pending = true;
    }
}
