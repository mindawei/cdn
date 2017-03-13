package com.cacheserverdeploy.deploy;

/**
 * 链路:图中的一条边
 * @author mindw
 * @date 2017年3月10日
 */
public final class Edge {	
	
	/** 剩余网络带宽 */
	public int bandWidth;
	
	/** 单位网络租用费 */
	public final int cost;

	public final int initBandWidth;
	
	public void reset() {
		bandWidth = initBandWidth;
	}
	
	public Edge(int bandWidth, int cost) {
		this.initBandWidth = bandWidth;
		this.bandWidth = bandWidth;
		this.cost = cost;
	}
	
	private int savedBandWidth;
	
	/** 保存 */
	public void saveCurrentBandWidth(){
		savedBandWidth = bandWidth;
	}
	
	/** 恢复 */
	public void goBackBandWidth(){
		bandWidth = savedBandWidth;
	}
	
}
