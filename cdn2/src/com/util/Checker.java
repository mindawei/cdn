package com.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.cacheserverdeploy.deploy.Edge;
import com.cacheserverdeploy.deploy.Global;
import com.cacheserverdeploy.deploy.Server;
import com.cacheserverdeploy.deploy.ServerInfo;
import com.filetool.util.FileUtil;

/**
 * 校验
 *
 * @author mindw
 * @date 2017年3月13日
 */
public class Checker {
	
	// case_example/case0.txt result/0.txt
	public static void main(String[] args) {
		 String graphFilePath = FilePath.graphFilePath;
	     String resultFilePath = FilePath.resultFilePath;
	     System.out.println(graphFilePath+" "+resultFilePath);
	     // 读取输入文件
	     String[] graphContent = FileUtil.read(graphFilePath, null); 
	     // printLine(graphContent);
	     String[] resultContent = FileUtil.read(resultFilePath, null);
	     // printLine(resultContent);
	     
	     Global.init(graphContent);
	    
	     Map<Integer,Integer> consumerDemands = new HashMap<Integer,Integer>();
	     for(Server server : Global.getBestServers()){
	    	 if(server==null){
	    		 break;
	    	 }
	    	 ServerInfo serverInfo = server.getServerInfos()[0];
	    	 Integer consumerId = serverInfo.consumerId;
	    	 int demand = serverInfo.provideBandWidth;
	    	 consumerDemands.put(consumerId, demand);
	     }
	    
	     class ServerInfo implements Comparable<ServerInfo>{
	    	 int node;
	    	 int level;
	    	 int nodeCost;
			public ServerInfo(int node, int level,int nodeCost) {
				super();
				this.node = node;
				this.level = level;
				this.nodeCost = nodeCost;
			}
			@Override
			public String toString() {
				return "[" + node + ", " + level + ", "+nodeCost+"]";
			}
			@Override
			public int compareTo(ServerInfo o) {
				return node-o.node;
			}
	    	 
	     }
	     Set<ServerInfo> servers = new TreeSet<ServerInfo>();
	     
//	     // 服务器等级 -> ServerType
//	     Map<Integer,ServerType> levelToServerType = new HashMap<Integer,ServerType>();
//	     for(ServerType serverType : Global.serverTypes){
//	    	 levelToServerType.put(serverType.serverLevel,serverType);
//	     }
	     
	     // 需要验证，服务器节点的level是否相同
	     Map<Integer,Integer> serverNodeToLevel = new HashMap<Integer,Integer>();
	     
	     // 需要验证，服务器节点的供应是否超出最大流量
	     Map<Integer,Integer> serverNodeLeftOutput = new HashMap<Integer,Integer>();
	     
	     
	     int cost = 0;
	     
	     int serverCost = 0;
	     int nodeCost = 0;
	     int routeCost = 0;
	     
	     for(int i=2;i<resultContent.length;++i){
	    	 String line = resultContent[i];
	    	 // System.out.println(line);
	    	 String[] parts = line.split(" ");
	    	 int size = parts.length;
	    	 
	    	 // 服务器档次
	    	 int serverLevel = Integer.parseInt(parts[size-1]) ;
	    	 int bandwidth = Integer.parseInt(parts[size-2]);
	    	 Integer consumerId = Integer.parseInt(parts[size-3]);
	    	 for(int j=0;j<size-4;++j){
	    		 Edge edge = Global.graph[Integer.parseInt(parts[j])][Integer.parseInt( parts[j+1])];
	 			
	    		 edge.leftBandWidth -= bandwidth;
	    		 if(edge.leftBandWidth<0){
	    			 System.out.println("edge.initBandWidth"+edge.initBandWidth);
	    			 System.out.println("流量超出边界限制 edge.bandWidth<0:"+line);
	    			 System.exit(0);
	    		 }  
	    		 cost+=bandwidth*edge.cost;
	    		 routeCost+=bandwidth*edge.cost;
	    	 }
	    	 
	    	 int demand = consumerDemands.get(consumerId);
	    	 demand-=bandwidth;
	    	 consumerDemands.put(consumerId,demand);
	    	 
	    	 // 服务器节点
	    	 int serverNode = Integer.parseInt(parts[0]);
	    	 servers.add(new ServerInfo(serverNode, serverLevel,Global.nodeDeployCosts[serverNode]));
	    	 
	    	 // 检查服务器部署等级是否一致
	    	 if(serverNodeToLevel.containsKey(serverNode)){
	    		 if(serverNodeToLevel.get(serverNode)!=serverLevel){
	    			 System.out.println("serverNode 部署等级不一致："+line);
	    			 System.exit(0);
	    		 }
	    	 }else{
	    		 
	    		 // 部署服务器的费用
	    		 cost += Global.nodeDeployCosts[serverNode] + Global.serverDeployCosts[serverLevel];
	    		 serverCost += Global.nodeDeployCosts[serverNode];
	    		 nodeCost += Global.serverDeployCosts[serverLevel];
	    		 
	    		 
	    		 serverNodeToLevel.put(serverNode,serverLevel);
	    		 int leftOutput =  Global.serverMaxOutputs[serverLevel];
	    		 serverNodeLeftOutput.put(serverNode, leftOutput);
	    	 }
	    	 
	    	 // 检查是否超出服务器流量
	    	 int leftOutput = serverNodeLeftOutput.get(serverNode);
	    	 leftOutput -= bandwidth;
	    	 if(leftOutput<0){
	    		 System.out.println("serverNode:"+serverNode+" 的流量超出最大输出！");
	    		 System.exit(0);
	    	 }else{
	    		 // 更新剩余的流量
	    		 serverNodeLeftOutput.put(serverNode, leftOutput);
	    	 }
	    	 
	     }
	     
	     for(Map.Entry<Integer,Integer> consumerDemand : consumerDemands.entrySet()){
	    	 if(consumerDemand.getValue()!=0){
	    		 System.out.print("consumerDemand.getValue()!=0 : ");
	    		 System.out.println(consumerDemand.getKey()+" "+consumerDemand.getValue());
	    		 System.exit(0);
	    	 }
	     }
	     
	     
	     System.out.println("ok");
	     System.out.println("费用："+cost+" 服务器数："+servers.size());
	     System.out.println(String.format("服务器费用：%d,占比：%.2f", serverCost,(serverCost*1.0)/cost));
	     System.out.println(String.format("节点费用：%d,占比：%.2f", nodeCost,(nodeCost*1.0)/cost));
	     System.out.println(String.format(" 路由费用：%d,占比：%.2f", routeCost,(routeCost*1.0)/cost));
	     
	  
	     
	     
	     
	   System.out.println("服务器部署如下：");
	   for(ServerInfo info :servers)
	     System.out.println(info);
	   
	   System.out.println();
	     
	     printLine(resultContent);
	    
	}
	
	private static void printLine(String[] content){
		for(String line : content){
			System.out.println(line);
		}
	}

}
