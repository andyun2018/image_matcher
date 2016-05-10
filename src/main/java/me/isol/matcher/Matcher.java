package me.isol.matcher;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import com.android.uiautomator.core.UiDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

/**
 * the main image matcher helper class
 * it use image compare to find the location of a sample image on the screen
 *
 * @author zhaohui
 *
 */
public class Matcher {
	
	private static int RGB_RANGE = 10;
	
	/**
	 * 从屏幕上查找一个元素并返回坐标
	 * @param resourcePath 相对于jar包的资源路径
	 * @return 匹配到的坐标点，若没有匹配则返回null
	 */
	public static List<MatchPoint> findObject(String resourcePath){
		RawScreen raw = ScreenUtil.rawScreenshot();
		Bitmap pattern = BitmapFactory.decodeStream(Matcher.class.getClassLoader().getResourceAsStream(resourcePath));
		ByteBuffer pbuffer = ByteBuffer.allocate(pattern.getWidth() * pattern.getHeight() * 4);
		if(raw.rotate){
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			pattern = Bitmap.createBitmap(pattern, 0, 0, pattern.getWidth() , pattern.getHeight(),matrix,true);
		}
		pattern.copyPixelsToBuffer(pbuffer);
		byte[] patternBuffer = pbuffer.array();
		byte[] screenBuffer = raw.rgba();
		int sw = raw.width;
		int sh = raw.height;
		int pw = pattern.getWidth();
		int ph = pattern.getHeight();
		int match_x = 0,match_y = 0,x = 0,y = 0;
		int[][] keys = new int[][]{{0,0,0xff,0xff,0xff},{1,0,0,0,0}};
		for(int ky = 0;ky<ph;ky++){
			for(int kx = 0;kx<pw;kx++){
				int r = patternBuffer[(ky * pw + kx) * 4] & 0xff;
				int g = patternBuffer[(ky * pw + kx) * 4 + 1] & 0xff;
				int b = patternBuffer[(ky * pw + kx) * 4 + 2] & 0xff;
				int a = patternBuffer[(ky * pw + kx) * 4 + 3] & 0xff;
				if(a == 255){
					if(r < keys[0][2] && g < keys[0][3] && b < keys[0][4]){
						keys[0][0] = kx;keys[0][1] = ky;keys[0][2] = r;keys[0][3] = g;keys[0][4] = b;
					}
					if(r > keys[1][2] && g > keys[1][3] && b > keys[1][4]){
						keys[1][0] = kx;keys[1][1] = ky;keys[1][2] = r;keys[1][3] = g;keys[1][4] = b;
					}
				}
			}
		}
		
		try{		
			while(y < sh){
				//获取屏幕当前像素
				//本行的剩余宽度和剩余高度满足匹配剩余
				boolean w_avail = (sw - x >= (pw - match_x));
				boolean h_avail = (sh - y >= (ph - match_y));
				boolean p_skip = (x - match_x + pw < sw);
				if(w_avail && h_avail){

					int sr = screenBuffer[(sw * y + x) * 4] & 0xff;
					int sg = screenBuffer[(sw * y + x) * 4 + 1] & 0xff;
					int sb = screenBuffer[(sw * y + x) * 4 + 2] & 0xff;				
					
					int pr = patternBuffer[(pw * match_y + match_x) * 4] & 0xff;
					int pg = patternBuffer[(pw * match_y + match_x) * 4 + 1] & 0xff;
					int pb = patternBuffer[(pw * match_y + match_x) * 4 + 2] & 0xff;
					int pa = patternBuffer[(pw * match_y + match_x) * 4 + 3] & 0xff;
					boolean equals = false;
					if(pa < 255){
						equals = true;
					}else{
						equals = (sr - pr <= RGB_RANGE && sr - pr >= -RGB_RANGE) && (sg - pg <= RGB_RANGE && sg - pg >= -RGB_RANGE) && (sb - pb <= RGB_RANGE && sb - pb >= -RGB_RANGE);
					}
					if(equals){
						//检查匹配图的当前像素是否为横向的匹配边界
						if(match_x == pw - 1){
							//检查匹配图的当前像素是否为纵向的匹配边界
							if(match_y == ph - 1){
								//找到匹配
								MatchPoint match = null;
								if(raw.rotate){
									Point p = new Point(sh - (y - ph / 2), sw - (x - pw / 2));
									match = new MatchPoint(p, sh - y, sw - x, ph, pw);
								}else{
	                                match = new MatchPoint(new Point(x - pw / 2, y - ph / 2),x,y,pw,ph);
								}
								return Arrays.asList(match);
							}else{
								//不为纵向匹配边界则匹配像素横向置零，纵向加一，屏幕横向还原至匹配起始，纵向加一，重新开始当前循环
								match_x = 0;
								match_y += 1;
								x = x - pw + 1;
								y += 1;
							}							
						}else{
							//非横向边界，匹配图横向匹配加一
							if(match_x == match_y && match_x == 0){
								int lwr = screenBuffer[((y + keys[0][1]) * sw + x + keys[0][0]) * 4] & 0xff;
								int lwg = screenBuffer[((y + keys[0][1]) * sw + x + keys[0][0]) * 4 + 1] & 0xff;
								int lwb = screenBuffer[((y + keys[0][1]) * sw + x + keys[0][0]) * 4 + 2] & 0xff;
								
								int upr = screenBuffer[((y + keys[1][1]) * sw + x + keys[1][0]) * 4] & 0xff;
								int upg = screenBuffer[((y + keys[1][1]) * sw + x + keys[1][0]) * 4 + 1] & 0xff;
								int upb = screenBuffer[((y + keys[1][1]) * sw + x + keys[1][0]) * 4 + 2] & 0xff;
								
								boolean found = (lwr - keys[0][2] <= RGB_RANGE && lwr - keys[0][2] >= -RGB_RANGE) && (lwg - keys[0][3] <= RGB_RANGE && lwg - keys[0][3] >= -RGB_RANGE) && (lwb - keys[0][4] <= RGB_RANGE && lwb - keys[0][4] >= -RGB_RANGE);
								if(found){
									if((upr - keys[1][2] <= RGB_RANGE && upr - keys[1][2] >= -RGB_RANGE) && (upg - keys[1][3] <= RGB_RANGE && upg - keys[1][3] >= -RGB_RANGE) && (upb - keys[1][4] <= RGB_RANGE && upb - keys[1][4] >= -RGB_RANGE)){
										
									}else{
										x = x + 1;
										match_x = match_y = 0;
										continue;
									}
								}else{
									x = x + 1;
									match_x = match_y = 0;
									continue;
								}
							}
							x += 1;
							match_x += 1;
						}

					}else{
						//检查匹配起始点 + pw + 1元素是否在匹配图第一行中						
						if(p_skip){
							int nr = screenBuffer[(sw * (y - match_y) + x - match_x + pw) * 4] & 0xff;
							int ng = screenBuffer[(sw * (y - match_y) + x - match_x + pw) * 4 + 1] & 0xff;
							int nb = screenBuffer[(sw * (y - match_y) + x - match_x + pw) * 4 + 2] & 0xff;
							boolean found = false;
							for(int l = pw -1;l >= 0;l--){
								int r = patternBuffer[l * 4] & 0xff;
								int g = patternBuffer[l * 4 + 1] & 0xff;
								int b = patternBuffer[l * 4 + 2] & 0xff;
								int a = patternBuffer[l * 4 + 3] & 0xff;
								if(a < 255){
									found = true;
								}else{
									found = (nr - r <= RGB_RANGE && nr - r >= -RGB_RANGE) && (ng - g <= RGB_RANGE && ng - g >= -RGB_RANGE) && (nb - b <= RGB_RANGE && nb - b >= -RGB_RANGE);
								}
								if(found){
									found = true;
									x = x - match_x + pw - l;
									y = y - match_y;
									match_x = match_y = 0;
									break;
								}
							}
							if(!found){
								x = x - match_x + pw + 1;
								y = y - match_y;
								match_x = match_y = 0;
							}
						}else{
							x = 0;
							y = y - match_y + 1;
							match_x = match_y = 0;
						}
					}
				}else{					
					//剩余宽度或高度不满足
					//若宽度不满足
					if(h_avail &&(!w_avail)){
						x = 0;
						y = y - match_y + 1;
						match_x = match_y = 0;
					}else{
						break;
					}						
				}					
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	

	/**
	 * 从屏幕上查找一个元素并返回坐标，此方法先对屏幕和样本图做256介灰度转换，也即第一步将颜色空间压缩到256种，第二步根据windows做二值化处理
	 * @param resourcePath 相对于jar包的资源路径
	 * @param windows RGB二值化边界
	 * @return 匹配到的坐标点，若没有匹配则返回null
	 */
	public static List<MatchPoint> findObject(String resourcePath,int[] windows){
		RawScreen raw = ScreenUtil.rawScreenshot();
		Bitmap patternImage = BitmapFactory.decodeStream(Matcher.class.getClassLoader().getResourceAsStream(resourcePath));
		ByteBuffer pbuffer = ByteBuffer.allocate(patternImage.getWidth() * patternImage.getHeight() * 4);
		if(raw.rotate){
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			patternImage = Bitmap.createBitmap(patternImage, 0, 0, patternImage.getWidth() , patternImage.getHeight(),matrix,true);
		}
		patternImage.copyPixelsToBuffer(pbuffer);
		byte[] pattern = pbuffer.array();
		byte[] screen = raw.rgba();
		int sw = raw.width;
		int sh = raw.height;
		int pw = patternImage.getWidth();
		int ph = patternImage.getHeight();
		int sm = 0,pm = 0,begin = 0;
		try{
			while(sm < sw * sh){
				int sr = screen[sm * 4] & 0xff;
				int sg = screen[sm * 4 + 1] & 0xff;
				int sb = screen[sm * 4 + 2] & 0xff;		
				int pr = pattern[pm * 4] & 0xff;
				int pg = pattern[pm * 4 + 1] & 0xff;
				int pb = pattern[pm * 4 + 2] & 0xff;
				int s_gray = (int)((sr * 77.0 + sg * 151.0 + sb * 28.0) / 256.0);
				int p_gray = (int)((pr * 77.0 + pg * 151.0 + pb * 28.0) / 256.0);
				s_gray = (s_gray <= windows[1] && s_gray >= windows[0] ? 255:0);
				p_gray = (p_gray <= windows[1] && p_gray >= windows[0] ? 255:0);
				if(s_gray - p_gray == 0){
					if((pm + 1) % pw == 0){
						//匹配边界
						//检查是否匹配完成
						if(pm + 1 == pw * ph){
							//匹配完成
							int x = (sm + 1) % sw == 0 ? 0:(sm + 1) - (sm + 1) / sw * sw;
							int y = (sm + 1) % sw == 0 ? (sm + 1) / sw - ph / 2:(sm + 1) / sw + 1;
							MatchPoint match = null;
							if(raw.rotate){
								Point p = new Point(sh - (y - 1 - ph / 2), sw - (x - 1 - pw / 2));
								match = new MatchPoint(p, sh - y - 1, sw - x - 1, ph, pw);
							}else{
                                match = new MatchPoint(new Point(x - 1 - pw / 2, y - 1 - ph / 2),x,y,pw,ph);
							}
							return Arrays.asList(match);
						}
						//检查屏幕剩余像素是否满足继续匹配
						if(ph - (pm + 1) / pw <= sh - Math.ceil((sm + 1) / sw)){
							sm = sm + sw - pw + 1;
							pm += 1;
						}else{
							//纵向不够匹配，活脱脱的没找到
							return null;
						}
					}else{
						//非匹配边界
						if(pm == 0){
							begin = sm;
						}
						sm += 1;
						pm += 1;
					}
				}else{
					//回滚匹配到起始位置加一
					if(pm == 0){
						sm += 1;
					}else{
						sm = begin + 1;
					}					
					pm = 0;
				}				
				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * track a rect object location
	 * @see RectScreen
	 * @param rs
	 * @return
	 */
	public static List<MatchPoint> findObject(RectScreen rs){
		RawScreen raw = ScreenUtil.rawScreenshot();
		byte[] patternBuffer = rs.raw;
		byte[] screenBuffer = raw.rgba();
		int sw = raw.width;
		int sh = raw.height;
		int pw = rs.width;
		int ph = rs.height;
		int match_x = 0,match_y = 0,x = 0,y = 0;
		int[][] keys = new int[][]{{0,0,0xff,0xff,0xff},{1,0,0,0,0}};
		for(int ky = 0;ky<ph;ky++){
			for(int kx = 0;kx<pw;kx++){
				int r = patternBuffer[(ky * pw + kx) * 4] & 0xff;
				int g = patternBuffer[(ky * pw + kx) * 4 + 1] & 0xff;
				int b = patternBuffer[(ky * pw + kx) * 4 + 2] & 0xff;
				int a = patternBuffer[(ky * pw + kx) * 4 + 3] & 0xff;
				if(a == 255){
					if(r < keys[0][2] && g < keys[0][3] && b < keys[0][4]){
						keys[0][0] = kx;keys[0][1] = ky;keys[0][2] = r;keys[0][3] = g;keys[0][4] = b;
					}
					if(r > keys[1][2] && g > keys[1][3] && b > keys[1][4]){
						keys[1][0] = kx;keys[1][1] = ky;keys[1][2] = r;keys[1][3] = g;keys[1][4] = b;
					}
				}
			}
		}
		
		try{		
			while(y < sh){
				//获取屏幕当前像素
				//本行的剩余宽度和剩余高度满足匹配剩余
				boolean w_avail = (sw - x >= (pw - match_x));
				boolean h_avail = (sh - y >= (ph - match_y));
				boolean p_skip = (x - match_x + pw < sw);
				if(w_avail && h_avail){

					int sr = screenBuffer[(sw * y + x) * 4] & 0xff;
					int sg = screenBuffer[(sw * y + x) * 4 + 1] & 0xff;
					int sb = screenBuffer[(sw * y + x) * 4 + 2] & 0xff;					
					
					int pr = patternBuffer[(pw * match_y + match_x) * 4] & 0xff;
					int pg = patternBuffer[(pw * match_y + match_x) * 4 + 1] & 0xff;
					int pb = patternBuffer[(pw * match_y + match_x) * 4 + 2] & 0xff;
					int pa = patternBuffer[(pw * match_y + match_x) * 4 + 3] & 0xff;
					boolean equals = false;
					if(pa < 255){
						equals = true;
					}else{
						equals = (sr - pr <= RGB_RANGE && sr - pr >= -RGB_RANGE) && (sg - pg <= RGB_RANGE && sg - pg >= -RGB_RANGE) && (sb - pb <= RGB_RANGE && sb - pb >= -RGB_RANGE);
					}
					if(equals){
						//检查匹配图的当前像素是否为横向的匹配边界
						if(match_x == pw - 1){
							//检查匹配图的当前像素是否为纵向的匹配边界
							if(match_y == ph - 1){
								//找到匹配
								return Arrays.asList(new MatchPoint(new Point(x - pw / 2, y - ph / 2),x,y,pw,ph));
							}else{
								//不为纵向匹配边界则匹配像素横向置零，纵向加一，屏幕横向还原至匹配起始，纵向加一，重新开始当前循环
								match_x = 0;
								match_y += 1;
								x = x - pw + 1;
								y += 1;
							}							
						}else{
							//非横向边界，匹配图横向匹配加一
							if(match_x == match_y && match_x == 0){
								int lwr = screenBuffer[((y + keys[0][1]) * sw + x + keys[0][0]) * 4] & 0xff;
								int lwg = screenBuffer[((y + keys[0][1]) * sw + x + keys[0][0]) * 4 + 1] & 0xff;
								int lwb = screenBuffer[((y + keys[0][1]) * sw + x + keys[0][0]) * 4 + 2] & 0xff;
								
								int upr = screenBuffer[((y + keys[1][1]) * sw + x + keys[1][0]) * 4] & 0xff;
								int upg = screenBuffer[((y + keys[1][1]) * sw + x + keys[1][0]) * 4 + 1] & 0xff;
								int upb = screenBuffer[((y + keys[1][1]) * sw + x + keys[1][0]) * 4 + 2] & 0xff;
								
								boolean found = (lwr - keys[0][2] <= RGB_RANGE && lwr - keys[0][2] >= -RGB_RANGE) && (lwg - keys[0][3] <= RGB_RANGE && lwg - keys[0][3] >= -RGB_RANGE) && (lwb - keys[0][4] <= RGB_RANGE && lwb - keys[0][4] >= -RGB_RANGE);
								if(found){
									if((upr - keys[1][2] <= RGB_RANGE && upr - keys[1][2] >= -RGB_RANGE) && (upg - keys[1][3] <= RGB_RANGE && upg - keys[1][3] >= -RGB_RANGE) && (upb - keys[1][4] <= RGB_RANGE && upb - keys[1][4] >= -RGB_RANGE)){
										
									}else{
										x = x + 1;
										match_x = match_y = 0;
										continue;
									}
								}else{
									x = x + 1;
									match_x = match_y = 0;
									continue;
								}
							}
							x += 1;
							match_x += 1;
						}

					}else{
						//检查匹配起始点 + pw + 1元素是否在匹配图第一行中						
						if(p_skip){
							int nr = screenBuffer[(sw * (y - match_y) + x - match_x + pw) * 4] & 0xff;
							int ng = screenBuffer[(sw * (y - match_y) + x - match_x + pw) * 4 + 1] & 0xff;
							int nb = screenBuffer[(sw * (y - match_y) + x - match_x + pw) * 4 + 2] & 0xff;
							boolean found = false;
							for(int l = pw -1;l >= 0;l--){
								int r = patternBuffer[l * 4] & 0xff;
								int g = patternBuffer[l * 4 + 1] & 0xff;
								int b = patternBuffer[l * 4 + 2] & 0xff;
								int a = patternBuffer[l * 4 + 3] & 0xff;
								if(a < 255){
									found = true;
								}else{
									found = (nr - r <= RGB_RANGE && nr - r >= -RGB_RANGE) && (ng - g <= RGB_RANGE && ng - g >= -RGB_RANGE) && (nb - b <= RGB_RANGE && nb - b >= -RGB_RANGE);
								}
								if(found){
									found = true;
									x = x - match_x + pw - l;
									y = y - match_y;
									match_x = match_y = 0;
									break;
								}
							}
							if(!found){
								x = x - match_x + pw + 1;
								y = y - match_y;
								match_x = match_y = 0;
							}
						}else{
							x = 0;
							y = y - match_y + 1;
							match_x = match_y = 0;
						}
					}
				}else{					
					//剩余宽度或高度不满足
					//若宽度不满足
					if(h_avail &&(!w_avail)){
						x = 0;
						y = y - match_y + 1;
						match_x = match_y = 0;
					}else{
						break;
					}						
				}					
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	

	
	/**
	 * 从屏幕上查找一个元素并返回坐标
	 * @param resourcePath 相对于jar包的资源路径
	 * @param threads 线程数
	 * @return 匹配到的坐标点，若没有匹配则返回null
	 */
	public static List<MatchPoint> findObject(String resourcePath,int threads){
		RawScreen raw = ScreenUtil.rawScreenshot();
		Bitmap pattern = BitmapFactory.decodeStream(Matcher.class.getClassLoader().getResourceAsStream(resourcePath));
		ByteBuffer pbuffer = ByteBuffer.allocate(pattern.getWidth() * pattern.getHeight() * 4);
		final boolean rotation = UiDevice.getInstance().getDisplayRotation() != 0;
		if(rotation){
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			pattern = Bitmap.createBitmap(pattern, 0, 0, pattern.getWidth() , pattern.getHeight(),matrix,true);
		}
		pattern.copyPixelsToBuffer(pbuffer);
		final byte[] patternBuffer = pbuffer.array();
		final byte[] screenBuffer = raw.rgba();
		final int sw = raw.width;
		final int sh = raw.height;
		final int pw = pattern.getWidth();
		final int ph = pattern.getHeight();
		
		class Runner implements Runnable{
			
			private int starts;
			
			private MatchPoint match;
			
			private final Object locker;
			
			public Runner(int starts,Object locker){
				this.locker = locker;
				this.starts = starts;
			}
			
			public List<MatchPoint> matches(){
				if(match != null){
					return Arrays.asList(match);
				}else{
					return null;
				}				
			}

			@Override
			public void run() {
				int match_x = 0,match_y = 0,x = 0,y = starts;
				int[][] keys = new int[][]{{0,0,0xff,0xff,0xff},{1,0,0,0,0}};
				for(int ky = 0;ky<ph;ky++){
					for(int kx = 0;kx<pw;kx++){
						int r = patternBuffer[(ky * pw + kx) * 4] & 0xff;
						int g = patternBuffer[(ky * pw + kx) * 4 + 1] & 0xff;
						int b = patternBuffer[(ky * pw + kx) * 4 + 2] & 0xff;
						int a = patternBuffer[(ky * pw + kx) * 4 + 3] & 0xff;
						if(a == 255){
							if(r < keys[0][2] && g < keys[0][3] && b < keys[0][4]){
								keys[0][0] = kx;keys[0][1] = ky;keys[0][2] = r;keys[0][3] = g;keys[0][4] = b;
							}
							if(r > keys[1][2] && g > keys[1][3] && b > keys[1][4]){
								keys[1][0] = kx;keys[1][1] = ky;keys[1][2] = r;keys[1][3] = g;keys[1][4] = b;
							}
						}
					}
				}
				
				try{		
					while(y < sh){
						//获取屏幕当前像素
						//本行的剩余宽度和剩余高度满足匹配剩余
						boolean w_avail = (sw - x >= (pw - match_x));
						boolean h_avail = (sh - y >= (ph - match_y));
						boolean p_skip = (x - match_x + pw < sw);
						if(w_avail && h_avail){

							int sr = screenBuffer[(sw * y + x) * 4] & 0xff;
							int sg = screenBuffer[(sw * y + x) * 4 + 1] & 0xff;
							int sb = screenBuffer[(sw * y + x) * 4 + 2] & 0xff;					
							
							int pr = patternBuffer[(pw * match_y + match_x) * 4] & 0xff;
							int pg = patternBuffer[(pw * match_y + match_x) * 4 + 1] & 0xff;
							int pb = patternBuffer[(pw * match_y + match_x) * 4 + 2] & 0xff;
							int pa = patternBuffer[(pw * match_y + match_x) * 4 + 3] & 0xff;
							boolean equals = false;
							if(pa < 255){
								equals = true;
							}else{
								equals = (sr - pr <= RGB_RANGE && sr - pr >= -RGB_RANGE) && (sg - pg <= RGB_RANGE && sg - pg >= -RGB_RANGE) && (sb - pb <= RGB_RANGE && sb - pb >= -RGB_RANGE);
							}
							if(equals){
								//检查匹配图的当前像素是否为横向的匹配边界
								if(match_x == pw - 1){
									//检查匹配图的当前像素是否为纵向的匹配边界
									if(match_y == ph - 1){
										//找到匹配
										if(rotation){
											Point p = new Point(sh - (y - ph / 2), sw - (x - pw / 2));
											match = new MatchPoint(p, sh - y, sw - x, ph, pw);
										}else{
			                                match = new MatchPoint(new Point(x - pw / 2, y - ph / 2),x,y,pw,ph);
										}
										match = new MatchPoint(new Point(x - pw / 2, y - ph / 2),x,y,pw,ph);
										synchronized (locker) {
											locker.notify();
										}
									}else{
										//不为纵向匹配边界则匹配像素横向置零，纵向加一，屏幕横向还原至匹配起始，纵向加一，重新开始当前循环
										match_x = 0;
										match_y += 1;
										x = x - pw + 1;
										y += 1;
									}							
								}else{
									//非横向边界，匹配图横向匹配加一
									if(match_x == match_y && match_x == 0){
										int lwr = screenBuffer[((y + keys[0][1]) * sw + x + keys[0][0]) * 4] & 0xff;
										int lwg = screenBuffer[((y + keys[0][1]) * sw + x + keys[0][0]) * 4 + 1] & 0xff;
										int lwb = screenBuffer[((y + keys[0][1]) * sw + x + keys[0][0]) * 4 + 2] & 0xff;
										
										int upr = screenBuffer[((y + keys[1][1]) * sw + x + keys[1][0]) * 4] & 0xff;
										int upg = screenBuffer[((y + keys[1][1]) * sw + x + keys[1][0]) * 4 + 1] & 0xff;
										int upb = screenBuffer[((y + keys[1][1]) * sw + x + keys[1][0]) * 4 + 2] & 0xff;
										
										boolean found = (lwr - keys[0][2] <= RGB_RANGE && lwr - keys[0][2] >= -RGB_RANGE) && (lwg - keys[0][3] <= RGB_RANGE && lwg - keys[0][3] >= -RGB_RANGE) && (lwb - keys[0][4] <= RGB_RANGE && lwb - keys[0][4] >= -RGB_RANGE);
										if(found){
											if((upr - keys[1][2] <= RGB_RANGE && upr - keys[1][2] >= -RGB_RANGE) && (upg - keys[1][3] <= RGB_RANGE && upg - keys[1][3] >= -RGB_RANGE) && (upb - keys[1][4] <= RGB_RANGE && upb - keys[1][4] >= -RGB_RANGE)){
												
											}else{
												x = x + 1;
												match_x = match_y = 0;
												continue;
											}
										}else{
											x = x + 1;
											match_x = match_y = 0;
											continue;
										}
									}
									x += 1;
									match_x += 1;
								}

							}else{
								//检查匹配起始点 + pw + 1元素是否在匹配图第一行中						
								if(p_skip){
									int nr = screenBuffer[(sw * (y - match_y) + x - match_x + pw) * 4] & 0xff;
									int ng = screenBuffer[(sw * (y - match_y) + x - match_x + pw) * 4 + 1] & 0xff;
									int nb = screenBuffer[(sw * (y - match_y) + x - match_x + pw) * 4 + 2] & 0xff;
									boolean found = false;
									for(int l = pw -1;l >= 0;l--){
										int r = patternBuffer[l * 4] & 0xff;
										int g = patternBuffer[l * 4 + 1] & 0xff;
										int b = patternBuffer[l * 4 + 2] & 0xff;
										int a = patternBuffer[l * 4 + 3] & 0xff;
										if(a < 255){
											found = true;
										}else{
											found = (nr - r <= RGB_RANGE && nr - r >= -RGB_RANGE) && (ng - g <= RGB_RANGE && ng - g >= -RGB_RANGE) && (nb - b <= RGB_RANGE && nb - b >= -RGB_RANGE);
										}
										if(found){
											found = true;
											x = x - match_x + pw - l;
											y = y - match_y;
											match_x = match_y = 0;
											break;
										}
									}
									if(!found){
										x = x - match_x + pw + 1;
										y = y - match_y;
										match_x = match_y = 0;
									}
								}else{
									x = 0;
									y = y - match_y + 1;
									match_x = match_y = 0;
								}
							}
						}else{					
							//剩余宽度或高度不满足
							//若宽度不满足
							if(h_avail &&(!w_avail)){
								x = 0;
								y = y - match_y + 1;
								match_x = match_y = 0;
							}else{
								break;
							}						
						}					
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		final Object locker = new Object();
		List<MatchPoint> result = null;
		threads = sh / threads > ph ? threads:(sh / ph > threads? threads: sh / ph);
		Thread[] threadArray = new Thread[threads];
		Runner[] runnerArray = new Runner[threads];
		for(int i = 0;i<threads;i++){
			runnerArray[i] = new Runner(sw / threads * i, locker);
			threadArray[i] = new Thread(runnerArray[i]);
			threadArray[i].start();
		}
		synchronized (locker) {
			try {
				locker.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for(int i = 0;i<threads;i++){
			if(runnerArray[i].matches() == null){
				threadArray[i].interrupt();
			}else{
				result = runnerArray[i].matches();
				System.out.println("thread "+ i + " got result;");
			}
		}
		
		return result;
	}
}
