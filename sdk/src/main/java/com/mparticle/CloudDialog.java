package com.mparticle;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by sdozor on 10/17/14.
 */
public class CloudDialog extends DialogFragment {
    public static CloudDialog newInstance(MPCloudMessage message) {
        CloudDialog frag = new CloudDialog();
        Bundle args = new Bundle();
        args.putParcelable(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MPCloudMessage message = getArguments().getParcelable(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
                .setIcon(message.getSmallIconResourceId(getActivity()))
                .setTitle(message.getContentTitle(getActivity()));

        String primary = message.getPrimaryText(getActivity());
        String bigText = message.getBigText();
        if (bigText != null){
            dialog.setMessage(bigText);
        }else{
            dialog.setMessage(primary);
        }

        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (getActivity() instanceof DialogInterface.OnClickListener){
                    ((DialogInterface.OnClickListener)getActivity()).onClick(dialog, which);
                }
                dialog.dismiss();
            }
        });

        return dialog.create();
    }
}
