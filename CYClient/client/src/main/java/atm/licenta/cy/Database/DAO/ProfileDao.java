package atm.licenta.cy.Database.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import atm.licenta.cy.Database.Entities.ProfileEntity;

@Dao
public interface ProfileDao {
    @Insert
    void insertProfile(ProfileEntity profile);
    @Query("SELECT * FROM Profiles WHERE uid = :uid LIMIT 1")
    ProfileEntity getProfileByUID(String uid);
    @Query("SELECT * FROM Profiles")
    List<ProfileEntity> getAllProfiles();
    @Query("DELETE FROM Profiles WHERE uid = :uid")
    void deleteProfileByUID(String uid);
}
