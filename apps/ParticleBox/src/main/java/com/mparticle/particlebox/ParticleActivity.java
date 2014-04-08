package com.mparticle.particlebox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;

import com.mparticle.MParticle;


public abstract class ParticleActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MParticle.start(this);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

    }

    @Override
    protected void onStart() {
        super.onStart();
        MParticle.getInstance().activityStarted(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
        MParticle.getInstance().activityStopped(this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment fragment = null;
        switch (position) {
            case 0:
                mTitle = getString(R.string.title_section1);
                fragment = MainEventTestFragment.newInstance(position + 1);
                break;
            case 1:
                mTitle = getString(R.string.title_section2);
                fragment = MainAttributeTestFragment.newInstance(position + 1);
                break;
            case 2:
                mTitle = getString(R.string.title_section3);
                fragment = MainTransactionTestFragment.newInstance(position + 1);
                break;
            case 3:
                mTitle = getString(R.string.title_section4);
                fragment = WebAppFragment.newInstance(position + 1);
                break;
            case 4:
                mTitle = getString(R.string.title_section5);
                fragment = NetworkPerformanceFragment.newInstance(position + 1);
                break;
            case 5:
                mTitle = getString(R.string.title_section6);
                fragment = AudienceFragment.newInstance(position + 1);
                break;
        }
        if (fragmentManager.findFragmentByTag(mTitle.toString()) == null) {
            transaction.replace(R.id.container, fragment, mTitle.toString())
                    .commit();
        }
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
            case 4:
                mTitle = getString(R.string.title_section4);
                break;
            case 5:
                mTitle = getString(R.string.title_section5);
                break;
            case 6:
                mTitle = getString(R.string.title_section6);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
       // actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

}