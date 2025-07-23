package atm.licenta.cy.Activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.appcompat.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import atm.licenta.cy.Fragments.CallsFragment;
import atm.licenta.cy.Fragments.ChatsFragment;
import atm.licenta.cy.R;
import atm.licenta.cy.Services.ForegroundWebSocketService;

public class ConversationsActivity extends AppCompatActivity {
    private ImageView searchIcon;
    private SearchView searchView;
    private BottomNavigationView bottomNavigation;
    private Fragment chatsFrag;
    private Fragment callsFrag;
    private ImageButton settings_button;
    private final BroadcastReceiver globalMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("CHATS");
            if (fragment instanceof ChatsFragment) {
                ((ChatsFragment) fragment).forceReload();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(globalMessageReceiver, new IntentFilter("INCOMING_CHAT_MESSAGE"));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(globalMessageReceiver);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        String raw = prefs.getString("UID", null);
        if (raw != null) {
            new Thread(() -> {
                try {
                    Intent serviceIntent = new Intent(this, ForegroundWebSocketService.class);
                    serviceIntent.putExtra("uid", raw);
                    ContextCompat.startForegroundService(this, serviceIntent);
                } catch (Exception e) {
                    Log.e("ACTIVITY", "Error at parsing UID", e);
                }
            }).start();
        } else {
            Log.e("ACTIVITY", "No UID found in SharedPreferences");
        }
    }

    @SuppressLint({"NotifyDataSetChanged", "NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.conversations_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.conversations), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            bottomNavigation.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            return insets;
        });

        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(ConversationsActivity.this, v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.settings_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.menu_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                } else if (id == R.id.menu_scan_qr) {
                    startActivity(new Intent(this, ScanQRActivity.class));
                    return true;
                } else if (id == R.id.menu_show_qr) {
                    startActivity(new Intent(this, ShowQRActivity.class));
                    return true;
                }

                return false;
            });

            popup.show();
        });

        searchIcon = findViewById(R.id.search_icon);
        searchView = findViewById(R.id.searchView);
        bottomNavigation = findViewById(R.id.navigation);
        chatsFrag = new ChatsFragment();
        callsFrag = new CallsFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, callsFrag, "CALLS").hide(callsFrag)
                .add(R.id.fragment_container, chatsFrag, "CHATS")
                .commit();

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_conversations) {
                getSupportFragmentManager().beginTransaction()
                        .hide(callsFrag)
                        .show(chatsFrag)
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .hide(chatsFrag)
                        .show(callsFrag)
                        .commit();
            }
            return true;
        });

        ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);

        closeButton.setOnClickListener(v -> {
            if (!searchView.getQuery().toString().isEmpty()) {
                searchView.setQuery("", false);
            } else {
                hideSearchView();
            }
        });

        searchIcon.setOnClickListener(v -> {
            searchIcon.setVisibility(View.GONE);
            searchView.setVisibility(View.VISIBLE);
            searchView.setIconified(false);

            EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            searchEditText.requestFocus();

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        searchView.setOnCloseListener(() -> {
            hideSearchView();
            return true;
        });

        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideSearchView();
        });

        FloatingActionButton fab = findViewById(R.id.fab_new_chat);

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, ContactsActivity.class);
            startActivity(intent);
        });
    }

    private void hideSearchView() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView.clearFocus();
        searchView.setQuery("", false);
        searchView.setVisibility(View.GONE);
        searchIcon.setVisibility(View.VISIBLE);
    }
}