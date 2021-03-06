/*
 * Copyright 2018 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.todo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.SyncConfiguration;
import io.realm.SyncUser;
import io.realm.todo.model.Item;
import io.realm.todo.parser.NSPredicateParser;
import io.realm.todo.ui.ItemsRecyclerAdapter;

public class ItemsActivity extends AppCompatActivity {

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);

        setSupportActionBar(findViewById(R.id.toolbar));

        findViewById(R.id.fab).setOnClickListener(view -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_task, null);
            EditText taskText = dialogView.findViewById(R.id.task);
            new AlertDialog.Builder(ItemsActivity.this)
                    .setTitle("Add a new task")
                    .setMessage("What do you want to do next?")
                    .setView(dialogView)
                    .setPositiveButton("Add", (dialog, which) -> {
                        realm.executeTransactionAsync(realm -> {
                            Item item = new Item();
                            item.setBody(taskText.getText().toString());
                            realm.insert(item);
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
        });

        RealmResults<Item> items = setUpRealm();

        final ItemsRecyclerAdapter itemsRecyclerAdapter = new ItemsRecyclerAdapter(items);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(itemsRecyclerAdapter);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                String id = itemsRecyclerAdapter.getItem(position).getItemId();
                realm.executeTransactionAsync(realm -> {
                    Item item = realm.where(Item.class)
                            .equalTo("itemId", id)
                            .findFirst();
                    if (item != null) {
                        item.deleteFromRealm();
                    }
                });
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private RealmResults<Item> setUpRealm() {
        realm = Realm.getDefaultInstance();
        return realm
                .where(Item.class)
                .sort("timestamp", Sort.DESCENDING)
                .findAllAsync();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.query_one) {
            Log.d("Query Selected", "Query One");
            String predicate = "(body == 'jonah' && isDone != True) OR NOT body == 'zach'";
            NSPredicateParser<Item> parser = new NSPredicateParser<>(realm, predicate, Item.class, null);
            RealmQuery query = parser.parsePredicate();
            try {
                Log.d("results", query.findAll().toString());
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
            }
            return true;
        } else if (item.getItemId() == R.id.query_two) {
            Log.d("Query Selected", "Query Two");
            String predicate = "body == %@ && timestamp > %@";
            ArrayList<Object> variableStack = new ArrayList<>();
            variableStack.add("jonah");
            variableStack.add(new GregorianCalendar(2018, 3, 26, 8, 0).getTime());
            NSPredicateParser<Item> parser = new NSPredicateParser<>(realm, predicate, Item.class, variableStack);
            RealmQuery query = parser.parsePredicate();
            try {
                Log.d("results", query.findAll().toString());
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
            }
            return true;
        } else if (item.getItemId() == R.id.query_three) {
            Log.d("Query Selected", "Query Three");
            String predicate = "isDone == false || body == 'jonah'";
            NSPredicateParser<Item> parser = new NSPredicateParser<>(realm, predicate, Item.class, null);
            RealmQuery query = parser.parsePredicate();
            Log.d("results",query.findAll().toString());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
