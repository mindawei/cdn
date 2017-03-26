package com.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.cacheserverdeploy.deploy.Edge;
import com.cacheserverdeploy.deploy.Global;
import com.cacheserverdeploy.deploy.Parser;
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
	     Parser.buildNetwork(graphContent);
	     
	     Global.init();
	    
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
	    
	     Set<Integer> servers = new TreeSet<Integer>();
	     
	     int cost = 0;
	     for(int i=2;i<resultContent.length;++i){
	    	 String line = resultContent[i];
	    	 // System.out.println(line);
	    	 String[] parts = line.split(" ");
	    	 int size = parts.length;
	    	 int bandwidth = Integer.parseInt(parts[size-1]);
	    	 Integer consumerId = Integer.parseInt(parts[size-2]);
	    	 for(int j=0;j<size-3;++j){
	    		 Edge edge = Global.graph[Integer.parseInt(parts[j])][Integer.parseInt( parts[j+1])];
	 			
	    		 edge.leftBandWidth -= bandwidth;
	    		 if(edge.leftBandWidth<0){
	    			 System.out.println("edge.bandWidth<0:"+line);
	    			 System.exit(0);
	    		 }  
	    		 cost+=bandwidth*edge.cost;
	    	 }
	    	 
	    	 int demand = consumerDemands.get(consumerId);
	    	 demand-=bandwidth;
	    	 consumerDemands.put(consumerId,demand);
	    	 
	    	 servers.add(Integer.parseInt(parts[0]));
	     }
	     
	     for(Map.Entry<Integer,Integer> consumerDemand : consumerDemands.entrySet()){
	    	 if(consumerDemand.getValue()!=0){
	    		 System.out.print("consumerDemand.getValue()!=0 : ");
	    		 System.out.println(consumerDemand.getKey()+" "+consumerDemand.getValue());
	    		 System.exit(0);
	    	 }
	     }
	     
	     cost += servers.size() * Global.depolyCostPerServer;
	     System.out.println("ok");
	     System.out.println("费用："+cost+" 服务器数："+servers.size());
	     for(int node :servers){
	    	 System.out.print(node+", ");
	     }
	     System.out.println();
	     
	     printLine(resultContent);
	    
	}
	
	private static void printLine(String[] content){
		for(String line : content){
			System.out.println(line);
		}
	}

}
