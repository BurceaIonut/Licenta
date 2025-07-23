package atm.licenta.cy.Database.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import atm.licenta.cy.Database.Entities.ContactEntity;

@Dao
public interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ContactEntity contact);
    @Query("SELECT * FROM contacts WHERE pending = false")
    List<ContactEntity> getAllContacts();
    @Query("SELECT * FROM contacts WHERE uid = :uid LIMIT 1")
    ContactEntity getContactByUid(String uid);
    @Query("DELETE FROM contacts WHERE uid = :uid")
    void deleteByUid(String uid);
    @Query("UPDATE contacts SET pending = FALSE WHERE uid = :uid")
    void updatePendingStatusForContact(String uid);
}
