package atm.licenta.cy.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import atm.licenta.cy.Adapters.CallAdapter;
import atm.licenta.cy.Models.CallItemModel;
import atm.licenta.cy.R;

public class CallsFragment extends Fragment {
    private List<CallItemModel> calls;
    private CallAdapter adapter;
    public CallsFragment(){}
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversations, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.conversations_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(null);

        calls = new ArrayList<>();
        adapter = new CallAdapter(calls);
        recyclerView.setAdapter(adapter);

        adapter.updateData(calls);

        SearchView searchView = requireActivity().findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        return view;
    }
}
