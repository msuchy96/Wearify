package com.seapip.thomas.wearify;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.seapip.thomas.wearify.Browse.Activity;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.CurrentlyPlaying;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Util;
import com.squareup.picasso.Picasso;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.seapip.thomas.wearify.Spotify.Util.largestImageUrl;

public class NowPlayingActivity extends Activity {

    private boolean mAmbient;
    private WearableDrawerLayout mDrawerLayout;
    private ImageView mBackgroundImage;
    private RoundImageButtonView mPlay;
    private RoundImageButtonView mPrev;
    private RoundImageButtonView mNext;
    private RoundImageButtonView mVolDown;
    private RoundImageButtonView mVolUp;
    private TextView mTitle;
    private TextView mSubTitle;
    private CircularProgressView mProgress;
    private Handler mProgressHandler;
    private CurrentlyPlaying mCurrentlyPlaying;
    private WearableActionDrawer mActionDrawer;
    private MenuItem mShuffleMenuItem;
    private MenuItem mRepeatMenuItem;
    private MenuItem mDeviceMenuItem;
    private Callback<CurrentlyPlaying> mPlaybackCallback;
    private long mIgnoreCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        setContentView(R.layout.activity_now_playing);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer), null);

        mDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mBackgroundImage = (ImageView) findViewById(R.id.background_image);
        mPlay = (RoundImageButtonView) findViewById(R.id.button_play);
        mPrev = (RoundImageButtonView) findViewById(R.id.button_prev);
        mNext = (RoundImageButtonView) findViewById(R.id.button_next);
        mVolDown = (RoundImageButtonView) findViewById(R.id.button_vol_down);
        mVolUp = (RoundImageButtonView) findViewById(R.id.button_vol_up);
        mTitle = (TextView) findViewById(R.id.title);
        mSubTitle = (TextView) findViewById(R.id.sub_title);
        mProgress = (CircularProgressView) findViewById(R.id.circle_progress);
        mProgressHandler = new Handler();

        mPlaybackCallback = new Callback<CurrentlyPlaying>() {
            @Override
            public void onSuccess(final CurrentlyPlaying currentlyPlaying) {
                if(currentlyPlaying.item != null && System.currentTimeMillis() > mIgnoreCallbacks) {
                    if (mCurrentlyPlaying == null || !mCurrentlyPlaying.item.id.equals(currentlyPlaying.item.id)) {
                        if (!mAmbient) {
                            mBackgroundImage.setVisibility(VISIBLE);
                        }
                        Picasso.with(getApplicationContext())
                                .load(largestImageUrl(currentlyPlaying.item.album.images))
                                .fit().into(mBackgroundImage);
                        mTitle.setText(currentlyPlaying.item.name);
                        mSubTitle.setText(Util.names(currentlyPlaying.item.artists));
                    } else if(mCurrentlyPlaying.device.id.equals(currentlyPlaying.device.id)) {
                        //These kind of fixes make me cry...
                        currentlyPlaying.device.volume_percent = mCurrentlyPlaying.device.volume_percent;
                    }
                    mCurrentlyPlaying = currentlyPlaying;
                    setPlayIcon();
                    setShuffleIcon();
                    setRepeatIcon();
                    switch (currentlyPlaying.device.type) {
                        case "Smartphone":
                            mDeviceMenuItem.setIcon(R.drawable.ic_smartphone_black_24dp);
                            break;
                        case "Tablet":
                            mDeviceMenuItem.setIcon(R.drawable.ic_tablet_black_24dp);
                            break;
                        default:
                        case "Computer":
                            mDeviceMenuItem.setIcon(R.drawable.ic_computer_black_24dp);
                            break;
                    }
                    mDeviceMenuItem.setTitle(currentlyPlaying.device.name);
                }
            }
        };

        Manager.onPlayback(mPlaybackCallback);
        onProgress();

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentlyPlaying != null) {
                    mCurrentlyPlaying.is_playing = !mCurrentlyPlaying.is_playing;
                    if(mCurrentlyPlaying.is_playing) {
                        Manager.resume(null);
                    } else {
                        Manager.pause(null);
                    }
                    setPlayIcon();
                }
            }
        });
        mPlay.setClickAlpha(80);

        mPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBackgroundImage.getVisibility() == VISIBLE) {
                    Manager.prev(null);
                    setLoading();
                }
            }
        });
        mPrev.setClickAlpha(20);

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBackgroundImage.getVisibility() == VISIBLE) {
                    Manager.next(null);
                    setLoading();
                }
            }
        });
        mNext.setClickAlpha(20);

        mVolDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCurrentlyPlaying != null) {
                    mCurrentlyPlaying.device.volume_percent = Math.min(100, mCurrentlyPlaying.device.volume_percent - 10);
                    Manager.volume(mCurrentlyPlaying.device.volume_percent, null);
                }
            }
        });
        mVolDown.setClickAlpha(20);

        mVolUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCurrentlyPlaying != null) {
                    mCurrentlyPlaying.device.volume_percent = Math.max(0, mCurrentlyPlaying.device.volume_percent + 10);
                    Manager.volume(mCurrentlyPlaying.device.volume_percent, null);
                }
            }
        });
        mVolUp.setClickAlpha(20);

        mActionDrawer = (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);
        Menu menu = mActionDrawer.getMenu();
        mShuffleMenuItem = menu.add("Shuffle").setIcon(getDrawable(R.drawable.ic_shuffle_black_24dp));
        mShuffleMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                if (mCurrentlyPlaying != null) {
                    mCurrentlyPlaying.shuffle_state = !mCurrentlyPlaying.shuffle_state;
                    setShuffleIcon();
                    Manager.shuffle(mCurrentlyPlaying.shuffle_state, null);
                }
                return false;
            }
        });
        mRepeatMenuItem = menu.add("Repeat").setIcon(getDrawable(R.drawable.ic_repeat_black_24dp));
        mRepeatMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                if (mCurrentlyPlaying != null) {
                    switch (mCurrentlyPlaying.repeat_state) {
                        case "off":
                            mCurrentlyPlaying.repeat_state = "context";
                            break;
                        case "context":
                            mCurrentlyPlaying.repeat_state = "track";
                            break;
                        case "track":
                            mCurrentlyPlaying.repeat_state = "off";
                            break;
                    }
                    setRepeatIcon();
                    Manager.repeat(mCurrentlyPlaying.repeat_state, null);
                }
                return false;
            }
        });
        mDeviceMenuItem = menu.add("Devices").setIcon(getDrawable(R.drawable.ic_devices_other_black_24dp));
    }

    private void setLoading() {
        mBackgroundImage.setVisibility(GONE);
        mTitle.setText("");
        mSubTitle.setText("");
    }

    private void setPlayIcon() {
        mPlay.setImageDrawable(
                getDrawable(mCurrentlyPlaying != null && mCurrentlyPlaying.is_playing ?
                        mAmbient ?
                                R.drawable.ic_pause_black_burn_in_24dp :
                                R.drawable.ic_pause_black_24dp :
                        mAmbient ?
                                R.drawable.ic_play_arrow_black_burn_in_24dp :
                                R.drawable.ic_play_arrow_black_24dp));
    }

    private void setShuffleIcon() {
        mShuffleMenuItem.setIcon(mCurrentlyPlaying.shuffle_state ?
                R.drawable.ic_shuffle_black_24dp : R.drawable.ic_shuffle_disabled_black_24px);
    }

    private void setRepeatIcon() {
        switch (mCurrentlyPlaying.repeat_state) {
            case "off":
                mRepeatMenuItem.setIcon(R.drawable.ic_repeat_disabled_black_24px);
                break;
            case "context":
                mRepeatMenuItem.setIcon(R.drawable.ic_repeat_black_24dp);
                break;
            case "track":
                mRepeatMenuItem.setIcon(R.drawable.ic_repeat_one_black_24dp);
                break;
        }
    }

    private void onProgress() {
        (new Runnable() {
            @Override
            public void run() {
                if (mCurrentlyPlaying != null && mCurrentlyPlaying.item.duration_ms > 0) {
                    mProgress.setProgress((float) (System.currentTimeMillis() - mCurrentlyPlaying.timestamp)
                            / (float) mCurrentlyPlaying.item.duration_ms * 100f);
                    mBackgroundImage.invalidate();
                }
                mProgressHandler.postDelayed(this, 20);
            }
        }).run();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Manager.onPlayback(mPlaybackCallback);
        onProgress();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Manager.offPlayback();
        mProgressHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Manager.offPlayback();
        mProgressHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mAmbient = true;
        Manager.offPlayback();
        mProgressHandler.removeCallbacksAndMessages(null);
        mDrawerLayout.setBackgroundColor(Color.BLACK);
        mBackgroundImage.setVisibility(GONE);
        mProgress.setVisibility(GONE);
        mActionDrawer.setVisibility(GONE);
        setPlayIcon();
        mPlay.setBackgroundColor(Color.TRANSPARENT);
        mPlay.setTint(Color.WHITE);
        mNext.setImageDrawable(getDrawable(R.drawable.ic_skip_next_black_burn_in_24dp));
        mPrev.setImageDrawable(getDrawable(R.drawable.ic_skip_previous_black_burn_in_24dp));
        mVolDown.setImageDrawable(getDrawable(R.drawable.ic_volume_down_black_burn_in_24dp));
        mVolUp.setImageDrawable(getDrawable(R.drawable.ic_volume_up_black_burn_in_24dp));
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mAmbient = false;
        Manager.onPlayback(mPlaybackCallback);
        onProgress();
        mDrawerLayout.setBackgroundColor(Color.parseColor("#141414"));
        mBackgroundImage.setVisibility(VISIBLE);
        mProgress.setVisibility(VISIBLE);
        mActionDrawer.setVisibility(VISIBLE);
        setPlayIcon();
        mPlay.setBackgroundColor(Color.parseColor("#00ffe0"));
        mPlay.setTint(Color.argb(180, 0, 0, 0));
        mNext.setImageDrawable(getDrawable(R.drawable.ic_skip_next_black_24dp));
        mPrev.setImageDrawable(getDrawable(R.drawable.ic_skip_previous_black_24dp));
        mVolDown.setImageDrawable(getDrawable(R.drawable.ic_volume_down_black_24dp));
        mVolUp.setImageDrawable(getDrawable(R.drawable.ic_volume_up_black_24dp));
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        Manager.getPlayback(mPlaybackCallback);
    }
}
