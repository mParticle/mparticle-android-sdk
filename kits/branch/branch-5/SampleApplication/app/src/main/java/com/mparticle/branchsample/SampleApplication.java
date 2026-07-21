package com.mparticle.branchsample;

import android.app.Application;

import com.mparticle.AttributionError;
import com.mparticle.AttributionListener;
import com.mparticle.AttributionResult;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;

import org.json.JSONObject;

import io.branch.referral.Branch;

public class SampleApplication extends Application implements AttributionListener {
    private IBranchEvents branchEventCallback;
    private static SampleApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        MParticleOptions options = MParticleOptions.builder(this)
                .credentials("REPLACE_ME_WITH_API_KEY", "REPLACE_ME_WITH_API_SECRET")
                .attributionListener(this)
                //Set an instance of AttributionListener
                //this instance will continually be called whenever
                //there is new attribution/deep linking information.
                //It is essentially equivalent to the Branch SDK's BranchReferralInitListener class
                //that is passed to the Branch SDK's initSession() API.
                .logLevel(MParticle.LogLevel.VERBOSE)
                .build();

        MParticle.start(options);
    }

    public static void setBranchEventCallback(IBranchEvents branchEventCallback) {
        instance.branchEventCallback = branchEventCallback;
    }


    /**
     * A few typical scenarios where this callback would be invoked:
     * <p>
     * (1) Base case:
     * - User does not tap on a link, and then opens the app (either after a fresh install or not)
     * - This block will be invoked with Branch Metrics' response indicating that this user did not tap on a link.
     * <p>
     * (2) Deferred deep link:
     * - User without the app installed taps on a link
     * - User is redirected from Branch Metrics to the App Store and installs the app
     * - User opens the app
     * - This block will be invoked with Branch Metrics' response containing the details of the link
     * <p>
     * (3) Deep link with app installed:
     * - User with the app already installed taps on a link
     * - Application opens directly to an Activity via a link click, mParticle forwards the launch URI etc to Branch
     * - This callback will be invoked with Branch Metrics' response containing the details of the link
     * <p>
     * If the user navigates away from the app without killing it, this callback could be invoked several times:
     * once for the initial launch, and then again each time the user taps on a link to re-open the app.
     *
     * <p>
     *     This method is equivalent to {@link io.branch.referral.Branch#initSession(Branch.BranchReferralInitListener)}.
     *     This will return deep links params if the app is opened by a link click otherwise a JSONObject with ""+clicked_branch_link = false".
     *     The below example shows how you can message this to your Activities listening for Branch deep link params.
     * </p>
     *
     * @param attributionResult
     **/
    @Override
    public void onResult(AttributionResult attributionResult) {
        //this will be invoked for
        if (attributionResult.getServiceProviderId() == MParticle.ServiceProviders.BRANCH_METRICS) {
            JSONObject params = attributionResult.getParameters();
            // The Branch SDK will return a response for all new session/app opens, even
            // if the user did not click a branch link.
            // The parameters supported by the Branch SDK are documented here:
            // https://github.com/BranchMetrics/android-branch-deep-linking#branch-provided-data-parameters-in-initsession-callback
            // For eg:
//            if (params.optBoolean("+clicked_branch_link", false)) {
//                //handle the Branch link click
//            }
            if (branchEventCallback != null) {
                branchEventCallback.onBranchInitialised(params);
            }
        }
    }

    @Override
    public void onError(AttributionError attributionError) {
        //if the Branch SDK returns an error, it will be surfaced here.
        if (branchEventCallback != null) {
            branchEventCallback.onBranchInitialised(new JSONObject());
        }
    }

    public interface IBranchEvents {
        void onBranchInitialised(JSONObject params);
    }
}
