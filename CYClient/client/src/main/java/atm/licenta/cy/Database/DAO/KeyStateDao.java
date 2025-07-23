package atm.licenta.cy.Database.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import atm.licenta.cy.Database.Entities.KeyStateEntity;

@Dao
public interface KeyStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateKeyState(KeyStateEntity keyState);
    @Query("SELECT * FROM key_state WHERE contact_uid = :uid LIMIT 1")
    KeyStateEntity getKeyStateForContact(String uid);
    @Query("DELETE FROM key_state WHERE contact_uid = :uid")
    void deleteKeyStateForContact(String uid);
}

