package com.cacheserverdeploy.deploy;

import java.util.ArrayList;

/**
 * 传送的消耗 
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public final class TransferInfo {
	
	/** 可提供的带宽 */
	int avaliableBandWidth;
	
	/** 到达需要的费用：单位花费 */
	int cost; 
	
	/** 经过的节点ID,包括了首尾 */
	ArrayList<Integer> nodes;
	
	public TransferInfo(int cost,ArrayList<Integer> nodes) {
		super();
		this.cost = cost;
		this.nodes = nodes;
	}
	
}
