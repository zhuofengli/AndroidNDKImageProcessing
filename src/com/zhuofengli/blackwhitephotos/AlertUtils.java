package com.zhuofengli.blackwhitephotos;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class AlertUtils {
	public static void toastMsg(String msg, Context con){
		Toast toast = Toast.makeText(con.getApplicationContext(), msg, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.TOP, 0, 80);
		toast.show();
	}
}
