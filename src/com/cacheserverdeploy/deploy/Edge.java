package com.cacheserverdeploy.deploy;

/**
 * 链路:图中的一条边
 * @author mindw
 * @date 2017年3月10日
 */
public final class Edge {	
	
	/** 剩余网络带宽 */
	private int leftBandWidth;
	
	public int getLeftBandWidth() {
		return leftBandWidth;
	}
	//public StringBuilder log = new StringBuilder();
	public void useBandWidth(int bandWidth) {
		leftBandWidth-=bandWidth;
		//log.append("-"+bandWidth+"="+leftBandWidth+"\n");
	}
	
	public void relaseBandWidth(int bandWidth) {
		leftBandWidth+=bandWidth;
		//log.append("+"+bandWidth+"="+leftBandWidth+"\n");
	}

	/** 单位网络租用费 */
	public final int cost;

	/** 初始化带宽 */
	public final int initBandWidth;

	public Edge(int bandWidth, int cost) {
		this.initBandWidth = bandWidth;
		this.leftBandWidth = bandWidth;
		this.cost = cost;
	}
	
	private int savedBandWidth;
	
	/** 保存 */
	public void saveCurrentBandWidth(){
		savedBandWidth = leftBandWidth;
	}
	
	/** 恢复 */
	public void goBackBandWidth(){
		leftBandWidth = savedBandWidth;
		//log.append("back "+savedBandWidth+"\n");
	}

	
}
