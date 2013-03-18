package com.zhuofengli.blackwhitephotos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class TouchImageView extends ImageView {
	// These matrices will be used to move and zoom image
	Matrix matrix;
	Matrix savedMatrix;

	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	private PointF start = new PointF();
	private PointF mid = new PointF();
	private float oldDist = 1f;
	private int imageWidth;
	private int imageHeight;
	private int displayWidth;
	private int displayHeight;
	
	public TouchImageView(Context context, AttributeSet attrSet) {
		super(context, attrSet);
		super.setClickable(true);

		setScaleType(ScaleType.MATRIX);
		
		setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent rawEvent) {
				WrapMotionEvent event = WrapMotionEvent.wrap(rawEvent);

				// Handle touch events here...
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					savedMatrix.set(matrix);
					start.set(event.getX(), event.getY());
					mode = DRAG;
					break;
				case MotionEvent.ACTION_POINTER_DOWN:
					oldDist = spacing(event);
					if (oldDist > 10f) {
						savedMatrix.set(matrix);
						midPoint(mid, event);
						mode = ZOOM;
					}
					break;
				case MotionEvent.ACTION_UP:
					int xDiff = (int) Math.abs(event.getX() - start.x);
					int yDiff = (int) Math.abs(event.getY() - start.y);
					if (xDiff < 8 && yDiff < 8) {
						performClick();
					}
				case MotionEvent.ACTION_POINTER_UP:
					mode = NONE;
					break;
				case MotionEvent.ACTION_MOVE:
					if (mode == DRAG) {
						matrix.set(savedMatrix);
						matrix.postTranslate(event.getX() - start.x,
								event.getY() - start.y);
					} else if (mode == ZOOM) {
						float newDist = spacing(event);
						if (newDist > 10f) {
							matrix.set(savedMatrix);
							float scale = newDist / oldDist;
							float[] values = new float[9];
							matrix.getValues(values);
							
							//min width = screen width, max width = 3* screen width
							float currentWidth = 0f;
							currentWidth = values[0] * imageWidth;
							
							if (scale < 1.0f && currentWidth * scale < displayWidth) 
								scale = displayWidth / currentWidth;
							else if (scale > 1.0f && currentWidth * scale >= displayWidth * 3)
								scale = displayWidth * 3 / currentWidth;
							
							matrix.postScale(scale, scale, mid.x, mid.y);
						}
					}
					break;
				}

				float[] values = new float[9];
				matrix.getValues(values);
				float x = values[2];
				float y = values[5];
				float width = values[0] * imageWidth;
				float height = values[4] * imageHeight;
				
				if (width >= displayWidth) {
					if (x > 0)
						values[2] = 0;
					else if (x < displayWidth - width)
						values[2] = displayWidth - width;
				} else {
					values[2] = (displayWidth - width) / 2;
				}
				
				if (height < displayHeight) {
					values[5] = (displayHeight - height) / 2;
				} else {
					if (y > 0)
						values[5] = 0;
					else if (y < displayHeight - height)
						values[5] = displayHeight - height;
				}
					
				matrix.setValues(values);
				setImageMatrix(matrix);
				invalidate();
				
				return true; // indicate event was handled
			}

		});
	}

	public void setImage(Bitmap bm, Context context) {

//		super.setImageURI(uri);		//set image from uri causes bug when displaying the image
		
		//get image width and height
//		String imageFilePath = ImageUtils.getRealPathFromURI(uri, (Activity) context);
//		BitmapFactory.Options options = new BitmapFactory.Options();
//		options.inJustDecodeBounds = true;
//		BitmapFactory.decodeFile(imageFilePath, options);
//
//		imageWidth = options.outWidth;
//		imageHeight = options.outHeight;
		
		
		super.setImageBitmap(bm);
		
		//get image width and height
		imageWidth = bm.getWidth();
		imageHeight = bm.getHeight();
		
		//get screen width and height
		WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE); 
		displayWidth = window.getDefaultDisplay().getWidth();
		displayHeight = window.getDefaultDisplay().getHeight();

		
		//get the correct matrix
		matrix = getImageMatrix();
		RectF drawableRect = new RectF(0, 0, imageWidth, imageHeight);
		RectF viewRect = new RectF(0, 0, displayWidth, displayHeight);
		matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
		setImageMatrix(matrix);
		
		savedMatrix = new Matrix();
		savedMatrix.set(matrix);

	}

	/** Determine the space between the first two fingers */
	private float spacing(WrapMotionEvent event) {
		// ...
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/** Calculate the mid point of the first two fingers */
	private void midPoint(PointF point, WrapMotionEvent event) {
		// ...
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}
}

class WrapMotionEvent {
	protected MotionEvent event;

	protected WrapMotionEvent(MotionEvent event) {
		this.event = event;
	}

	public static WrapMotionEvent wrap(MotionEvent event) {
		try {
			return new EclairMotionEvent(event);
		} catch (VerifyError e) {
			return new WrapMotionEvent(event);
		}
	}

	public int getAction() {
		return event.getAction();
	}

	public float getX() {
		return event.getX();
	}

	public float getX(int pointerIndex) {
		verifyPointerIndex(pointerIndex);
		return getX();
	}

	public float getY() {
		return event.getY();
	}

	public float getY(int pointerIndex) {
		verifyPointerIndex(pointerIndex);
		return getY();
	}

	public int getPointerCount() {
		return 1;
	}

	public int getPointerId(int pointerIndex) {
		verifyPointerIndex(pointerIndex);
		return 0;
	}

	private void verifyPointerIndex(int pointerIndex) {
		if (pointerIndex > 0) {
			throw new IllegalArgumentException(
					"Invalid pointer index for Donut/Cupcake");
		}
	}

}

class EclairMotionEvent extends WrapMotionEvent {

	protected EclairMotionEvent(MotionEvent event) {
		super(event);
	}

	public float getX(int pointerIndex) {
		return event.getX(pointerIndex);
	}

	public float getY(int pointerIndex) {
		return event.getY(pointerIndex);
	}

	public int getPointerCount() {
		return event.getPointerCount();
	}

	public int getPointerId(int pointerIndex) {
		return event.getPointerId(pointerIndex);
	}
	
}
