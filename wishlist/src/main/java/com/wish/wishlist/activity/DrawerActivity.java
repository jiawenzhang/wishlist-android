package com.wish.wishlist.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.ParseUser;
import com.squareup.otto.Subscribe;
import com.wish.wishlist.R;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.ProfileChangeEvent;
import com.wish.wishlist.friend.FriendsActivity;
import com.wish.wishlist.login.UserLoginActivity;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.wish.MyWishActivity;

import java.io.File;
import static com.wish.wishlist.R.style.AppCompatAlertDialogStyle;

/***
 * Base activity class for all activities that show navigation drawer
 */
public abstract class DrawerActivity extends ActivityBase {
    public static final String TAG = "DrawerActivity";

    protected DrawerLayout mDrawerLayout;
    protected NavigationView mNavigationView;
    protected View mNavigationViewHeader;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected RelativeLayout mHeaderLayout;
    protected Object mBusEventListener;

    protected abstract void setContentView();

    protected void prepareDrawerList() {}
    protected boolean onTapAdd() { return true; }
    protected boolean switchView(int viewType) { return true; }
    protected boolean mapView() { return true; }
    protected void drawerOpened() {}
    protected boolean goBack() { return true; }

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Otto EventBust cannot delivere events to a base class, workaround is to create
        // an Object and let it listen to the events and forward to the base class, see
        // https://github.com/square/otto/issues/26
        mBusEventListener = new Object() {
            @Subscribe
            public void profileChanged(ProfileChangeEvent event) {
                Log.d(TAG, "profileChanged " + event.type.toString());
                DrawerActivity.this.profileChanged(event);
            }
        };

        // Listen for profile change events
        EventBus.getInstance().register(mBusEventListener);

        setContentView();

        setupNavigationDrawer();

        // hide different drawer menu options for different activities
        prepareDrawerList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getInstance().unregister(mBusEventListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // we need this so when top left three bar button tapped, navigation drawer will open
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return false;
    }

    protected void profileChanged(ProfileChangeEvent event) {
        Log.d(TAG, "profileChanged " + event.type.toString());
        if (event.type == ProfileChangeEvent.ProfileChangeType.email) {
            setupUserEmail();
        } else if (event.type == ProfileChangeEvent.ProfileChangeType.name) {
            setupUserName();
        } else if (event.type == ProfileChangeEvent.ProfileChangeType.image) {
            setupProfileImage();
        } else if (event.type == ProfileChangeEvent.ProfileChangeType.all) {
            setupUserEmail();
            setupUserName();
            setupProfileImage();
        }
    }

    private void setupUserEmail() {
        TextView emailTextView = (TextView) mHeaderLayout.findViewById(R.id.email);
        emailTextView.setText(ParseUser.getCurrentUser().getEmail());
    }

    private void setupUserName() {
        TextView nameTextView = (TextView) mHeaderLayout.findViewById(R.id.username);
        nameTextView.setText(ParseUser.getCurrentUser().getString("name"));
    }

    private void setupProfileImage() {
        // set profile image in the header
        if (ProfileActivity.profileImageName() != null) {
            final File profileImageFile = new File(getFilesDir(), ProfileActivity.profileImageName());
            final Bitmap bitmap = BitmapFactory.decodeFile(profileImageFile.getAbsolutePath());
            final ImageView profileImageView = (ImageView) mNavigationViewHeader.findViewById(R.id.profile_image);
            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap);
            }
        }
    }

    protected void disableDrawer()
    {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "home selected, go back");
                finish();
            }
        });
    }

    protected void setupNavigationDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationViewHeader = mNavigationView.inflateHeaderView(R.layout.navigation_drawer_header);
        mHeaderLayout = (RelativeLayout) mNavigationViewHeader.findViewById(R.id.drawer_header_layout);
        mHeaderLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ParseUser.getCurrentUser() != null) {
                    startActivity(new Intent(DrawerActivity.this, ProfileActivity.class));
                } else {
                    startActivity(new Intent(DrawerActivity.this, UserLoginActivity.class));
                }
            }
        });

        if (ParseUser.getCurrentUser() != null) {
            setupUserName();
            setupUserEmail();
        }
        setupProfileImage();

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                //Closing drawer on item click
                mDrawerLayout.closeDrawers();

                //Check to see which item was being clicked and perform appropriate action
                switch (menuItem.getItemId()) {
                    case R.id.Add:
                        return onTapAdd();
                    case R.id.all_wishes:
                        return goBack();
                    case R.id.my_wish:
                        Intent intent = new Intent(getApplicationContext(), MyWishActivity.class);
                        startActivity(intent);
                        finish();
                        return true;
                    case R.id.list_view:
                        return switchView(Options.View.LIST);
                    case R.id.grid_view:
                        return switchView(Options.View.GRID);
                    case R.id.map_view:
                        return mapView();
                    case R.id.settings:
                        Intent prefIntent = new Intent(getApplicationContext(), PreferenceActivity.class);
                        startActivity(prefIntent);
                        return true;
                    case R.id.friends:
                        if (ParseUser.getCurrentUser() == null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(DrawerActivity.this, AppCompatAlertDialogStyle);
                            String message = "Sign up or login to add friends and see their wishes";
                            builder.setMessage(message);
                            builder.setPositiveButton("Sign up/login", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    startActivity(new Intent(DrawerActivity.this, UserLoginActivity.class));
                                }
                            });
                            builder.setNegativeButton("CANCEL",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });

                            AlertDialog dialog;
                            dialog = builder.create();
                            dialog.show();
                        } else {
                            final Intent friendsIntent = new Intent(getApplicationContext(), FriendsActivity.class);
                            startActivity(friendsIntent);
                        }
                        return true;
                    default:
                        return true;
                }
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
            /** Called when a drawer has settled in a completely closed state. */
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            /** Called when a drawer has settled in a completely open state. */
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                drawerOpened();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Calling sync state is needed or the hamburger icon wont show up
        mDrawerToggle.syncState();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
}
