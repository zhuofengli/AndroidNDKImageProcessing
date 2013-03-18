package com.zhuofengli.blackwhitephotos;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnScrollListener {
	private ListView photoListView;
	private PhotoListAdapter photoListAdapter;
	private List<PhotoRow> photoItems;

	public static final int SELECT_PICTURE = 0;
	
	private ProgressDialog loadingDialog;
	
	private int page = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		photoListView = (ListView) findViewById(R.id.photo_list_view);
		photoItems = new ArrayList<PhotoRow>();
		photoListAdapter = new PhotoListAdapter(this, photoItems);
		photoListView.setAdapter(photoListAdapter);
		photoListView.setOnScrollListener(this);

		retrieveDataFromDB();

		Button createBt = (Button) findViewById(R.id.create_bt);
		createBt.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(
						Intent.createChooser(intent, getString(R.string.select_picture)), SELECT_PICTURE);
			}
		});
	}

	private void retrieveDataFromDB() {
		SQLiteDatabase db = null;
		try {
			DBUtils dbUtils = new DBUtils(MainActivity.this);
			db = dbUtils.getWritableDatabase();
			List<PhotoRow> items = dbUtils.selectPhotos(db, page);
			
			if(items != null && items.size() > 0){
				for (PhotoRow item : items) {
					photoItems.add(item);
				}
				photoListAdapter.notifyDataSetChanged();
				
				page++;	//increment the page count for next load
			}
			else {
				page = -1;	//there's no more in the database, set page to -1
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (db != null)
				db.close();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public class PhotoListAdapter extends ArrayAdapter<PhotoRow> {
		private LayoutInflater mInflater;
		private Context context;

		public PhotoListAdapter(Activity activity, List<PhotoRow> itemList) {
			super(activity, 0, itemList);
			mInflater = activity.getLayoutInflater();
			context = activity;
		}
		
		public class ViewHolder {
			public TextView nameText;
			public ImageView thumbImage;
			public TextView timeText;
		}
		
		private ViewHolder createViewHolder(View convertView) {
			ViewHolder holder = new ViewHolder();
			holder.nameText = (TextView) convertView
					.findViewById(R.id.photo_name);
			holder.thumbImage = (ImageView) convertView
					.findViewById(R.id.photo_img);
			holder.timeText = (TextView) convertView.findViewById(R.id.photo_datetime);
			
			convertView.setTag(holder);
			return holder;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.photo_list_row, null);
				viewHolder = createViewHolder(convertView);
			}
			else {
				viewHolder = (ViewHolder)convertView.getTag();
			}

			final PhotoRow photoRow = getItem(position);
	
			viewHolder.nameText.setText(photoRow.getPhotoName(context));
			
			ImageUtils.getThumbForPhotoRow(viewHolder.thumbImage, photoRow.getThumbUri(), MainActivity.this);
			
			String time = TimeUtils.getDisplayTime(photoRow.getDatetime(), context);
			viewHolder.timeText.setText(time);
			
			convertView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent().setClass(MainActivity.this, PicZoomView.class);
					intent.putExtra("uri", photoRow.getPhotoUri());
					startActivity(intent);
				}
			});

			return convertView;

		}
	}

	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		
		case SELECT_PICTURE:{
			if(resultCode == Activity.RESULT_OK){
				
				Uri selectedImageUri = data.getData();
//				selectedImageUri = Uri.parse("http://www.doggonecitykitty.com/Dog_Olive.jpg");
				
				if (selectedImageUri != null) {
					loadingDialog = ProgressDialog.show(MainActivity.this, "",
							getString(R.string.processing), true);
					loadingDialog.setCancelable(true);
					String oriImagePath = ImageUtils.getRealPathFromURI(selectedImageUri, MainActivity.this);
					new AsyncPhotoCreator().execute(selectedImageUri.toString(), oriImagePath);
					
				} 
				else {
					
					AlertUtils.toastMsg(
							getString(R.string.select_from_gallery_fail),
							MainActivity.this);
				}
			}
			break;
		}
		default:
			break;
		}
	}
	private class AsyncPhotoCreator extends AsyncTask<String,Void,String>{
		@Override
		protected String doInBackground(String... params) {
			
			List<Uri> photoUris = new ArrayList<Uri>();

			String errMsg = ImageUtils.processImage(params[0], params[1], MainActivity.this, photoUris);
			
			if(errMsg == null && photoUris.size()>=2) {
				PhotoRow photoRow = new PhotoRow();
				photoRow.setPhotoUri(photoUris.get(0));
				photoRow.setThumbUri(photoUris.get(1));
				String datetime = TimeUtils.getCurrentDateTime();
				photoRow.setDatetime(datetime);
				
				saveToDB(photoRow);
				
				photoItems.add(0, photoRow);
				
				return null;
			}
			else {
				return "\n"+errMsg;
			}
		}
		
		@Override
		protected void onPostExecute(String errMsg) {
			loadingDialog.dismiss();
			
			if(errMsg == null){
				photoListAdapter.notifyDataSetChanged();
				AlertUtils.toastMsg(getString(R.string.image_process_complete), MainActivity.this);
				photoListView.setSelection(0);
			}
			else{
				AlertUtils.toastMsg(getString(R.string.image_process_failed)+errMsg, MainActivity.this);
			}
		}
	}
	
	private void saveToDB(PhotoRow pr){
		SQLiteDatabase db = null;
		try {
			DBUtils dbUtils = new DBUtils(MainActivity.this);
			db = dbUtils.getWritableDatabase();

			dbUtils.insertPhoto(db, pr);

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (db != null)
				db.close();
		}
	}

	@Override
	public void onScroll(AbsListView arg0, int firstVisible, int visibleCount, int totalCount) {

		//if reaches the bottom of the listview, load more from DB
        if(page > 0 && (firstVisible + visibleCount) >= totalCount) {
        	retrieveDataFromDB();
        }
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		
	}

}
