package atm.licenta.cy.Database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import atm.licenta.cy.Database.DAO.ContactDao;
import atm.licenta.cy.Database.DAO.ConversationDao;
import atm.licenta.cy.Database.DAO.DHRatchetStateDao;
import atm.licenta.cy.Database.DAO.KeyStateDao;
import atm.licenta.cy.Database.DAO.MessageDao;
import atm.licenta.cy.Database.DAO.ProfileDao;
import atm.licenta.cy.Database.DAO.SkippedMessageKeyDao;
import atm.licenta.cy.Database.Entities.ContactEntity;
import atm.licenta.cy.Database.Entities.ConversationEntity;
import atm.licenta.cy.Database.Entities.DHRatchetStateEntity;
import atm.licenta.cy.Database.Entities.KeyStateEntity;
import atm.licenta.cy.Database.Entities.MessageEntity;
import atm.licenta.cy.Database.Entities.ProfileEntity;
import atm.licenta.cy.Database.Entities.SkippedMessageKeyEntity;

@Database(entities = {ContactEntity.class, ConversationEntity.class, MessageEntity.class, ProfileEntity.class,
        KeyStateEntity.class, DHRatchetStateEntity.class, SkippedMessageKeyEntity.class}, version = 14)
public abstract class CYDB extends RoomDatabase {
    public abstract ContactDao contactDao();
    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();
    public abstract ProfileDao profileDao();
    public abstract KeyStateDao keyStateDao();
    public abstract DHRatchetStateDao dhRatchetStateDao();
    public abstract SkippedMessageKeyDao skippedMessageKeyDao();
}
