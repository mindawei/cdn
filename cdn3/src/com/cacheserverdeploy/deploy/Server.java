package com.cacheserverdeploy.deploy;

import java.util.List;

/** 服务器 */
public final class Server{

	/** 放置的位置 */
	public final int node;
	
	/** 服务的消费者 */
	ServerInfo[] serverInfos;
	int size;
	
	/** 等级 */
	private int serverLevel;
	
	public Server(int node){
		super();
		this.node = node;
	}
	
	public void addServerInfo(ServerInfo serverInfo) {
		if(serverInfos==null){
			size = 0;
			serverInfos = new ServerInfo[16]; // 初始容量为16
			serverInfos[size++] = serverInfo;
			return;
		}
	
		if(size<serverInfos.length){
			serverInfos[size++] = serverInfo;
		}else{
			ServerInfo[] newServerInfos = new ServerInfo[serverInfos.length*2];
			System.arraycopy(serverInfos, 0, newServerInfos, 0, serverInfos.length);
			serverInfos = newServerInfos;
			serverInfos[size++] = serverInfo;
		}
	}

	/** 获得服务器所有的需求 */
	public int getDemand() {
		int demand = 0;
		for(int i=0;i<size;++i){
			demand += serverInfos[i].provideBandWidth;
		}
		return demand;
	}
	
	private final String SPACE = " ";
	/**
	 * 网络节点ID-01 网络节点ID-02 …… 网络节点ID-n 消费节点ID 占用带宽大小 服务器硬件档次ID <br>
	 * 路径的起始节点ID-01表示该节点部署了视频内容服务器，终止节点为某个消费节点<br>
	 */
	public void getSolution(List<String> lines,StringBuilder builder) {
		
		// 根据提供的流量确定服务器的等级
		for(int i=0;i<size;++i){
			// 清空
			builder = new StringBuilder();
			ServerInfo serverInfo = serverInfos[i];
			for(int j=serverInfo.viaNodes.length-1;j>=0;--j){
				builder.append(serverInfo.viaNodes[j]);
				builder.append(SPACE);
			}
			builder.append(serverInfo.consumerId);
			builder.append(SPACE);
			builder.append(serverLevel);
			builder.append(SPACE);
			builder.append(serverInfo.provideBandWidth);
			lines.add(builder.toString());
		}
	}
	
	void setServerLevel(int serverLevel){
		this.serverLevel = serverLevel;
	}

}
