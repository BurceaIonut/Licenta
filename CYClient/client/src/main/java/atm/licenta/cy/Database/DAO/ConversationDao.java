package atm.licenta.cy.Database.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import atm.licenta.cy.Database.Entities.ConversationEntity;

@Dao
public interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ConversationEntity conversation);
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    List<ConversationEntity> getAllConversations();
    @Query("DELETE FROM conversations WHERE uid = :uid")
    void deleteConversationByUid(String uid);
    @Query("UPDATE conversations SET lastMessage = :lastMessage, timestamp = :timestamp WHERE uid = :contactUid")
    void updateLastMessage(String contactUid, String lastMessage, String timestamp);
    @Query("SELECT * FROM conversations WHERE uid = :contactUid LIMIT 1")
    ConversationEntity getConversationByUid(String contactUid);
}
