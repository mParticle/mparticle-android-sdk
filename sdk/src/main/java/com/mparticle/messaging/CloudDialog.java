package com.mparticle.messaging;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;

import com.mparticle.MParticlePushUtility;

/**
 * A basic DialogFragment designed to show an in-app push message to the user. The mParticle
 * SDK will automatically show this for push messages that are configured for it.
 *
 * This dialog is themeable via the mParticle push generation console. By default it will use the
 * default AlertDialog style as configured in the given Activity context.
 *
 */
public class CloudDialog extends DialogFragment implements DialogInterface.OnClickListener{

    private static final String DARK_THEME = "mp.dark";
    private static final String LIGHT_THEME = "mp.light";

    /**
     * The mParticle SDK will use this tag when adding this DialogFragment in a FragmentTransaction
     */
    public static String TAG = "mp_dialog";
    private DialogInterface.OnClickListener mListener;

    public static CloudDialog newInstance(MPCloudNotificationMessage message) {
        CloudDialog frag = new CloudDialog();
        Bundle args = new Bundle();
        args.putParcelable(MessagingUtils.CLOUD_MESSAGE_EXTRA, message);
        frag.setArguments(args);
        return frag;
    }

    public CloudDialog() {
        super();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MPCloudNotificationMessage message = getArguments().getParcelable(MessagingUtils.CLOUD_MESSAGE_EXTRA);

        int iconId = android.R.drawable.ic_dialog_alert;
        try {
            iconId = getActivity().getPackageManager().getApplicationInfo(getActivity().getPackageName(), 0).icon;
        } catch (PackageManager.NameNotFoundException e) {
            // use the ic_dialog_alert icon if the app's can not be found
        }

        String theme = message.getInAppTheme();
        int themeId = 0;
        if (theme != null && !theme.equals(DARK_THEME) && !theme.equals(LIGHT_THEME)){
            try {
                themeId = getResources().getIdentifier(theme, "style", getActivity().getPackageName());
            }catch (Exception e){

            }
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (DARK_THEME.equals(theme)) {
                themeId = AlertDialog.THEME_HOLO_DARK;
            } else {
                themeId = AlertDialog.THEME_HOLO_LIGHT;
            }
        }

        AlertDialog.Builder dialog;

        if (themeId > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                dialog = new AlertDialog.Builder(getActivity(), themeId);
            } else {
                ContextThemeWrapper ctw = new ContextThemeWrapper( getActivity(), themeId );
                dialog = new AlertDialog.Builder(ctw);
            }
        }else{
            dialog = new AlertDialog.Builder(getActivity());
        }

        dialog.setIcon(iconId);
        dialog.setTitle(message.getContentTitle(getActivity()));

        String primary = message.getPrimaryMessage(getActivity());
        String bigText = message.getBigText();
        if (bigText != null){
            dialog.setMessage(bigText);
        }else{
            dialog.setMessage(primary);
        }

        dialog.setPositiveButton("OK", this);

        return dialog.create();
    }

    /**
     * Use this to set your own listener for when the CloudDialog is dismissed.
     *
     * @param listener
     */
    public void setOnClickListener(DialogInterface.OnClickListener listener){
        mListener = listener;
    }

    @Override
    public final void onClick(DialogInterface dialog, int which) {
        if (mListener != null){
            mListener.onClick(dialog, which);
        }

        dialog.dismiss();
    }
}
