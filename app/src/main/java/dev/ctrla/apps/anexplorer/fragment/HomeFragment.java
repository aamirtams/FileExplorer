
package dev.ctrla.apps.anexplorer.fragment;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dev.ctrla.apps.anexplorer.BaseActivity;
import dev.ctrla.apps.anexplorer.DocumentsActivity;
import dev.ctrla.apps.anexplorer.DocumentsApplication;
import dev.ctrla.apps.anexplorer.R;
import dev.ctrla.apps.anexplorer.adapter.RecentsAdapter;
import dev.ctrla.apps.anexplorer.adapter.ShortcutsAdapter;
import dev.ctrla.apps.anexplorer.cursor.LimitCursorWrapper;
import dev.ctrla.apps.anexplorer.loader.RecentLoader;
import dev.ctrla.apps.anexplorer.misc.AnalyticsManager;
import dev.ctrla.apps.anexplorer.misc.AsyncTask;
import dev.ctrla.apps.anexplorer.misc.CrashReportingManager;
import dev.ctrla.apps.anexplorer.misc.IconUtils;
import dev.ctrla.apps.anexplorer.misc.RootsCache;
import dev.ctrla.apps.anexplorer.misc.Utils;
import dev.ctrla.apps.anexplorer.model.DirectoryResult;
import dev.ctrla.apps.anexplorer.model.DocumentInfo;
import dev.ctrla.apps.anexplorer.model.RootInfo;
import dev.ctrla.apps.anexplorer.provider.AppsProvider;
import dev.ctrla.apps.anexplorer.setting.SettingsActivity;
import dev.ctrla.apps.anexplorer.ui.AdWrapper;
import dev.ctrla.apps.anexplorer.ui.HomeItem;
import dev.ctrla.apps.anexplorer.ui.MaterialProgressDialog;

import static dev.ctrla.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.ctrla.apps.anexplorer.misc.AnalyticsManager.FILE_TYPE;
import static dev.ctrla.apps.anexplorer.provider.AppsProvider.getRunningAppProcessInfo;

/**
 * Display home.
 */
public class HomeFragment extends Fragment {
    public static final String TAG = "HomeFragment";
    private static final int MAX_RECENT_COUNT = isTelevision() ? 20 : 10;

    private final int mLoaderId = 42;
    private HomeItem storageStats;
    private HomeItem memoryStats;
    private Timer storageTimer;
    private Timer secondatyStorageTimer;
    private Timer usbStorageTimer;
    private Timer processTimer;
    private RootsCache roots;
    private RecyclerView mRecentsRecycler;
    private RecyclerView mShortcutsRecycler;
    private RecentsAdapter mRecentsAdapter;
    private LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks;
    private View recents_container;
    private TextView recents;
    private ShortcutsAdapter mShortcutsAdapter;
    private RootInfo mHomeRoot;
    private HomeItem secondayStorageStats;
    private HomeItem usbStorageStats;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private AdWrapper mAdView1;
    private AdWrapper mInters;

