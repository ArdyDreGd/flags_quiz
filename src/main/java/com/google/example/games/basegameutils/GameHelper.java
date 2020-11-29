package com.google.example.games.basegameutils;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Api.ApiOptions.NoOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Games.GamesOptions;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.request.GameRequest;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.Plus.PlusOptions;

public class GameHelper implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    static final String TAG = "GameHelper";

    public interface GameHelperListener {

        void onSignInFailed();

        void onSignInSucceeded();
    }

    private boolean mSetupDone = false;

    private boolean mConnecting = false;

    boolean mExpectingResolution = false;

    boolean mSignInCancelled = false;

    Activity mActivity = null;

    Context mAppContext = null;

    final static int RC_RESOLVE = 9001;

    final static int RC_UNUSED = 9002;

    GoogleApiClient.Builder mGoogleApiClientBuilder = null;

    GamesOptions mGamesApiOptions = GamesOptions.builder().build();
    PlusOptions mPlusApiOptions = null;

    GoogleApiClient mGoogleApiClient = null;

    public final static int CLIENT_NONE = 0x00;
    public final static int CLIENT_GAMES = 0x01;
    public final static int CLIENT_PLUS = 0x02;
    public final static int CLIENT_SNAPSHOT = 0x08;
    public final static int CLIENT_ALL = CLIENT_GAMES | CLIENT_PLUS
            | CLIENT_SNAPSHOT;

    int mRequestedClients = CLIENT_NONE;

    boolean mConnectOnStart = true;

    boolean mUserInitiatedSignIn = false;

    ConnectionResult mConnectionResult = null;

    SignInFailureReason mSignInFailureReason = null;

    boolean mShowErrorDialogs = true;

    boolean mDebugLog = false;

    Handler mHandler;

    Invitation mInvitation;

    TurnBasedMatch mTurnBasedMatch;

    ArrayList<GameRequest> mRequests;

    GameHelperListener mListener = null;

    static final int DEFAULT_MAX_SIGN_IN_ATTEMPTS = 3;
    int mMaxAutoSignInAttempts = DEFAULT_MAX_SIGN_IN_ATTEMPTS;

    public GameHelper(Activity activity, int clientsToUse) {
        mActivity = activity;
        mAppContext = activity.getApplicationContext();
        mRequestedClients = clientsToUse;
        mHandler = new Handler();
    }

    public void setMaxAutoSignInAttempts(int max) {
        mMaxAutoSignInAttempts = max;
    }

    void assertConfigured(String operation) {
        if (!mSetupDone) {
            String error = "GameHelper error: Operation attempted without setup: "
                    + operation
                    + ". The setup() method must be called before attempting any other operation.";
            logError(error);
            throw new IllegalStateException(error);
        }
    }

    private void doApiOptionsPreCheck() {
        if (mGoogleApiClientBuilder != null) {
            String error = "GameHelper: you cannot call set*ApiOptions after the client "
                    + "builder has been created. Call it before calling createApiClientBuilder() "
                    + "or setup().";
            logError(error);
            throw new IllegalStateException(error);
        }
    }

    public void setGamesApiOptions(GamesOptions options) {
        doApiOptionsPreCheck();
        mGamesApiOptions = options;
    }

    public void setPlusApiOptions(PlusOptions options) {
        doApiOptionsPreCheck();
        mPlusApiOptions = options;
    }

    public GoogleApiClient.Builder createApiClientBuilder() {
        if (mSetupDone) {
            String error = "GameHelper: you called GameHelper.createApiClientBuilder() after "
                    + "calling setup. You can only get a client builder BEFORE performing setup.";
            logError(error);
            throw new IllegalStateException(error);
        }

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(
                mActivity, this, this);

        if (0 != (mRequestedClients & CLIENT_GAMES)) {
            builder.addApi(Games.API, mGamesApiOptions);
            builder.addScope(Games.SCOPE_GAMES);
        }

        if (0 != (mRequestedClients & CLIENT_PLUS)) {
            builder.addApi(Plus.API);
            builder.addScope(Plus.SCOPE_PLUS_LOGIN);
        }

        if (0 != (mRequestedClients & CLIENT_SNAPSHOT)) {
            builder.addScope(Drive.SCOPE_APPFOLDER);
            builder.addApi(Drive.API);
        }

        mGoogleApiClientBuilder = builder;
        return builder;
    }

    public void setup(GameHelperListener listener) {
        if (mSetupDone) {
            String error = "GameHelper: you cannot call GameHelper.setup() more than once!";
            logError(error);
            throw new IllegalStateException(error);
        }
        mListener = listener;
        debugLog("Setup: requested clients: " + mRequestedClients);

        if (mGoogleApiClientBuilder == null) {
            createApiClientBuilder();
        }

        mGoogleApiClient = mGoogleApiClientBuilder.build();
        mGoogleApiClientBuilder = null;
        mSetupDone = true;
    }

    public GoogleApiClient getApiClient() {
        if (mGoogleApiClient == null) {
            throw new IllegalStateException(
                    "No GoogleApiClient. Did you call setup()?");
        }
        return mGoogleApiClient;
    }

    public boolean isSignedIn() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    public boolean isConnecting() {
        return mConnecting;
    }

    public boolean hasSignInError() {
        return mSignInFailureReason != null;
    }

    public SignInFailureReason getSignInError() {
        return mSignInFailureReason;
    }

    public void setShowErrorDialogs(boolean show) {
        mShowErrorDialogs = show;
    }

    public void onStart(Activity act) {
        mActivity = act;
        mAppContext = act.getApplicationContext();

        debugLog("onStart");
        assertConfigured("onStart");

        if (mConnectOnStart) {
            if (mGoogleApiClient.isConnected()) {
                Log.w(TAG,
                        "GameHelper: client was already connected on onStart()");
            } else {
                debugLog("Connecting client.");
                mConnecting = true;
                mGoogleApiClient.connect();
            }
        } else {
            debugLog("Not attempting to connect becase mConnectOnStart=false");
            debugLog("Instead, reporting a sign-in failure.");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyListener(false);
                }
            }, 1000);
        }
    }

    public void onStop() {
        debugLog("onStop");
        assertConfigured("onStop");
        if (mGoogleApiClient.isConnected()) {
            debugLog("Disconnecting client due to onStop");
            mGoogleApiClient.disconnect();
        } else {
            debugLog("Client already disconnected when we got onStop.");
        }
        mConnecting = false;
        mExpectingResolution = false;

        mActivity = null;
    }

    public String getInvitationId() {
        if (!mGoogleApiClient.isConnected()) {
            Log.w(TAG,
                    "Warning: getInvitationId() should only be called when signed in, "
                            + "that is, after getting onSignInSuceeded()");
        }
        return mInvitation == null ? null : mInvitation.getInvitationId();
    }

    public Invitation getInvitation() {
        if (!mGoogleApiClient.isConnected()) {
            Log.w(TAG,
                    "Warning: getInvitation() should only be called when signed in, "
                            + "that is, after getting onSignInSuceeded()");
        }
        return mInvitation;
    }

    public boolean hasInvitation() {
        return mInvitation != null;
    }

    public boolean hasTurnBasedMatch() {
        return mTurnBasedMatch != null;
    }

    public boolean hasRequests() {
        return mRequests != null;
    }

    public void clearInvitation() {
        mInvitation = null;
    }

    public void clearTurnBasedMatch() {
        mTurnBasedMatch = null;
    }

    public void clearRequests() {
        mRequests = null;
    }

    public TurnBasedMatch getTurnBasedMatch() {
        if (!mGoogleApiClient.isConnected()) {
            Log.w(TAG,
                    "Warning: getTurnBasedMatch() should only be called when signed in, "
                            + "that is, after getting onSignInSuceeded()");
        }
        return mTurnBasedMatch;
    }

    public ArrayList<GameRequest> getRequests() {
        if (!mGoogleApiClient.isConnected()) {
            Log.w(TAG, "Warning: getRequests() should only be called "
                    + "when signed in, "
                    + "that is, after getting onSignInSuceeded()");
        }
        return mRequests;
    }

    public void enableDebugLog(boolean enabled) {
        mDebugLog = enabled;
        if (enabled) {
            debugLog("Debug log enabled.");
        }
    }

    @Deprecated
    public void enableDebugLog(boolean enabled, String tag) {
        Log.w(TAG, "GameHelper.enableDebugLog(boolean,String) is deprecated. "
                + "Use GameHelper.enableDebugLog(boolean)");
        enableDebugLog(enabled);
    }

    public void signOut() {
        if (!mGoogleApiClient.isConnected()) {
            debugLog("signOut: was already disconnected, ignoring.");
            return;
        }

        if (0 != (mRequestedClients & CLIENT_PLUS)) {
            debugLog("Clearing default account on PlusClient.");
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        }

        if (0 != (mRequestedClients & CLIENT_GAMES)) {
            debugLog("Signing out from the Google API Client.");
            Games.signOut(mGoogleApiClient);
        }

        debugLog("Disconnecting client.");
        mConnectOnStart = false;
        mConnecting = false;
        mGoogleApiClient.disconnect();
    }

    public void onActivityResult(int requestCode, int responseCode,
                                 Intent intent) {
        debugLog("onActivityResult: req="
                + (requestCode == RC_RESOLVE ? "RC_RESOLVE" : String
                .valueOf(requestCode)) + ", resp="
                + GameHelperUtils.activityResponseCodeToString(responseCode));
        if (requestCode != RC_RESOLVE) {
            debugLog("onActivityResult: request code not meant for us. Ignoring.");
            return;
        }

        mExpectingResolution = false;

        if (!mConnecting) {
            debugLog("onActivityResult: ignoring because we are not connecting.");
            return;
        }

        if (responseCode == Activity.RESULT_OK) {
            debugLog("onAR: Resolution was RESULT_OK, so connecting current client again.");
            connect();
        } else if (responseCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            debugLog("onAR: Resolution was RECONNECT_REQUIRED, so reconnecting.");
            connect();
        } else if (responseCode == Activity.RESULT_CANCELED) {
            debugLog("onAR: Got a cancellation result, so disconnecting.");
            mSignInCancelled = true;
            mConnectOnStart = false;
            mUserInitiatedSignIn = false;
            mSignInFailureReason = null;
            mConnecting = false;
            mGoogleApiClient.disconnect();

            int prevCancellations = getSignInCancellations();
            int newCancellations = incrementSignInCancellations();
            debugLog("onAR: # of cancellations " + prevCancellations + " --> "
                    + newCancellations + ", max " + mMaxAutoSignInAttempts);

            notifyListener(false);
        } else {
            debugLog("onAR: responseCode="
                    + GameHelperUtils
                    .activityResponseCodeToString(responseCode)
                    + ", so giving up.");
            giveUp(new SignInFailureReason(mConnectionResult.getErrorCode(),
                    responseCode));
        }
    }

    void notifyListener(boolean success) {
        debugLog("Notifying LISTENER of sign-in "
                + (success ? "SUCCESS"
                : mSignInFailureReason != null ? "FAILURE (error)"
                : "FAILURE (no error)"));
        if (mListener != null) {
            if (success) {
                mListener.onSignInSucceeded();
            } else {
                mListener.onSignInFailed();
            }
        }
    }

    public void beginUserInitiatedSignIn() {
        debugLog("beginUserInitiatedSignIn: resetting attempt count.");
        resetSignInCancellations();
        mSignInCancelled = false;
        mConnectOnStart = true;

        if (mGoogleApiClient.isConnected()) {
            logWarn("beginUserInitiatedSignIn() called when already connected. "
                    + "Calling listener directly to notify of success.");
            notifyListener(true);
            return;
        } else if (mConnecting) {
            logWarn("beginUserInitiatedSignIn() called when already connecting. "
                    + "Be patient! You can only call this method after you get an "
                    + "onSignInSucceeded() or onSignInFailed() callback. Suggestion: disable "
                    + "the sign-in button on startup and also when it's clicked, and re-enable "
                    + "when you get the callback.");
            return;
        }

        debugLog("Starting USER-INITIATED sign-in flow.");

        mUserInitiatedSignIn = true;

        if (mConnectionResult != null) {
            debugLog("beginUserInitiatedSignIn: continuing pending sign-in flow.");
            mConnecting = true;
            resolveConnectionResult();
        } else {
            debugLog("beginUserInitiatedSignIn: starting new sign-in flow.");
            mConnecting = true;
            connect();
        }
    }

    void connect() {
        if (mGoogleApiClient.isConnected()) {
            debugLog("Already connected.");
            return;
        }
        debugLog("Starting connection.");
        mConnecting = true;
        mInvitation = null;
        mTurnBasedMatch = null;
        mGoogleApiClient.connect();
    }

    public void reconnectClient() {
        if (!mGoogleApiClient.isConnected()) {
            Log.w(TAG, "reconnectClient() called when client is not connected.");
            connect();
        } else {
            debugLog("Reconnecting client.");
            mGoogleApiClient.reconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        debugLog("onConnected: connected!");

        if (connectionHint != null) {
            debugLog("onConnected: connection hint provided. Checking for invite.");
            Invitation inv = connectionHint
                    .getParcelable(Multiplayer.EXTRA_INVITATION);
            if (inv != null && inv.getInvitationId() != null) {
                debugLog("onConnected: connection hint has a room invite!");
                mInvitation = inv;
                debugLog("Invitation ID: " + mInvitation.getInvitationId());
            }

            mRequests = Games.Requests
                    .getGameRequestsFromBundle(connectionHint);
            if (!mRequests.isEmpty()) {
                debugLog("onConnected: connection hint has " + mRequests.size()
                        + " request(s)");
            }

            debugLog("onConnected: connection hint provided. Checking for TBMP game.");
            mTurnBasedMatch = connectionHint
                    .getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);
        }

        succeedSignIn();
    }

    void succeedSignIn() {
        debugLog("succeedSignIn");
        mSignInFailureReason = null;
        mConnectOnStart = true;
        mUserInitiatedSignIn = false;
        mConnecting = false;
        notifyListener(true);
    }

    private final String GAMEHELPER_SHARED_PREFS = "GAMEHELPER_SHARED_PREFS";
    private final String KEY_SIGN_IN_CANCELLATIONS = "KEY_SIGN_IN_CANCELLATIONS";

    int getSignInCancellations() {
        SharedPreferences sp = mAppContext.getSharedPreferences(
                GAMEHELPER_SHARED_PREFS, Context.MODE_PRIVATE);
        return sp.getInt(KEY_SIGN_IN_CANCELLATIONS, 0);
    }

    int incrementSignInCancellations() {
        int cancellations = getSignInCancellations();
        SharedPreferences.Editor editor = mAppContext.getSharedPreferences(
                GAMEHELPER_SHARED_PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_SIGN_IN_CANCELLATIONS, cancellations + 1);
        editor.commit();
        return cancellations + 1;
    }

    void resetSignInCancellations() {
        SharedPreferences.Editor editor = mAppContext.getSharedPreferences(
                GAMEHELPER_SHARED_PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_SIGN_IN_CANCELLATIONS, 0);
        editor.commit();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        debugLog("onConnectionFailed");

        mConnectionResult = result;
        debugLog("Connection failure:");
        debugLog("   - code: "
                + GameHelperUtils.errorCodeToString(mConnectionResult
                .getErrorCode()));
        debugLog("   - resolvable: " + mConnectionResult.hasResolution());
        debugLog("   - details: " + mConnectionResult.toString());

        int cancellations = getSignInCancellations();
        boolean shouldResolve = false;

        if (mUserInitiatedSignIn) {
            debugLog("onConnectionFailed: WILL resolve because user initiated sign-in.");
            shouldResolve = true;
        } else if (mSignInCancelled) {
            debugLog("onConnectionFailed WILL NOT resolve (user already cancelled once).");
            shouldResolve = false;
        } else if (cancellations < mMaxAutoSignInAttempts) {
            debugLog("onConnectionFailed: WILL resolve because we have below the max# of "
                    + "attempts, "
                    + cancellations
                    + " < "
                    + mMaxAutoSignInAttempts);
            shouldResolve = true;
        } else {
            shouldResolve = false;
            debugLog("onConnectionFailed: Will NOT resolve; not user-initiated and max attempts "
                    + "reached: "
                    + cancellations
                    + " >= "
                    + mMaxAutoSignInAttempts);
        }

        if (!shouldResolve) {
            debugLog("onConnectionFailed: since we won't resolve, failing now.");
            mConnectionResult = result;
            mConnecting = false;
            notifyListener(false);
            return;
        }

        debugLog("onConnectionFailed: resolving problem...");
        resolveConnectionResult();
    }

    void resolveConnectionResult() {
        if (mExpectingResolution) {
            debugLog("We're already expecting the result of a previous resolution.");
            return;
        }

        if (mActivity == null) {
            debugLog("No need to resolve issue, activity does not exist anymore");
            return;
        }

        debugLog("resolveConnectionResult: trying to resolve result: "
                + mConnectionResult);
        if (mConnectionResult.hasResolution()) {
            debugLog("Result has resolution. Starting it.");
            try {
                mExpectingResolution = true;
                mConnectionResult.startResolutionForResult(mActivity,
                        RC_RESOLVE);
            } catch (SendIntentException e) {
                debugLog("SendIntentException, so connecting again.");
                connect();
            }
        } else {
            debugLog("resolveConnectionResult: result has no resolution. Giving up.");
            giveUp(new SignInFailureReason(mConnectionResult.getErrorCode()));

            mConnectionResult = null;
        }
    }

    public void disconnect() {
        if (mGoogleApiClient.isConnected()) {
            debugLog("Disconnecting client.");
            mGoogleApiClient.disconnect();
        } else {
            Log.w(TAG,
                    "disconnect() called when client was already disconnected.");
        }
    }

    void giveUp(SignInFailureReason reason) {
        mConnectOnStart = false;
        disconnect();
        mSignInFailureReason = reason;

        if (reason.mActivityResultCode == GamesActivityResultCodes.RESULT_APP_MISCONFIGURED) {
            GameHelperUtils.printMisconfiguredDebugInfo(mAppContext);
        }

        showFailureDialog();
        mConnecting = false;
        notifyListener(false);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        debugLog("onConnectionSuspended, cause=" + cause);
        disconnect();
        mSignInFailureReason = null;
        debugLog("Making extraordinary call to onSignInFailed callback");
        mConnecting = false;
        notifyListener(false);
    }

    public void showFailureDialog() {
        if (mSignInFailureReason != null) {
            int errorCode = mSignInFailureReason.getServiceErrorCode();
            int actResp = mSignInFailureReason.getActivityResultCode();

            if (mShowErrorDialogs) {
                showFailureDialog(mActivity, actResp, errorCode);
            } else {
                debugLog("Not showing error dialog because mShowErrorDialogs==false. "
                        + "" + "Error was: " + mSignInFailureReason);
            }
        }
    }

    public static void showFailureDialog(Activity activity, int actResp,
                                         int errorCode) {
        if (activity == null) {
            Log.e("GameHelper", "*** No Activity. Can't show failure dialog!");
            return;
        }
        Dialog errorDialog = null;

        switch (actResp) {
            case GamesActivityResultCodes.RESULT_APP_MISCONFIGURED:
                errorDialog = makeSimpleDialog(activity, GameHelperUtils.getString(
                        activity, GameHelperUtils.R_APP_MISCONFIGURED));
                break;
            case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED:
                errorDialog = makeSimpleDialog(activity, GameHelperUtils.getString(
                        activity, GameHelperUtils.R_SIGN_IN_FAILED));
                break;
            case GamesActivityResultCodes.RESULT_LICENSE_FAILED:
                errorDialog = makeSimpleDialog(activity, GameHelperUtils.getString(
                        activity, GameHelperUtils.R_LICENSE_FAILED));
                break;
            default:
                errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
                        activity, RC_UNUSED, null);
                if (errorDialog == null) {
                    Log.e("GameHelper",
                            "No standard error dialog available. Making fallback dialog.");
                    errorDialog = makeSimpleDialog(
                            activity,
                            GameHelperUtils.getString(activity,
                                    GameHelperUtils.R_UNKNOWN_ERROR)
                                    + " "
                                    + GameHelperUtils.errorCodeToString(errorCode));
                }
        }

        errorDialog.show();
    }

    static Dialog makeSimpleDialog(Activity activity, String text) {
        return (new AlertDialog.Builder(activity)).setMessage(text)
                .setNeutralButton(android.R.string.ok, null).create();
    }

    static Dialog
    makeSimpleDialog(Activity activity, String title, String text) {
        return (new AlertDialog.Builder(activity)).setMessage(text)
                .setTitle(title).setNeutralButton(android.R.string.ok, null)
                .create();
    }

    public Dialog makeSimpleDialog(String text) {
        if (mActivity == null) {
            logError("*** makeSimpleDialog failed: no current Activity!");
            return null;
        }
        return makeSimpleDialog(mActivity, text);
    }

    public Dialog makeSimpleDialog(String title, String text) {
        if (mActivity == null) {
            logError("*** makeSimpleDialog failed: no current Activity!");
            return null;
        }
        return makeSimpleDialog(mActivity, title, text);
    }

    void debugLog(String message) {
        if (mDebugLog) {
            Log.d(TAG, "GameHelper: " + message);
        }
    }

    void logWarn(String message) {
        Log.w(TAG, "!!! GameHelper WARNING: " + message);
    }

    void logError(String message) {
        Log.e(TAG, "*** GameHelper ERROR: " + message);
    }

    public static class SignInFailureReason {
        public static final int NO_ACTIVITY_RESULT_CODE = -100;
        int mServiceErrorCode = 0;
        int mActivityResultCode = NO_ACTIVITY_RESULT_CODE;

        public int getServiceErrorCode() {
            return mServiceErrorCode;
        }

        public int getActivityResultCode() {
            return mActivityResultCode;
        }

        public SignInFailureReason(int serviceErrorCode, int activityResultCode) {
            mServiceErrorCode = serviceErrorCode;
            mActivityResultCode = activityResultCode;
        }

        public SignInFailureReason(int serviceErrorCode) {
            this(serviceErrorCode, NO_ACTIVITY_RESULT_CODE);
        }

        @Override
        public String toString() {
            return "SignInFailureReason(serviceErrorCode:"
                    + GameHelperUtils.errorCodeToString(mServiceErrorCode)
                    + ((mActivityResultCode == NO_ACTIVITY_RESULT_CODE) ? ")"
                    : (",activityResultCode:"
                    + GameHelperUtils
                    .activityResponseCodeToString(mActivityResultCode) + ")"));
        }
    }

    public void setConnectOnStart(boolean connectOnStart) {
        debugLog("Forcing mConnectOnStart=" + connectOnStart);
        mConnectOnStart = connectOnStart;
    }
}