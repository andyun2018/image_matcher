package me.isol.matcher;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.android.uiautomator.core.UiDevice;
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;

/**
 * screen rgba buffer
 * @author zhaohui.sol
 *
 */
public class RawScreen {
	
	private byte[] raws;
	
	public final int width;
	
	public final int height;
	
	public final boolean rotate;
	
	public RawScreen(byte[] raw){
		this.width =  ((raw[1]&0xff)<<8) | (raw[0]&0xff);
		this.height = ((raw[5]&0xff)<<8) | (raw[4]&0xff);
		if(raw != null && raw.length == width * height * 4 + 12){
			this.raws = raw;
			this.rotate = UiDevice.getInstance().getDisplayRotation() != 0;
		}else{
			throw new RuntimeException("framebuffer data can not validate.");
		}
	}
	
	/**
	 * raw screencap buffer
	 * @return
	 */
	public byte[] raw(){
		return raws;
	}
	
	/**
	 * raw screen rgba buffer
	 * @return
	 */
	public byte[] rgba(){
		return Arrays.copyOfRange(raws, 12, raws.length);
	}
	
	/**
	 * generate a bitmap of current framebuffer
	 * @return
	 */
	public Bitmap bitmap(){
		Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		bmp.copyPixelsFromBuffer(ByteBuffer.wrap(Arrays.copyOfRange(this.raws,12,this.raws.length)));
		return bmp;
	}
	
	/**
	 * generate a scaled bitmap of current framebuffer
	 * @param scale float
	 * @return
	 */
	public Bitmap bitmap(float scale){
		Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		bmp.copyPixelsFromBuffer(ByteBuffer.wrap(Arrays.copyOfRange(this.raws,12,this.raws.length)));
		if(scale > 0){
			Matrix matrix = new Matrix();
			matrix.postScale(scale,scale);
			bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix,true);
		}
		return bmp;		
	}
	
	/**
	 * rect this framebuffer
	 * @param rect the rect object
	 * @return
	 */
	public Bitmap rectBitmap(Rect rect){
		rect = transform(rect);
		int r_width = rect.right - rect.left;
		int r_height = rect.bottom - rect.top;
		ByteBuffer buffer = ByteBuffer.allocate(r_width * r_height * 4);
		for(int y = rect.top;y < rect.top + r_height;y++){
			byte[] row = Arrays.copyOfRange(raws, 4 * (y * width + rect.left) + 12, 4 * (y * width + rect.left) + 12 + r_width * 4);
			buffer.put(row);
		}
		buffer.flip();
		Bitmap bmp = Bitmap.createBitmap(r_width, r_height, Config.ARGB_8888);
		bmp.copyPixelsFromBuffer(buffer);
		return bmp;		
	}
	
	/**
	 * rect raw framebuffer
	 * @param rect the rect object
	 * @return
	 */
	public RectScreen rectScreen(Rect rect){
		rect = transform(rect);
		int r_width = rect.right - rect.left;
		int r_height = rect.bottom - rect.top;
		ByteBuffer buffer = ByteBuffer.allocate(r_width * r_height * 4);
		for(int y = rect.top;y < rect.top + r_height;y++){
			byte[] row = Arrays.copyOfRange(raws, 4 * (y * width + rect.left) + 12, 4 * (y * width + rect.left) + 12 + r_width * 4);
			buffer.put(row);
		}
		return new RectScreen(buffer.array(), rect, rotate);		
	}

	/**
	 * transform a Rect object based on screen rotate
	 * @param rect
	 * @return
     */
	public Rect transform(Rect rect){
		if(rotate){
			rect = new Rect(width - (rect.top + rect.height()), rect.left, width - rect.top, rect.left + rect.width());
		}
		return rect;
	}
	
	
	
	/**
	 * uiobject bitmap
	 * @param o UIobject
	 * @return
	 */
	public Bitmap elementBitmap(UiObject o){
		if(o != null && o.exists()){
			try {
				return rectBitmap(o.getBounds());				
			} catch (UiObjectNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * rectscreen a uiobject
	 * @param o UIobject
	 * @return
	 */
	public RectScreen elementScreen(UiObject o){
		if(o != null && o.exists()){
			try {
				return rectScreen(o.getBounds());				
			} catch (UiObjectNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	

}
