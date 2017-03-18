package com.cacheserverdeploy.deploy;

/**
 * 传送的消耗 
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public final class TransferInfo {
		
	Server fromServer;
	
	/** 可提供的带宽 */
	int avaliableBandWidth;
	
	/** 到达需要的费用：单位花费 */
	int cost; 
	
	/** 经过的节点ID,包括了首尾,从消费者开始，服务器路径应该逆向 */
	int[] viaNodes;
	
	public TransferInfo(Server fromServer,int cost,int[] viaNodes) {
		super();
		this.fromServer = fromServer;
		this.cost = cost;
		this.viaNodes = viaNodes;
	}
		
}