    public static void show(FragmentManager fm) {
        final HomeFragment fragment = new HomeFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    public static HomeFragment get(FragmentManager fm) {
        return (HomeFragment) fm.findFragmentByTag(TAG);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storageTimer = new Timer();
        secondatyStorageTimer = new Timer();
        usbStorageTimer = new Timer();
        processTimer = new Timer();
        storageStats = (HomeItem) view.findViewById(R.id.storage_stats);
        secondayStorageStats = (HomeItem) view.findViewById(R.id.seconday_storage_stats);
        usbStorageStats = (HomeItem) view.findViewById(R.id.usb_storage_stats);
        memoryStats = (HomeItem) view.findViewById(R.id.memory_stats);
        recents = (TextView)view.findViewById(R.id.recents);
        recents_container = view.findViewById(R.id.recents_container);

        mShortcutsRecycler = (RecyclerView) view.findViewById(R.id.shortcuts_recycler);
        mRecentsRecycler = (RecyclerView) view.findViewById(R.id.recents_recycler);

        roots = DocumentsApplication.getRootsCache(getActivity());
        mHomeRoot = roots.getHomeRoot();
        showRecents();
        showData();

      /*  AdView adView = new AdView(getContext());
        adView.setAdSize(AdSize.SMART_BANNER);
        adView.setAdUnitId("ca-app-pub-5246243065157193/5355630353");
        mAdView =(com.google.android.gms.ads.AdView)view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
             mAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                mAdView.setVisibility(View.GONE);

            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
            }
        });

        mInterstitialAd = new InterstitialAd(getContext());
        mInterstitialAd.setAdUnitId("ca-app-pub-5246243065157193/5874505107");
        if (!mInterstitialAd.isLoading() && !mInterstitialAd.isLoaded()) {

           // AdRequest adRequest = new AdRequest.Builder().build();

            mInterstitialAd.loadAd(adRequest);

        }
        mInterstitialAd.setAdListener(new AdListener() {

            @Override

            public void onAdLoaded() {

              showInterstitial();

            }

        });*/

    }
    private void showInterstitial() {
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {

            mInterstitialAd.show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        mInters.initInterstitialAd();
        updateUI();


    }

    public void showData(){
        updateUI();
        showStorage();
        showOtherStorage();
        showMemory(0);
        showShortcuts();
        getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
    }

    private void updateUI() {
        recents_container.setVisibility(SettingsActivity.getDisplayRecentMedia() ? View.VISIBLE : View.GONE);
        roots = DocumentsApplication.getRootsCache(getActivity());
        int accentColor = SettingsActivity.getAccentColor();
        recents.setTextColor(accentColor);
        storageStats.updateColor();
        memoryStats.updateColor();
        secondayStorageStats.updateColor();
        usbStorageStats.updateColor();
    }

    public void reloadData(){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showData();
            }
        }, 500);
    }

    private void showStorage() {
        final RootInfo primaryRoot = roots.getPrimaryRoot();
        if (null != primaryRoot) {
            storageStats.setVisibility(View.VISIBLE);
            storageStats.setInfo(primaryRoot);
            storageStats.setAction(R.drawable.ic_analyze, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((DocumentsActivity)getActivity()).showInfo("Coming Soon!");
                    Bundle params = new Bundle();
                    AnalyticsManager.logEvent("storage_analyze", params);
                }
            });
            storageStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openRoot(primaryRoot);
                }
            });
            storageTimer = new Timer();
            animateProgress(storageStats, storageTimer, primaryRoot);
        } else {
            storageStats.setVisibility(View.GONE);
        }
    }


    private void showOtherStorage() {
        final RootInfo secondaryRoot = roots.getSecondaryRoot();
        if (null != secondaryRoot) {
            secondayStorageStats.setVisibility(View.VISIBLE);
            secondayStorageStats.setInfo(secondaryRoot);
            secondayStorageStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openRoot(secondaryRoot);
                }
            });
            secondatyStorageTimer = new Timer();
            animateProgress(secondayStorageStats, secondatyStorageTimer, secondaryRoot);
        } else {
            secondayStorageStats.setVisibility(View.GONE);
        }

        final RootInfo usbRoot = roots.getUSBRoot();
        if (null != usbRoot) {
            usbStorageStats.setVisibility(View.VISIBLE);
            usbStorageStats.setInfo(usbRoot);
            usbStorageStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openRoot(usbRoot);
                }
            });
            usbStorageTimer = new Timer();
            animateProgress(usbStorageStats, usbStorageTimer, usbRoot);
        } else {
            usbStorageStats.setVisibility(View.GONE);
        }
    }

    private void showMemory(long currentAvailableBytes) {

        final RootInfo processRoot = roots.getProcessRoot();
        if (null != processRoot) {
            memoryStats.setVisibility(View.VISIBLE);
            memoryStats.setInfo(processRoot);
            memoryStats.setAction(R.drawable.ic_clean, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new OperationTask(processRoot).execute();
                    Bundle params = new Bundle();
                    AnalyticsManager.logEvent("process_clean", params);
                }
            });
            memoryStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openRoot(processRoot);
                }
            });
            if(currentAvailableBytes != 0) {
                long availableBytes = processRoot.availableBytes - currentAvailableBytes;
                String summaryText = availableBytes <= 0 ? "Already cleaned up!" :
                        getActivity().getString(R.string.root_available_bytes,
                        Formatter.formatFileSize(getActivity(), availableBytes));
                ((DocumentsActivity) getActivity()).showInfo(summaryText);
            }

            processTimer = new Timer();
            animateProgress(memoryStats, processTimer, processRoot);
        }
    }

    private void showShortcuts() {
        ArrayList<RootInfo> data = roots.getShortcutsInfo();
        mShortcutsAdapter = new ShortcutsAdapter(getActivity(), data);
        mShortcutsAdapter.setOnItemClickListener(new ShortcutsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ShortcutsAdapter.ViewHolder item, int position) {
                openRoot(mShortcutsAdapter.getItem(position));
            }
        });
        mShortcutsRecycler.setAdapter(mShortcutsAdapter);
    }

    private void showRecents() {
        final RootInfo root = roots.getRecentsRoot();
        recents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRoot(root);
            }
        });

        mRecentsAdapter = new RecentsAdapter(getActivity(), null);
        mRecentsAdapter.setOnItemClickListener(new RecentsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecentsAdapter.ViewHolder item, int position) {
                openDocument(item.mDocumentInfo);
            }
        });
        mRecentsRecycler.setAdapter(mRecentsAdapter);
        LinearSnapHelper helper = new LinearSnapHelper();
        helper.attachToRecyclerView(mRecentsRecycler);

        final BaseActivity.State state = getDisplayState(this);
        mCallbacks = new LoaderManager.LoaderCallbacks<DirectoryResult>() {

            @Override
            public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
                final RootsCache roots = DocumentsApplication.getRootsCache(getActivity());
                return new RecentLoader(getActivity(), roots, state);
            }

            @Override
            public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
                if (!isAdded())
                    return;
                if(null == result.cursor || (null != result.cursor && result.cursor.getCount() == 0)) {
                    recents_container.setVisibility(View.GONE);
                } else {
                    //recents_container.setVisibility(View.VISIBLE);
                    mRecentsAdapter.swapCursor(new LimitCursorWrapper(result.cursor, MAX_RECENT_COUNT));
                }
            }

            @Override
            public void onLoaderReset(Loader<DirectoryResult> loader) {
                mRecentsAdapter.swapCursor(null);
            }
        };
        getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        storageTimer.cancel();
        secondatyStorageTimer.cancel();
        usbStorageTimer.cancel();
        processTimer.cancel();
    }

    private class OperationTask extends AsyncTask<Void, Void, Boolean> {

        private MaterialProgressDialog progressDialog;
        private RootInfo root;
        private long currentAvailableBytes;

        public OperationTask(RootInfo root) {
            progressDialog = new MaterialProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.setColor(SettingsActivity.getAccentColor());
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Cleaning up RAM...");
            this.root = root;
            currentAvailableBytes = root.availableBytes;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;
            cleanupMemory(getActivity());
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Utils.isActivityAlive(getActivity())) {
                return;
            }
            AppsProvider.notifyDocumentsChanged(getActivity(), root.rootId);
            AppsProvider.notifyRootsChanged(getActivity());
            RootsCache.updateRoots(getActivity(), AppsProvider.AUTHORITY);
            roots = DocumentsApplication.getRootsCache(getActivity());
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showMemory(currentAvailableBytes);
                    progressDialog.dismiss();
                }
            }, 500);
        }
    }

    private void animateProgress(final HomeItem item, final Timer timer, RootInfo root){
        try {
            final double percent = (((root.totalBytes - root.availableBytes) / (double) root.totalBytes) * 100);
            item.setProgress(0);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(Utils.isActivityAlive(getActivity())){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (item.getProgress() >= (int) percent) {
                                    timer.cancel();
                                } else {
                                    item.setProgress(item.getProgress() + 1);
                                }
                            }
                        });
                    }
                }
            }, 50, 20);
        }
        catch (Exception e){
            item.setVisibility(View.GONE);
            CrashReportingManager.logException(e);
        }
    }

    private static BaseActivity.State getDisplayState(Fragment fragment) {
        return ((BaseActivity) fragment.getActivity()).getDisplayState();
    }

    private void openRoot(RootInfo rootInfo){
        DocumentsActivity activity = ((DocumentsActivity)getActivity());
        activity.onRootPicked(rootInfo, mHomeRoot);
        AnalyticsManager.logEvent("open_shortcuts", rootInfo ,new Bundle());
    }

    public void cleanupMemory(Context context){
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcessesList = getRunningAppProcessInfo(context);
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcessesList) {
            activityManager.killBackgroundProcesses(processInfo.processName);
        }
    }

    private void openDocument(DocumentInfo doc) {
        ((BaseActivity) getActivity()).onDocumentPicked(doc);
        Bundle params = new Bundle();
        String type = IconUtils.getTypeNameFromMimeType(doc.mimeType);
        params.putString(FILE_TYPE, type);
        AnalyticsManager.logEvent("open_image_recent", params);
    }
}