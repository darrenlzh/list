package com.example.darrenlim.list;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private android.support.v7.widget.Toolbar _toolbar;
    private DrawerLayout _drawerLayout;
    private CollapsingToolbarLayout _collapsingToolbarLayout;

    private RecyclerView _recyclerView;
    private ReminderAdapter _rAdapter;

    private ArrayList<Reminder> _data = new ArrayList<>();
    protected static Calendar _calendar = Calendar.getInstance();
    String _dayOfWeek, _dayOfMonth;

    private Boolean _isFabOpen = false;
    private FloatingActionButton _fab, _fab1, _fab2;
    private Animation _fabOpen, _fabClose, _rotateForward, _rotateBackward, _fabGrow;

    private AlertDialog _dialog;
    private View _dialogView;
    static public ParseUser _currentUser;
    static private boolean _reset = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE");
        _dayOfWeek = dayFormat.format(_calendar.getTime());
        _dayOfMonth = String.valueOf(_calendar.get(Calendar.DAY_OF_MONTH));
        setContentView(R.layout.activity_main);
        setUpToolbar();
        setupCollapsingToolbarLayout();
        setUpNavDrawer();

        // Enable Local Datastore.
        if(_reset) {
            Parse.enableLocalDatastore(this);
            Parse.initialize(this, "0BC99FjSMdD9UhB5ipsBEey5iSx85hSgb1zRK7l5", "gkZPUEo70rXQCKyjscI0Q4FDJvRHERzY78Kr8fiS");
            _reset = false;
        }

        _recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        _recyclerView.setHasFixedSize(false);

        // First param is number of columns and second param is orientation i.e Vertical or Horizontal
        StaggeredGridLayoutManager gridLayoutManager =
                new StaggeredGridLayoutManager(2, 1);
        // Attach the layout manager to the recycler view
        _recyclerView.setLayoutManager(gridLayoutManager);
        _recyclerView.setItemAnimator(new DefaultItemAnimator());
        SpacesItemDecoration decoration = new SpacesItemDecoration(25);
        _recyclerView.addItemDecoration(decoration);
        final int fabMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        _recyclerView.addOnScrollListener(new MyRecyclerScroll() {
            @Override
            public void show() {
                _fab.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
            }

            @Override
            public void hide() {
                _fab.animate().translationY(_fab.getHeight() + fabMargin).setInterpolator(new AccelerateInterpolator(2)).start();
            }
        });

        _rAdapter = new ReminderAdapter(MainActivity.this, _data);
//        _recyclerView.setAdapter(_rAdapter);

        _fab = (FloatingActionButton) findViewById(R.id.fab);
        _fab1 = (FloatingActionButton) findViewById(R.id.fab1);
        _fab2 = (FloatingActionButton) findViewById(R.id.fab2);
        _fabGrow = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_grow);
        _fabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        _fabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        _rotateForward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward);
        _rotateBackward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_backward);
//        _fabOpenMore = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open_more);
        _fab.startAnimation(_fabGrow);
        _fab.setOnClickListener(this);
        _fab1.setOnClickListener(this);
        _fab2.setOnClickListener(this);

        _currentUser = ParseUser.getCurrentUser();
        if(_currentUser!= null) {
            getData();
        }
        else {
            LayoutInflater inflater = getLayoutInflater();
            View view;
            view = inflater.inflate(R.layout.log_layout, null);
            AlertDialog.Builder d = new AlertDialog.Builder(this);
            _dialog = d.create();
            _dialogView = view;
            _dialog.setView(view);
            _dialog.show();
        }

        SwipeableRecyclerViewTouchListener swipeTouchListener =
                new SwipeableRecyclerViewTouchListener(_recyclerView,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return true;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    deleteItemFromCloud(position);
                                    _data.remove(position);
                                    _rAdapter.notifyItemRemoved(position);
                                }
//                                _rAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                System.out.println(_data.size());
                                for (int position : reverseSortedPositions) {
                                    deleteItemFromCloud(position);
                                    _data.remove(position);
                                    _rAdapter.notifyItemRemoved(position);
                                }
//                                _rAdapter.notifyDataSetChanged();
                            }
                        });

        _recyclerView.addOnItemTouchListener(swipeTouchListener);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.fab:
                animateFAB();
                break;
            case R.id.fab1:
//                animateFAB1();
                animateFAB();
                startActivityForResult(new Intent(getApplicationContext(), TaskMenu.class), 1);
                break;
            case R.id.fab2:
