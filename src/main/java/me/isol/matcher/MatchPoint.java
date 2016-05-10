package me.isol.matcher;

import java.util.List;

import android.graphics.Rect;

public class MatchPoint {

	/**
	 * 所找到元素的中心点
	 */
	public final Point point;
	/**
	 * 元素的边界（SIFT模式下，此值不一定完全准确）
	 */
	public Rect rect;
	
	public MatchPoint(Point point,List<Point> points,double scale){
		this.point = point;
		this.point.x = (int)(point.x * scale);
		this.point.y = (int)(point.y * scale);
		int left=point.x,right=point.x,top=point.y,bottom=point.y;
		for(Point p:points){
			p.x = (int)(p.x * scale);
			p.y = (int)(p.y * scale);
			if(p.x < left){
				left = p.x;
			}
			if(p.x > right){
				right = p.x;
			}
			if(p.y < top){
				top = p.y;
			}
			if(p.y > bottom){
				bottom = p.y;
			}
		}
		rect = new Rect(left, top, right, bottom);
	}
	
	public MatchPoint(Point point,int x,int y,int width,int height){
		this.point = point;
		rect = new Rect(x - width + 1, y - height + 1, x, y);
	}
	
	/**
	 * 返回匹配宽度
	 * @return
	 */
	public int width(){
		return rect.right - rect.left;
	}
	
	/**
	 * 返回匹配高度
	 * @return
	 */
	public int height(){
		return rect.bottom - rect.top;
	}


}
