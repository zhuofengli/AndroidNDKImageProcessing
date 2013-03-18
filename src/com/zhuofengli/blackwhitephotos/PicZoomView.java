package com.zhuofengli.blackwhitephotos;


import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;

public class PicZoomView extends Activity{
	private Bitmap mBitmap;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.pic_zoom_view);
		
		Bundle extras = getIntent().getExtras();
		Uri uri = (Uri)extras.get("uri");
		
//		picOrientation = extras.getInt("PicOrientation");
//		if(picOrientation == 1){
//			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//		}
		
		try {
			mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TouchImageView tiv = (TouchImageView) findViewById(R.id.imageview);
		tiv.setImage(mBitmap, PicZoomView.this);

	}

	@Override
	protected void onDestroy(){
		super.onDestroy();

		if(mBitmap!=null )
			mBitmap.recycle();
		mBitmap = null;
		
		System.gc();
	}

}
