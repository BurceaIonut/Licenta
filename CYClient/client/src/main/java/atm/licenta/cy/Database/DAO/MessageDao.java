package atm.licenta.cy.Database.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import atm.licenta.cy.Database.Entities.MessageEntity;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(MessageEntity message);
    @Query("SELECT * FROM chat_messages WHERE conversationUid = :uid ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    List<MessageEntity> getMessagesByConversation(String uid, int limit, int offset);
    @Query("DELETE FROM chat_messages WHERE conversationUid = :uid")
    void deleteMessagesForConversation(String uid);
}
