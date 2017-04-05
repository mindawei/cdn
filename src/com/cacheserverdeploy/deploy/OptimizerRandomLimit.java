package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.Random;

public class OptimizerRandomLimit extends OptimizerSimple{
	
	private final OptimizerMiddleLimit optimizerMiddleLimit;
	private final OptimizerMCMF optimizerMCMF;


	/** 频率大于0的点 */
	protected final int[] nodes;
	/** 每次最多随机选多少个 */
	protected final int selectNum;
	/** 每轮最多移动多少次 */
	private final int maxMovePerRound;
	private final int MAX_UPDATE_NUM;
	private final int MIN_UPDATE_NUM;
	private final Random random = new Random();
	private final boolean[] selected;
	
	/**
	 * 构造函数
	 * 
	 * @param nearestK
	 *            初始化的时候选每个消费者几个最近领
	 * @param selectNum
	 *            随机生成的时候服务器个数
	 * @param maxMovePerRound
	 *            每轮最多移动多少次
	 */
	public OptimizerRandomLimit(OptimizerMCMF optimizerMCMF,int[] nodes, 
			int selectNum,
			int simpleMaxMovePerRound,
			int simpleMaxUpdateNum,
			int middleMaxMovePerRound,
			int middleMaxUpdateNum) {
		this.nodes = nodes;
		this.selectNum = selectNum;
		this.maxMovePerRound = simpleMaxMovePerRound;
		this.MAX_UPDATE_NUM = simpleMaxUpdateNum;
		this.MIN_UPDATE_NUM = simpleMaxUpdateNum;
	
		this.selected = new boolean[Global.nodeNum];
		
		this.optimizerMiddleLimit = new OptimizerMiddleLimit(nodes, middleMaxMovePerRound , middleMaxUpdateNum, middleMaxUpdateNum);
		this.optimizerMCMF = optimizerMCMF;
		
		for(int i=0;i<Global.nodeNum;++i){
			nodeInfos[i] = new NodeInfo();
		}
	}

	
	
	protected void selectServers() {
		serverNodesSize = 0;

		int leftNum = selectNum;
		// 肯定是服务器的
		for (int node : Global.mustServerNodes) {
			serverNodes[serverNodesSize++] = node;
		}
		leftNum -= Global.mustServerNodes.length;
		int index = 0;
		// 随机选择
		while (leftNum > 0 && index < nodes.length) {
			// 没有被选过
			int node = nodes[index++];
			// 服务器上面已经添加过了
			if (!Global.isMustServerNode[node]) {
				serverNodes[serverNodesSize++] = node;
				leftNum--;
			}
		}
	}
	
	private int[] hits = new int[Global.nodeNum];
	
	class NodeInfo implements Comparable<NodeInfo>{
		int node;
		int hit;
		int rank;
		@Override
		public int compareTo(NodeInfo o) {
			return o.hit-hit;
		}
	}
	NodeInfo[] nodeInfos = new NodeInfo[Global.nodeNum];
	
	private void randomNodes(){
//		for(int i=0;i<Global.nodeNum;++i){
//			nodeInfos[i].node = i;
//			nodeInfos[i].hit = hits[i];
//		}
//		
//		Arrays.sort(nodeInfos);
//		for(int i=0;i<nodes.length;++i){
//		
//			nodes[i] = nodeInfos[i].node;
//	
//		}
//		
//		for(int i=0;i<Global.nodeNum;++i){
//			hits[i] = (hits[i]>>>1);
//		}
		
		for(int i=0;i<nodes.length;++i){
			int left = i;//random.nextInt(nodes.length);
			int right = random.nextInt(nodes.length);
			int tmp =  nodes[left];
			nodes[left] = nodes[right];
			nodes[right] = tmp;
		}
	}
	

	
	/** 随机选择服务器 ,改变{@link #nextRoundServers}*/
	private void selectRandomServers() {
		
		Arrays.fill(selected, false);
		serverNodesSize = 0;
		
//		long sum = 0;
//		for(long hit : hits){
//			sum += hit;
//		}
//		bound[0] = hits[0]*1.0/sum;
//		for(int i=1;i<bound.length;++i){
//			
//			bound[i]= bound[i-1]+(hits[i]*1.0/sum);
//		}
		
		for(int i=0;i<Global.nodeNum;++i){
			nodeInfos[i].node = i;
			nodeInfos[i].hit = hits[i];
		}
	
		Arrays.sort(nodeInfos);
		serverNodesSize = selectNum;
		for(int i=0;i<selectNum;++i){
			serverNodes[serverNodesSize-1-i] = nodeInfos[i].node;
			System.out.print(nodeInfos[i].node+" ");
		}
		System.out.println();
		
//		int leftNum = selectNum;
//		// 随机选择
//		while(leftNum>0){
//			//int index = random.nextInt(nodes.length);
//			int node = -1;
//			double val = random.nextDouble();
//			for(int i=0;i<bound.length;++i){
//				if(val<=bound[i]){
//					node = i;
//					break;
//				}
//			}
//			if(node==-1){
//				System.out.println("in");
//			}
//			
//			// 没有被选过
//			if(!selected[node]){
//				selected[node] = true;
//				serverNodes[serverNodesSize++] = node;
//				leftNum--;
//			}
//		}
		
//		int leftNum = selectNum;
//		// 随机选择
//		while(leftNum>0){
//			int index = random.nextInt(nodes.length);
//			// 没有被选过
//			if(!selected[index]){
//				int node = nodes[index];
//				selected[index] = true;
//				serverNodes[serverNodesSize++] = node;
//				leftNum--;
//			}
//		}
	}
	int round = 0;
	@Override
	void optimize() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();

