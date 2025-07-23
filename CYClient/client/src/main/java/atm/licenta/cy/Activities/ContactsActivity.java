package atm.licenta.cy.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import atm.licenta.cy.Adapters.ContactAdapter;
import atm.licenta.cy.Database.Entities.ContactEntity;
import atm.licenta.cy.Helpers.IntentKeys;
import atm.licenta.cy.Models.ContactModel;
import atm.licenta.cy.R;

public class ContactsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private List<ContactModel> contactList;
    private LinearLayout newGroupButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contacts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.contacts_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(R.string.choose_from_your_contacts);
            getSupportActionBar().setSubtitle("0 contacts"); //TODO

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        newGroupButton = findViewById(R.id.newGroupButton);

        newGroupButton.setOnClickListener(v -> {

        });

        contactList = new ArrayList<>();

        new Thread(() -> {
            List<ContactEntity> entities = atm.licenta.cy.Database
                    .DBClient.getInstance(getApplicationContext())
                    .getAppDatabase()
                    .contactDao()
                    .getAllContacts();

            contactList.clear();
            for (ContactEntity entity : entities) {
                contactList.add(new ContactModel(
                        entity.uid,
                        entity.firstName + " " + entity.lastName,
                        entity.status
                ));
            }

            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(contactList.size() + " contacts");
                }
            });
        }).start();

        adapter = new ContactAdapter(contactList, contact -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(IntentKeys.CONTACT_UID, contact.getUid());
            intent.putExtra(IntentKeys.CONTACT_NAME, contact.getName());
            Log.e("ContactsActivity", "Name: " + contact.getName());
            startActivity(intent);
            finish();
        });

        recyclerView = findViewById(R.id.contactsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
}