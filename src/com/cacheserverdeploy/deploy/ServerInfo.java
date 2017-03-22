package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.List;

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
	
	/** 提供的带宽  */
    public int provideBandWidth;

	/** 带宽费*/
	public int getBandWidthCost(){
		// 基于 viaNodes 不可变
		int unitBandWidthCost = 0;
		for(int i=0;i<viaNodes.length-1;++i){
			Edge edge = Global.graph[viaNodes[i]][viaNodes[i+1]];
			unitBandWidthCost += edge.cost;
		}
		return provideBandWidth * unitBandWidthCost;
	}
	
	public ServerInfo(int consumerId,int provideBandWidth,int[] viaNodes) {
		this.consumerId = consumerId;
		this.provideBandWidth = provideBandWidth;
		this.viaNodes = viaNodes;
	}

	@Override
	public String toString() {
		return "ServerInfo [consumerId=" + consumerId + ", viaNodes="
				+ Arrays.toString(viaNodes) + ", provideBandWidth="
				+ provideBandWidth + "]";
	}
	
	public String getSolution() {
		StringBuilder builder = new StringBuilder();
		for(int i=viaNodes.length-1;i>=0;--i){
			builder.append(viaNodes[i]);
			builder.append(" ");
		}
		builder.append(consumerId);
		builder.append(" ");
		builder.append(provideBandWidth);
		return builder.toString();
	}
	
	
	
}