		selectServers();

		int minCost = Global.INFINITY;
		int maxUpdateNum = MAX_UPDATE_NUM;
	
		int bestFromNode = -1;
		int bestToNode = -1;
		int lastToNode = -1;
		int lastCost = minCost;
		while (!Global.isTimeOut()) {
			
			if (serverNodesSize == 0) {
				break;
			}
			
			// 可选方案
			lastToNode = bestToNode;
			bestFromNode = -1;
			bestToNode = -1;
			int leftMoveRound = Math.min(nodes.length,maxMovePerRound / serverNodesSize);
			int updateNum = 0;
			boolean found = false;
			
			for (int i = 0; i < serverNodesSize; ++i) {
				int fromNode = serverNodes[i];
				
				if(fromNode==lastToNode){
					continue;
				}

				// 服务器不移动
				if (Global.isMustServerNode[fromNode]) {
					continue;
				}

				for (int j=0;j<leftMoveRound;++j) {
					int toNode = nodes[j];
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}
					
					if (Global.isTimeOut()) {
						if(bestFromNode!=-1){
							moveBest(bestFromNode, bestToNode);
						}
						if(minCost<Global.minCost){
							updateBeforeReturn();
						}
						return;
					}
					
					int cost = getCostAfterMove(fromNode,toNode);
					if(cost<lastCost){
						if(hits[fromNode]>0){
							hits[toNode] += hits[fromNode];
						}else{
							hits[toNode] ++;
						}
						hits[fromNode] = 0;
					}
					
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
			
						updateNum++;
						if (updateNum == maxUpdateNum) {
							found = true;
							break;
						}
					}
				}
				
				if (found) {
					break;
				}

			}
			
			// not better
			if (bestFromNode == -1) {
				if (Global.IS_DEBUG) {
					System.out.println("not better");
					System.out.println("minCost:"+minCost);
				}
				
		
				updateBeforeReturn();
				
				
				// 再做中级
				optimizerMiddleLimit.optimize();
				optimizerMiddleLimit.updateBeforeReturn();
				
				// 再做高级
				optimizerMCMF.optimizeGlobalBest();
			
				selectRandomServers();
				minCost = Global.INFINITY;
				lastCost = minCost;
				maxUpdateNum = MAX_UPDATE_NUM;
				bestFromNode = -1;
				bestToNode = -1;
				
				if (Global.IS_DEBUG) {
				System.out.println("Global.minCost:"+Global.minCost);
				System.out.println("round:"+round++);
				System.out.println();
				}
				
				randomNodes();
			
				
			} else { // 移动
				
				hits[bestToNode] +=10;
				
				moveBest(bestFromNode, bestToNode);
				if (Global.IS_DEBUG) {
					System.out.println("better : " + minCost);
				}
				lastCost = minCost;
			}

			if (maxUpdateNum <= updateNum) {
				maxUpdateNum++;
				if (maxUpdateNum > MAX_UPDATE_NUM) {
					maxUpdateNum = MAX_UPDATE_NUM;
				}
			} else { // > updateNum
				maxUpdateNum--;
				if (maxUpdateNum < MIN_UPDATE_NUM) {
					maxUpdateNum = MIN_UPDATE_NUM;
				}
			}
			
		}
		
		if(minCost<Global.minCost){
			updateBeforeReturn();
		}
		
		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: "+ (System.currentTimeMillis() - t));
		}

	}

}
