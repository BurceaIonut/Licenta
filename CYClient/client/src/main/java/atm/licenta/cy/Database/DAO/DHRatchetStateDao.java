package atm.licenta.cy.Database.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import atm.licenta.cy.Database.Entities.DHRatchetStateEntity;

@Dao
public interface DHRatchetStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DHRatchetStateEntity state);
    @Query("SELECT * FROM dh_ratchet_state WHERE contactUid = :uid")
    DHRatchetStateEntity getState(String uid);
    @Query("DELETE FROM dh_ratchet_state WHERE contactUid = :uid")
    void deleteStateForContact(String uid);
}
