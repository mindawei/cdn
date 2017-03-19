package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** 服务器 */
public final class Server {

	/** 放置的位置 */
	public final int node;
	
	/** 服务的消费者 */
	public final ArrayList<ServerInfo> serverInfos = new ArrayList<ServerInfo>();

	public Server(int node){
		super();
		this.node = node;
	}
	
	public Server(int consumerId,int node,int demand) {
		super();
		this.node = node;
		serverInfos.add(new ServerInfo(consumerId,demand,new int[]{node}));
	}
	
	/** 获得服务器所有的需求 */
	public int getDemand() {
		int demand = 0;
		for(ServerInfo info : serverInfos){
			demand += info.provideBandWidth;
		}
		return demand;
	}
	
	/**
	 * 网络节点ID-01 网络节点ID-02 …… 网络节点ID-n 消费节点ID 占用带宽大小
	 */
	public List<String> getSolution() {
		List<String> solution = new LinkedList<String>();
		for(ServerInfo serverInfo : serverInfos){
			StringBuilder builder = new StringBuilder();
			for(int i=serverInfo.viaNodes.length-1;i>=0;--i){
				builder.append(serverInfo.viaNodes[i]);
				builder.append(" ");
			}
			builder.append(serverInfo.consumerId);
			builder.append(" ");
			builder.append(serverInfo.provideBandWidth);

			solution.add(builder.toString());
		}
		return solution;
	}

	/** 获得总的带宽费用费用数 */
	public int getCost() {
		int toatlCost = Global.depolyCostPerServer;
		for(ServerInfo serverInfo : serverInfos){
			toatlCost += serverInfo.getBandWidthCost();
		}
		return toatlCost;
	}
	
}
