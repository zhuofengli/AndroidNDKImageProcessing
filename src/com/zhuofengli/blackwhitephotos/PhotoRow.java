package com.zhuofengli.blackwhitephotos;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

public class PhotoRow {
	private int photoId = -1;
	private Uri photoUri;	
	private Uri thumbUri;	//thumbnail
	private String datetime;
	
	private String photoName;
	
	public int getPhotoId(){
		return photoId;
	}
	public void setPhotoId(int id){
		photoId = id;
	}
	
	public Uri getPhotoUri(){
		return photoUri;
	}
	public void setPhotoUri(Uri uri){
		photoUri = uri;
	}
	
	public Uri getThumbUri(){
		return thumbUri;
	}
	public void setThumbUri(Uri uri){
		thumbUri = uri;
	}
	
	public String getDatetime(){
		return datetime;
	}
	public void setDatetime(String dt){
		datetime = dt;
	}
	
	public String getPhotoName(Context context){
		if(photoName == null) {
			if(photoUri!=null){
				String photoFilePath = ImageUtils.getRealPathFromURI(photoUri, (Activity) context);
				photoName = new File(photoFilePath).getName();
			}
			
		}
		return photoName;
	}

}
