package com.cacheserverdeploy.deploy;

import java.util.Arrays;

/**
 * 传送的消耗 
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public final class TransferInfo {
	
	/** 服务节点 */
	int serverNode;
	
	/** 可提供的带宽 */
	int avaliableBandWidth;
	
	/** 到达需要的费用：单位花费 */
	int cost; 
	
	/** 经过的节点ID,包括了首尾 */
	int[] viaNodes;
	
	public TransferInfo(int cost,int[] viaNodes) {
		super();
		this.cost = cost;
		this.viaNodes = viaNodes;
	}

	@Override
	public String toString() {
		return "TransferInfo [serverNode=" + serverNode
				+ ", avaliableBandWidth=" + avaliableBandWidth + ", cost="
				+ cost + ", viaNodes=" + Arrays.toString(viaNodes) + "]";
	}
	
	
	
}
