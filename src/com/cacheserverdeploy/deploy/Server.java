package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

	/** 
	 * 转移到另一个服务器，并返回价格<br>
	 * 注意：可能cost会改变(又返回之前的点)
	 */
	public int transferTo(Server toServer, TransferInfo transferInfo) {
	
		Set<String> consumerIds = new HashSet<String>(); 
		for(ServerInfo info : serverInfos ){
			consumerIds.add(info.consumerId);
		}
		
		int bandWidth = transferInfo.avaliableBandWidth;
		int transferCost = 0;
		
		ArrayList<String> viaNodes = transferInfo.nodes;
		// 去头
		ArrayList<String> transferNodes = new ArrayList<String>();
		for(int i=1;i<viaNodes.size();++i){
			transferNodes.add(viaNodes.get(i));
		}
		
		LinkedList<ServerInfo> needRemoveServerInfos = new LinkedList<ServerInfo>();
		for(ServerInfo localServerInfo : serverInfos){
			
			int transferBandWidth = Math.min(bandWidth, localServerInfo.bandWidth);
			// 转移给新的
//			System.out.println(transferBandWidth+" min "+bandWidth+" "+localServerInfo.bandWidth );
			
			ArrayList<String> localNodes = new ArrayList<String>(localServerInfo.nodes);
			for(String transferNode : transferNodes){
				if(localNodes.contains(transferNode)){  // 存在回路
					localNodes.add(transferNode);
					int index = localNodes.indexOf(transferNode);
					// 删除回路影响
					for(int i=index;i<localNodes.size()-1;++i){
						Edge edge = Global.getEdge(localNodes.get(i), localNodes.get(i+1));
						transferCost -= transferBandWidth * edge.cost;
						
//						if(edge.getLeftBandWidth()+transferBandWidth>edge.initBandWidth){
//							System.out.println(localNodes.get(i)+ "->"+ localNodes.get(i+1));
//							System.out.println(edge.getLeftBandWidth()+" + "+transferBandWidth+" > "+edge.initBandWidth);
//							//System.out.println(edge.log.toString());
//							System.exit(0);
//						}
						edge.relaseBandWidth(transferBandWidth);
					}
					// 删除回路
					ArrayList<String> newNodes = new ArrayList<String>(index+1);
					for(int i=0;i<=index;++i){
						newNodes.add(localNodes.get(i));
					}
					localNodes = newNodes;
				}else{ // 不存在回路
					localNodes.add(transferNode);
					int size = localNodes.size();
					Edge edge = Global.getEdge(localNodes.get(size-2), localNodes.get(size-1));
					transferCost += transferBandWidth * edge.cost;
				}
				
				if(bandWidth-transferBandWidth<0){
					System.out.println(bandWidth);
				}
				bandWidth-=transferBandWidth;
				// 已经完成
				if(bandWidth==0){
					return transferCost;
				}
			}
			
			ServerInfo toServerInfo = new ServerInfo(localServerInfo.consumerId,transferBandWidth,localNodes);
			toServer.serverInfos.add(toServerInfo);
			// 更新当前的
			localServerInfo.bandWidth -= transferBandWidth;
			if(localServerInfo.bandWidth==0){ // 已经全部转移
				needRemoveServerInfos.add(localServerInfo);
			}
		}
		serverInfos.removeAll(needRemoveServerInfos);
		
		
//		Set<String> toConsumerIds = new HashSet<String>(); 
//		for(ServerInfo info : toServer.serverInfos ){
//			toConsumerIds.add(info.consumerId);
//		}
		
		return transferCost;
	}
	
	/** 获得服务器所有的需求 */
	public int getDemand() {
		int demand = 0;
		for(ServerInfo info : serverInfos){
			demand+=info.bandWidth;
		}
		return demand;
	}
	
	public Server(String nodeId){
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

	@Override
	public boolean equals(Object obj) {
		Server other = (Server)obj;
		return nodeId.equals(other.nodeId);
	}
}
