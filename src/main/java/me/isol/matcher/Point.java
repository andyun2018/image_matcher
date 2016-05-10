package me.isol.matcher;


public class Point{
	/**
	 * 横坐标
	 */
	public int x;
	/**
	 * 纵坐标
	 */
	public int y;
	
	public Point(String x,String y){
		this.x = Integer.parseInt(x);			
		this.y = Integer.parseInt(y);			
	}
	
	public Point(int x,int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o != null){
			if(o instanceof Point){
				Point p = (Point) o;
				if(p.x == this.x && p.y == this.y){
					return true;
				}else{
					return false;
				}
			}else{
				return false;
			}
		}else{
			return false;
		}			
	}
}
