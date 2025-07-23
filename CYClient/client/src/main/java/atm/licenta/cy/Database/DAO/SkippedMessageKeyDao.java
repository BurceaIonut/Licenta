package atm.licenta.cy.Database.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import atm.licenta.cy.Database.Entities.SkippedMessageKeyEntity;

@Dao
public interface SkippedMessageKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SkippedMessageKeyEntity key);
    @Query("SELECT * FROM skipped_message_keys WHERE contactUid = :uid AND messageIndex = :index")
    SkippedMessageKeyEntity getKey(String uid, int index);
    @Query("DELETE FROM skipped_message_keys WHERE contactUid = :uid AND messageIndex = :index")
    void deleteKey(String uid, int index);
    @Query("SELECT * FROM skipped_message_keys WHERE contactUid = :uid")
    List<SkippedMessageKeyEntity> getAllForContact(String uid);
    @Query("DELETE FROM skipped_message_keys WHERE contactUid = :uid")
    void deleteAllKeysForUID(String uid);
}
