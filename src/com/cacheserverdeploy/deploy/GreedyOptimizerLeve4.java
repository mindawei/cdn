package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.Random;

/**
 * 防止在局部最优中出不来
 * 第一轮不随机，之后就完全随机了
 * 
 * @author mindw
 * @date 2017年3月23日
 */
public final class GreedyOptimizerLeve4 extends GreedyOptimizerMiddle{
	
	private OptimizerMCMF optimizerMCMF;
	/** 频率大于0的点 */
	private int[] nodes;
	
	/** 候选节点是否被选中 */
	private final boolean[] selected;
	
	/** 每次最多随机选多少个*/
	private final int selectNum;
	/** 下一轮的服务器*/
	private Server[] serversInRandom;
	private int serverSize;

	private final Random random = new Random(47);
	
	/** 每轮最多移动多少次 */
	private final int maxMovePerRound;
	
	private final int MAX_UPDATE_NUM;
	private final int MIN_UPDATE_NUM;
	
	/**
	 * 构造函数
	 * @param nearestK 初始化的时候选每个消费者几个最近领
	 * @param selectNum 随机生成的时候服务器个数
	 * @param maxMovePerRound 每轮最多移动多少次
	 */
	public GreedyOptimizerLeve4(OptimizerMCMF optimizerMCMF,int[] nodes,int selectNum,int maxMovePerRound,int maxUpdateNum,int minUpdateNum){
		this.optimizerMCMF = optimizerMCMF;
		this.MAX_UPDATE_NUM = maxUpdateNum;
		this.MIN_UPDATE_NUM = minUpdateNum;
		this.selectNum = selectNum;
		this.nodes = nodes;
		selected = new boolean[nodes.length];
		serversInRandom = new Server[Global.nodeNum];
		this.maxMovePerRound = maxMovePerRound;
	}
	
	private void selcetServers(){
		serverSize = 0;
		
		for (Server server : Global.getBestServers()) {
			if(server==null){
				break;
			}
			serversInRandom[serverSize++] = new Server(server.node);
		
		}
		// 设置结束标志
		if(serverSize<serversInRandom.length){
			serversInRandom[serverSize] = null;
		}
		
	}
	

	private void selcetBestServers(){
		serverSize = 0;
		
		int leftNum = selectNum;
		// 肯定是服务器的
		for(int node : Global.mustServerNodes){
			serversInRandom[serverSize++] = new Server(node);
		}
		leftNum -= Global.mustServerNodes.length;
		int index = 0;
		// 随机选择
		while(leftNum>0&&index<nodes.length){
			// 没有被选过
			int node = nodes[index++];
			// 服务器上面已经添加过了
			if (!Global.isMustServerNode[node]) {
				serversInRandom[serverSize++] = new Server(node);
				leftNum--;
			}
		}
		// 设置结束标志
		if(serverSize<serversInRandom.length){
			serversInRandom[serverSize] = null;
		}
		
	}
	
	/** 随机选择服务器 ,改变{@link #nextRoundServers}*/
	private void selectRandomServers() {
		
		Arrays.fill(selected, false);
		serverSize = 0;
		
		int leftNum = selectNum;
		// 肯定是服务器的
		for(int node : Global.mustServerNodes){
			serversInRandom[serverSize++] = new Server(node);
		}
		leftNum -= Global.mustServerNodes.length;
		// 随机选择
		while(leftNum>0){
			int index = random.nextInt(nodes.length);
			// 没有被选过
			if(!selected[index]){
				int node = nodes[index];
				if(Global.isMustServerNode[node]){
					//  是服务器，服务器上面已经添加过了
					selected[index] = true;
				}else{ 
					selected[index] = true;
					serversInRandom[serverSize++] = new Server(node);
					leftNum--;
				}
			}
		}
		// 设置结束标志
		if(serverSize<serversInRandom.length){
			serversInRandom[serverSize] = null;
		}
	}
	
	
	
	void optimizeMCMF() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();
			
		//selcetBestServers();
		//selectRandomServers();
		
		
		int lastCsot = Global.INFINITY;
		
		int maxUpdateNum = MAX_UPDATE_NUM;
		
		while (true) {

			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;
			selcetServers();
		
			if(serverSize==0){
				break;
			}
			
			 final int leftMoveRound = maxMovePerRound / serverSize;

			 if(Global.IS_DEBUG){
				 System.out.println("maxUpdateNum:"+maxUpdateNum);
			 }
			 
			 for (int i=0;i<serverSize;++i) {
				Server oldServer = serversInRandom[i];
				int fromNode = oldServer.node;
				
				// 服务器不移动
				if(Global.isMustServerNode[fromNode]){
					continue;
				}
				
				int leftNum = leftMoveRound;
				for (int toNode : nodes) {		
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}
					
					// 移动
					move(serversInRandom, fromNode, toNode);
					
				}
				
			}
			
			

			if (Global.isTimeOut()) {
				return;
			}
			
			
			
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}
		
	}
	
	/** 进行一步移动 ,不要改变传进来的Server,结果缓存在 nextGlobalServers */
	protected void move(Server[] oldServers,int fromServerNode, int toServerNode) {
		Arrays.fill(newServers, null);
		int lsSize = 0;
		for (Server server : oldServers) {
			if(server==null){
				break;
			}
			if (server.node != fromServerNode) {
				Server newServer = new Server(server.node);
				newServers[server.node] = newServer;
				lsNewServers[lsSize++] = newServer;
			}
		}
		Server newServer = new Server(toServerNode);
		newServers[toServerNode] = newServer;
		lsNewServers[lsSize++] = newServer;
		optimizerMCMF.optimizeCASE0(lsNewServers);
		//Global.resetEdgeBandWidth();
		//transferServers(nextGlobalServers,newServers,lsNewServers,lsSize);
	}
	
}
