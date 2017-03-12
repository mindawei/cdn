package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** 服务器 */
public final class Server {

	/** 放置的位置 */
	public final String nodeId;
	
	/** 服务的消费者 */
	public final List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();

	/** 复制 */
	public Server copy() {
		Server newCopyServer = new Server(nodeId);
		for(ServerInfo serverInfo : serverInfos){
			newCopyServer.serverInfos.add(serverInfo.copy());
		}
		return newCopyServer;
	}

	/** 转移到另一个服务器*/
	public void transferTo(Server toServer, TransferInfo transferInfo) {
		
		int bandWidth = transferInfo.avaliableBandWidth;
		ArrayList<String> viaNodes = transferInfo.nodes;
		// 去头
		ArrayList<String> transferNodes = new ArrayList<String>();
		for(int i=1;i<viaNodes.size();++i){
			transferNodes.add(viaNodes.get(i));
		}
		
		for(ServerInfo localServerInfo : serverInfos){
			// 已经完成
			if(bandWidth==0){
				return;
			}
			
			int transferBandWidth = Math.min(bandWidth, localServerInfo.bandWidth);
			// 转移给新的
			ArrayList<String> nodes = new ArrayList<String>(localServerInfo.nodes);
			nodes.addAll(transferNodes);
			ServerInfo toServerInfo = new ServerInfo(localServerInfo.consumerId,transferBandWidth,nodes);
			toServer.serverInfos.add(toServerInfo);
			// 更新当前的
			localServerInfo.bandWidth -= transferBandWidth;
			if(localServerInfo.bandWidth==0){ // 已经全部转移
				serverInfos.remove(localServerInfo);
			}
		}
		
	}
	
	/** 获得服务器所有的需求 */
	public int getDemand() {
		int demand = 0;
		for(ServerInfo info : serverInfos){
			demand+=info.bandWidth;
		}
		return demand;
	}
	
	private Server(String nodeId){
		super();
		this.nodeId = nodeId;
	}
	
	public Server(String consumerId,String nodeId,int demand) {
		super();
		this.nodeId = nodeId;
		ArrayList<String> viaNodes = new ArrayList<String>();
		viaNodes.add(nodeId);
		serverInfos.add(new ServerInfo(consumerId,demand,viaNodes));
	}

	/**
	 * 网络节点ID-01 网络节点ID-02 …… 网络节点ID-n 消费节点ID 占用带宽大小
	 */
	public List<String> getSolution() {
		List<String> solution = new LinkedList<String>();
		for(ServerInfo serverInfo : serverInfos){
			StringBuilder builder = new StringBuilder();
			for(int i=serverInfo.nodes.size()-1;i>=0;--i){
				builder.append(serverInfo.nodes.get(i));
				builder.append(" ");
			}
			builder.append(serverInfo.consumerId);
			builder.append(" ");
			builder.append(serverInfo.bandWidth);

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

	@Override
	public String toString() {
		return "Server [nodeId=" + nodeId + ", serverInfos=" + serverInfos
				+ "]";
	}

}
