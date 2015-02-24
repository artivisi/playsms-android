package com.artivisi.android.playsms.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.artivisi.android.playsms.R;
import com.artivisi.android.playsms.domain.Credit;
import com.artivisi.android.playsms.domain.User;
import com.artivisi.android.playsms.service.AndroidMasterService;
import com.artivisi.android.playsms.service.impl.AndroidMasterServiceImpl;
import com.artivisi.android.playsms.ui.db.PlaySmsDb;
import com.artivisi.android.playsms.ui.fragment.ComposerFragment;
import com.artivisi.android.playsms.ui.fragment.ContactsFragment;
import com.artivisi.android.playsms.ui.fragment.InboxFragment;
import com.artivisi.android.playsms.ui.fragment.SentMessageFragment;
import com.google.gson.Gson;

public class DashboardActivity extends ActionBarActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks,
        ComposerFragment.OnFragmentInteractionListener,
        SentMessageFragment.OnFragmentInteractionListener,
        InboxFragment.OnFragmentInteractionListener{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    public static final String TAG = "DashboardActivity";
    private ContactsFragment contactsFragment = new ContactsFragment();

    public static final String DISPLAY_MESSAGE_ACTION =
            "com.artivisi.android.playsms.DISPLAY_MESSAGE";

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private AndroidMasterService service;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private SentMessageFragment sentMessageFragment = new SentMessageFragment();
    private ComposerFragment composeFragment = new ComposerFragment();
    private InboxFragment inboxFragment = new InboxFragment();
    private User user;
    private String mCredit;
    private ImageButton btnComposeMsg;
    private PlaySmsDb playSmsDb;
    public String msg_destination;

    private PendingIntent pendingIntent;

    public void set_subtitle (String text) {
        getSupportActionBar().setSubtitle(text);
    }
    public String gimme_destination() {
        return msg_destination;
    }

    public void set_destination(String dst) {
        msg_destination = dst;
        return;
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("RECEIVER : ","DELIVERED");
            String poll = intent.getStringExtra("polling");
            if(poll.equals("newInbox")){
                inboxFragment.refreshList();
            } else if (poll.equals("newSent")){
                sentMessageFragment.refreshList();
            } else {
                Log.i("UNKNOWN EXTRAS : ", intent.getStringExtra("polling"));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        registerReceiver(receiver,
                new IntentFilter(DISPLAY_MESSAGE_ACTION));
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        user = getUserCookie(LoginActivity.KEY_USER, User.class);

        playSmsDb = new PlaySmsDb(getApplicationContext());

        service = new AndroidMasterServiceImpl(user);

        mTitle = user.getUsername();
        mCredit = "Checking Credit";

        if(isNetworkAvailable()){
            new GetCredit().execute();
        } else {
            Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
            mCredit = "";
            getSupportActionBar().setSubtitle(mCredit);
        }

        btnComposeMsg = (ImageButton) findViewById(R.id.btn_new_msg);
        btnComposeMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
	                show_compose(null);
            }
        });

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        Intent alarmIntent = new Intent(this, QueryReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
        start();
    }

    public  void show_compose(Context context){
        Log.d(TAG, "show_compose");

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, composeFragment);
        fragmentTransaction.commit();
        fragmentTransaction.disallowAddToBackStack();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.hasExtra("notif_action")){
            if(intent.getStringExtra("notif_action").equals("inbox")){
                onNavigationDrawerItemSelected(0);
            }
        } else {
            return;
        }
    }
    public void showButtonCompose(){
        if (btnComposeMsg!=null) btnComposeMsg.setVisibility(View.VISIBLE);
    }

    public void hideButtonCompose(){
        if (btnComposeMsg!=null) btnComposeMsg.setVisibility(View.GONE);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
//        fragmentManager
//                .beginTransaction()
//                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
//                .disallowAddToBackStack()
//                .commit();

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        switch (position){
            case 0:
		showButtonCompose();
                fragmentTransaction.replace(R.id.container, inboxFragment);
                break;
            case 1:
		showButtonCompose();
                fragmentTransaction.replace(R.id.container, sentMessageFragment);
                break;
            case 2:
                // show address book
                hideButtonCompose();
                fragmentTransaction.replace(R.id.container, contactsFragment);
                break;
            case 3:
		showButtonCompose();
                Intent aboutActivity = new Intent(DashboardActivity.this, AboutActivity.class);
                startActivity(aboutActivity);
                break;
            case 4:
                signout();
                break;
            default:
//                fragmentTransaction.replace(R.id.container, inboxFragment);
                break;
        }
        fragmentTransaction.commit();
        fragmentTransaction.disallowAddToBackStack();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void signout(){
        playSmsDb.truncateInbox();
        playSmsDb.truncateSent();
        SharedPreferences sharedpreferences = getSharedPreferences (LoginActivity.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.clear();
        editor.commit();
        Intent goToLogin = new Intent(this, LoginActivity.class);
        startActivity(goToLogin);
        unregisterReceiver(receiver);
        stop();
        deleteNotif();
        finish();
    }

    private void deleteNotif(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    public void start(){
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = 60 * 1000;
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
    }

    public void stop() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
        actionBar.setSubtitle(mCredit);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.dashboard, menu);
            restoreActionBar();

            return true;
        }
        return super.onCreateOptionsMenu(menu);
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

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);


            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
//            ((DashboardActivity) activity).onSectionAttached(
//                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    public class GetCredit extends AsyncTask<Void, Void, Credit>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            getSupportActionBar().setSubtitle("Checking Credit");
        }

        @Override
        protected Credit doInBackground(Void... params) {
            try {
                return service.getCredit();
            } catch (Exception e) {
                Log.d("CONNECTION ERROR : ", e.getMessage());
                return null;
            }
        }

        @Override
         protected void onPostExecute(Credit credit) {
            super.onPostExecute(credit);
            if (credit == null){
                Toast.makeText(getApplicationContext(), "Connection Timeout", Toast.LENGTH_SHORT).show();
            } else {
                mCredit = credit.getCredit();
                if(credit.getError().equals("0")){
                    getSupportActionBar().setSubtitle(mCredit);
                }
            }
        }
    }

    protected <T> T getUserCookie(String key, Class<T> a) {
        SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS, Context.MODE_PRIVATE);

        if (sharedPreferences == null) {
            return null;
        }

        String data = sharedPreferences.getString(key, null);

        if (data == null) {
            return null;
        } else {
            Gson gson = new Gson();
            return gson.fromJson(data, a);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
