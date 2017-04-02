package com.cacheserverdeploy.deploy;

/**
 * 链路:图中的一条边
 * @author mindw
 * @date 2017年3月10日
 */
public final class Edge {	
	
	/** 剩余网络带宽 */
	public int leftBandWidth;
	
	/** 单位网络租用费 */
	public final int cost;

	public final int initBandWidth;
	
	public Edge(int bandWidth, int cost) {
		this.initBandWidth = bandWidth;
		this.leftBandWidth = bandWidth;
		this.cost = cost;
	}
	
	public void reset() {
		leftBandWidth = initBandWidth;
	}
	
}