//                animateFAB2();
                animateFAB();
                break;
        }
    }

    public void animateFAB() {
        if (_isFabOpen) {
            _fab.startAnimation(_rotateBackward);
            _fab1.startAnimation(_fabClose);
            _fab2.startAnimation(_fabClose);
            _fab1.setClickable(false);
            _fab2.setClickable(false);
            _isFabOpen = false;
        }
        else {
            _fab.startAnimation(_rotateForward);
            _fab1.startAnimation(_fabOpen);
            _fab2.startAnimation(_fabOpen);
            _fab1.setClickable(true);
            _fab2.setClickable(true);
            _isFabOpen = true;
        }
    }

    public void getData() {
        _data.clear();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("ReminderObj");
        query.whereEqualTo("user",_currentUser.getUsername());
        query.orderByDescending("updatedAt");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> reminders, ParseException e) {
                if (e == null) {
                    for (ParseObject item : reminders) {
                        String id = item.getObjectId();
                        String title = item.getString("title");
                        String notes = item.getString("notes");
                        String label = item.getString("label");
                        Integer priority = item.getInt("priority");
                        Boolean remindOnDay = item.getBoolean("remindOnDay");
                        Boolean remindAtLoc = item.getBoolean("remindAtLoc");
                        Integer date = item.getInt("date");
                        Integer time = item.getInt("time");
                        Reminder reminder = new Reminder(id, title, notes, label, priority, remindOnDay, remindAtLoc, date, time);
                        _data.add(reminder);
                        _rAdapter.updateAdapter(_data);
//                        _rAdapter.notifyDataSetChanged();
                        _recyclerView.setAdapter(_rAdapter);

//                        System.out.println(_data.size());
                    }
                }

            }
        });

    }

    public void deleteItemFromCloud(int position) {
        String id = _data.get(position).getId();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("ReminderObj");
        query.getInBackground(id, new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    object.deleteInBackground();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                getData();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }//onActivityResult


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // NAV DRAWER
    private void setUpToolbar() {
        _toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (_toolbar != null) {
            setSupportActionBar(_toolbar);
            getSupportActionBar().setTitle(_dayOfWeek + " " + _dayOfMonth);
        }
    }

    private void setUpNavDrawer() {
        if (_toolbar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            _toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
            _drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            _toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    _drawerLayout.openDrawer(GravityCompat.START);
                    TextView text = (TextView) findViewById(R.id.user);
                    text.setText(_currentUser.getString("Name"));

                }
            });
        }
    }

    private void setupCollapsingToolbarLayout(){

        _collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        if(_collapsingToolbarLayout != null){
            _collapsingToolbarLayout.setTitle(_toolbar.getTitle());
            _collapsingToolbarLayout.setCollapsedTitleGravity(Gravity.CENTER_VERTICAL);
            //collapsingToolbarLayout.setCollapsedTitleTextColor(0xED1C24);
            //collapsingToolbarLayout.setExpandedTitleColor(0xED1C24);
        }
    }
    public void drawerButton(View v) {
        Button b = (Button)v;
        if (b.getText().equals("LOG OUT")) {
            ParseUser.logOut();
            _currentUser = null;
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }
    public void sign_up(View v){
        String name, username, password;
        EditText et;
        et = (EditText) _dialogView.findViewById(R.id.logName);
        name = et.getText().toString();
        et = (EditText) _dialogView.findViewById(R.id.logUsername);
        username = et.getText().toString();
        et = (EditText) _dialogView.findViewById(R.id.logPassword);
        password = et.getText().toString();
        if(name.equals("")) {
            Toast.makeText(getApplicationContext(), R.string.noNameError, Toast.LENGTH_LONG).show();
            return;
        }
        else if(username.equals("")) {
            Toast.makeText(getApplicationContext(), R.string.noUsernameError, Toast.LENGTH_LONG).show();
            return;
        }
        else if(password.equals("")){
            Toast.makeText(getApplicationContext(), R.string.noPasswordError, Toast.LENGTH_LONG).show();
            return;
        }
        ParseUser user = new ParseUser();
        user.setUsername(username);
        user.setPassword(password);
        user.put("Name", name);
        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Toast.makeText(getApplicationContext(), "Sign Up Successful, Logging In", Toast.LENGTH_LONG).show();
                } else {
                    System.out.println("ERROR");
                }
            }
        });

        SystemClock.sleep(2000);
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                _currentUser = user;
                _dialog.dismiss();
                _dialog = null;
                _dialogView = null;
            }
        });
    }

    public void log_in(View v){
        String username, password;
        EditText et;
        et = (EditText) _dialogView.findViewById(R.id.logUsername);
        username = et.getText().toString();
        et = (EditText) _dialogView.findViewById(R.id.logPassword);
        password = et.getText().toString();
        if(username.equals("")) {
            Toast.makeText(getApplicationContext(), R.string.noUsernameError, Toast.LENGTH_LONG).show();
            return;
        }
        else if(password.equals("")){
            Toast.makeText(getApplicationContext(), R.string.noPasswordError, Toast.LENGTH_LONG).show();
            return;
        }
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if(user != null) {
                    _currentUser = user;
                    _dialog.dismiss();
                    _dialog = null;
                    _dialogView = null;
                }
                else {
                    Toast.makeText(getApplicationContext(), R.string.logInError, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void update(View v){
        EditText et = (EditText) _dialogView.findViewById(R.id.logName);
        et.setVisibility(View.VISIBLE);
        Button b = (Button) _dialogView.findViewById(R.id.logButton);
        b.setVisibility(View.GONE);
        b = (Button) _dialogView.findViewById(R.id.signButton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sign_up(v);
            }
        });
    }
}
