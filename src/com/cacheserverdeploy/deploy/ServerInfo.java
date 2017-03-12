package com.cacheserverdeploy.deploy;

import java.util.ArrayList;

/**
 * 服务信息
 * 
 * @author mindw
 * @date 2017年3月11日
 */
public final class ServerInfo {
	
	/** 服务的消费者ID */
	final String consumerId;
	
	/** 使用的带宽  */
    int bandWidth;
	
	/**
	 * 消费者到服务器当中经过的路径 ,包括消费者节点和服务器节点<br> 
	 * 当服务器和消费者在一个节点上时只有一个值，服务器的节点（也是消费者的节点）<br>
	 * [消费者节点 节点1 节点2 ... 节点i ... 服务器节点] <br>
	 */
	final ArrayList<String> nodes;
	
	public ServerInfo copy() {
		return new ServerInfo(consumerId, bandWidth, new ArrayList<String>(nodes));
	}
	
	/** 带宽费*/
	public int getBandWidthCost(){
		int unitBandWidthCost = 0;
		for(int i=0;i<nodes.size()-1;++i){
			Edge edge = Global.getEdge(nodes.get(i), nodes.get(i+1));
			unitBandWidthCost += edge.cost;
		}
		return unitBandWidthCost * bandWidth;
	}
	
	public ServerInfo(String consumerId,int bandWidth,ArrayList<String> nodes) {
		super();
		this.consumerId = consumerId;
		this.bandWidth = bandWidth;
		this.nodes = nodes;
	}

	@Override
	public String toString() {
		return "ServerInfo [consumerId=" + consumerId + ", bandWidth="
				+ bandWidth + ", nodes=" + nodes + "]";
	}


}
