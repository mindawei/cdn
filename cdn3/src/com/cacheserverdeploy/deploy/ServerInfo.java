package com.cacheserverdeploy.deploy;

/**
 * 服务信息
 * 
 * @author mindw
 * @date 2017年3月11日
 */
public final class ServerInfo {

	/** 服务的消费者ID */
	public final int consumerId;

	/**
	 * 消费者到服务器当中经过的路径 ,包括消费者节点和服务器节点<br>
	 * 当服务器和消费者在一个节点上时只有一个值，服务器的节点（也是消费者的节点）<br>
	 * [消费者节点 节点1 节点2 ... 节点i ... 服务器节点] <br>
	 */
	final int[] viaNodes;

	/** 提供的带宽 */
	public int provideBandWidth;
	
	/** 退流 */
	public void returnBandWidth() {
		for (int i = 0; i < viaNodes.length - 1; ++i) {
			Edge edge = Global.graph[viaNodes[i + 1]][viaNodes[i]];
			edge.leftBandWidth += provideBandWidth;
		}
	}
	
	public ServerInfo(int consumerId, int provideBandWidth, int[] viaNodes) {
		this.consumerId = consumerId;
		this.provideBandWidth = provideBandWidth;
		this.viaNodes = viaNodes;
	}
}
