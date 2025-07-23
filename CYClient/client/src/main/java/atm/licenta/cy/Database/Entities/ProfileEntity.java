package atm.licenta.cy.Database.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profiles")
public class ProfileEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "uid")
    public String uid;
    @ColumnInfo(name="first_name")
    public String firstName;
    @ColumnInfo(name="last_name")
    public String lastName;

    public ProfileEntity(String uid, String firstName, String lastName) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
