package me.isol.matcher;

import android.graphics.Rect;

/**
 * rect object of the raw framebuffer
 * @author zhaohui.sol
 *
 */
public class RectScreen {
	
	public final byte[] raw;
	
	public final Rect rect;
	
	public final int width;
	
	public final int height;
	
	public final boolean rotate;
	
	public RectScreen(byte[] raw,Rect rect,boolean rotate){
		if(rect != null && raw != null){
			if(rect.width() * rect.height() * 4 == raw.length){
				this.raw = raw;
				this.rect = rect;
				this.rotate = rotate;
				this.width = rect.width();
				this.height = rect.height();
			}else{
				throw new RuntimeException("invalidate framebuffer");
			}
		}else{
			throw new RuntimeException("framebuffer can not be null");
		}
	}
}
