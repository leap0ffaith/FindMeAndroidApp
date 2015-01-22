package com.asif.test.sendsmsfromapptest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

public class ContactListDialogFragment extends DialogFragment
{
	public interface ContactListDialogListener {
		public void onDialogItemClick(DialogFragment dialog, String name, String number);
	}
	
	ContactListDialogListener mListener;
	
	 @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ContactListDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
	 
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final String[] contactNames = {"Red","Black","Green"};
		builder.setTitle("Select From Contacts")
			.setItems(contactNames, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Toast.makeText(getActivity(), contactNames[which], Toast.LENGTH_SHORT).show();
					
				}
			});
		// Create the AlertDialog object and return it
        return builder.create();
	}
}
