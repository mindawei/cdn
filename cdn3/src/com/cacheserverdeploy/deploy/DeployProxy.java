package com.cacheserverdeploy.deploy;

import java.util.Arrays;

public final class DeployProxy {

	/** 消费者信息 */
	private static final class ConsumerInfo implements Comparable<ConsumerInfo> {

		/** 消费者ID */
		int consumerId;
		/** 需求 */
		int demand;
		/** 优先级 */
		int priority;

		@Override
		public int compareTo(ConsumerInfo o) {

			// 优先级从大到小
			int off = o.priority - priority;
			if (off != 0) {
				return off;
			}

			// 需求从小到大
			off = demand - o.demand;
			if (off != 0) {
				return off;
			}

			// 标号从大到小
			return o.consumerId - consumerId;
		}
		
	}

	/** 初始可以付带宽的钱 */
	private static int initMoneyOfBandwidth;
	/** 剩余可以付带宽的钱 */
	private static int leftMoneyOfBandwidth;
	/** 上一轮带宽费 */
	private static int usedMoneyOfBandwidth = 0;

	/** 是否是服务器 */
	private static final boolean[] isServer = new boolean[Global.nodeNum];
	/** 是否是新服务器 */
	private static final boolean[] isNewServer = new boolean[Global.nodeNum];
	
	/** 第一轮是否是服务器 */
	private static final boolean[] isServerInFirstRound = new boolean[Global.nodeNum];

	/** 这一轮服务器 ,也是下一轮中上一轮的服务器 */
	private static final Server[] curServers = new Server[Global.nodeNum];

	/** 这一轮的输出等级，-1表示不输出 */
	private static final int[] outputServerLevel = new int[Global.nodeNum];

	/** 这一轮剩余的输出 */
	private static final int[] leftServerOutput = new int[Global.nodeNum];
	/** 剩余消费者需求 */
	private static final int[] leftConsumerDemands = new int[Global.consumerNum];

	/** 按评分排序的节点 */
	private static final int[] nodes = Global.nodes;

	/** 消费者信息 */
	private static final ConsumerInfo[] consumerInfos = new ConsumerInfo[Global.consumerNum];

	/** 初始化 */
	static void init() {
		for (int i = 0; i < Global.consumerNum; ++i) {
			consumerInfos[i] = new ConsumerInfo();
			consumerOutputs[i] = 0;
		}

		Zkw.init();
	}

	static void deploy() {

		// 重置带宽
		Global.resetEdgeBandWidth();
	
		// 本轮投资的总金额
		int investMoney = Global.leftMoney - 1;
		
		
		// 初始可以买服务器的钱
		int initMoneyOfServer = 0;
//		LogUtil.printLog("betterThanHe:" + Global.betterThanHe);
		// 保证带宽费
		if ((!Global.betterThanHe) && investMoney > usedMoneyOfBandwidth) {
			
			double buyServerRate = decideBuyServerRate();// 0.9;
//			LogUtil.printLog("buyServerRate:" + buyServerRate);
			
			initMoneyOfServer = (int) ((investMoney - usedMoneyOfBandwidth) * buyServerRate);
			if(initMoneyOfServer<0){
				initMoneyOfServer = 0;
			}
		}

		// 剩余可以买服务器的钱
		int leftMoneyOfServer = initMoneyOfServer;

//		int serverNum = 0;

		// 初始化
		for (int node : nodes) {

			// 重置
			isServer[node] = false;
			
			// 最后一轮:保留第一轮的服务器，其余全部卖掉
			if(Global.leftRound()==1&&!isServerInFirstRound[node]){
				// -1表示不输出，没有等级
				outputServerLevel[node] = -1;
				leftServerOutput[node] = 0;
				continue;
			}

			// 避免小带宽的
			if(Global.nodesMaxInputBandWidth[node] < Global.serverMaxOutputs[Global.serverLevelNum - 1]) {
				// -1表示不输出，没有等级
				outputServerLevel[node] = -1;
				leftServerOutput[node] = 0;
				continue;
			}

			if (curServers[node] != null) {
				// 之前安装过了
				isServer[node] = true;
				isNewServer[node] = false;
			} else {
				// 这一轮尝试安装
				int deployCost = Global.nodeDeployCosts[node] + Global.serverDeployCosts[Global.maxServerLevel];
				if (leftMoneyOfServer >= deployCost) {
					leftMoneyOfServer -= deployCost;
					isServer[node] = true;
					isNewServer[node] = true;
				}
				
			}

			if (isServer[node]) {
//				serverNum++;
				
				curServers[node] = new Server(node);
				outputServerLevel[node] = Global.maxServerLevel;
				leftServerOutput[node] = Global.maxServerOutput;

			} else {
				
				curServers[node] = null;
				outputServerLevel[node] = -1; // -1表示不输出，没有等级
				leftServerOutput[node] = 0;
				
			}
		}
//		LogUtil.printLog("serverNum: " + serverNum);

		// 减去买服务器的费用
		int usedMoneyOfBuyServers = initMoneyOfServer - leftMoneyOfServer;

		// 初始可以付带宽的钱
		initMoneyOfBandwidth = investMoney - usedMoneyOfBuyServers;
		leftMoneyOfBandwidth = initMoneyOfBandwidth;

		// 用买的服务器去服务消费者
		supply();

		usedMoneyOfBandwidth = initMoneyOfBandwidth - leftMoneyOfBandwidth;
		

//		LogUtil.printLog("investMoney: " + investMoney + " ,used:" + (usedMoneyOfBuyServers + usedMoneyOfBandwidth)
//				+ " ,buy servers:" + usedMoneyOfBuyServers + " ,pay bandwidth:" + usedMoneyOfBandwidth);

		// 设置服务器等级
		for(int node=0;node<Global.nodeNum;++node){
			Server server = curServers[node];
			if (server == null) {
				// 可能卖掉
				isServerInFirstRound[node]= false;
				continue;
			}
			
			if(Global.isFirstRound()){
				isServerInFirstRound[node] = true;
			}
			
			int serverLevel = outputServerLevel[node];
			server.setServerLevel(serverLevel);
		}

		// 更新结果
		Global.updateBestSolution(curServers);
	}

	/** 决定服务器购买比例 */
	private static double decideBuyServerRate() {

		if (Global.round <= 10) {
			return 0.98;
		}if (Global.round <= 40) {
			return 0.95;
		} else{
			return 0;
		}
	}

	/** 给消费者的需求 */
	/** 最小输出 */
	private static final int[] lowerDemnd = new int[Global.consumerNum];
	/** 最大输出 */
	private static final int[] upperDemnd = new int[Global.consumerNum];
	
	private static final int[] consumerOutputs = new int[Global.consumerNum];

	private static int off = 10;
	
	/** 决定这一轮的输出 */
	private static int decideCurConsumerOutputs(int consumerId) {

		// 需求
		int demand = Global.consumerDemands[consumerId];
		
		// 最大输入
		int maxInput = Global.consumerMaxInputBandWidth[consumerId];
		int consumerNode = Global.consumerNodes[consumerId];
		if(isServer[consumerNode] && leftServerOutput[consumerNode] > maxInput){
			maxInput = leftServerOutput[consumerNode];
		}
		
		// 之前那轮的输出
		int preDemnd = consumerOutputs[consumerId];

		// 这一轮提供
		int supply = 0;
		if (Global.consumerTeamIds[consumerId] == Global.teamID) { // 自己抢到的

//			if(Global.round<10){ // 前10轮hold住
				supply = preDemnd;
//			}else{
//				supply = preDemnd - off;
//			}
			
			if (supply < lowerDemnd[consumerId]) {
				supply = lowerDemnd[consumerId];
			}
			
		} else if (Global.consumerTeamIds[consumerId] == Global.emptyTeamID) { // 空的,没人抢过

			if (Global.isFirstRound()) { // 第一轮
				lowerDemnd[consumerId] = demand + 10;
				upperDemnd[consumerId] = demand + 40;
				supply = upperDemnd[consumerId];
			} else {
				lowerDemnd[consumerId] = demand + 10;
				upperDemnd[consumerId] = demand + 40;
				supply = upperDemnd[consumerId];
			}

		} else { // 敌人

			if (preDemnd > 0) { // 抢过要更新

				lowerDemnd[consumerId] = preDemnd + off;
				supply = lowerDemnd[consumerId];
				if (lowerDemnd[consumerId] > upperDemnd[consumerId]) {
					upperDemnd[consumerId] = lowerDemnd[consumerId] + 40;
					supply = upperDemnd[consumerId];
				}

			} else { // 没抢过

				lowerDemnd[consumerId] = demand + 10;
				upperDemnd[consumerId] = demand + 40;
				supply = upperDemnd[consumerId];
			}

		}

		// 检查
		if (supply < demand + 1) {
			supply = demand + 1;
		}

		if (supply > maxInput) {
			supply = maxInput;
		}

		// 当消费者的最大带宽-消费者的需求不大时，让其满流，最大流流入
		if (maxInput - demand <= 20) {
			supply = maxInput;
		}

		return supply;

	}

	/** 处理消费者 */
	private static void prepareConsumer() {

		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {

			ConsumerInfo consumerInfo = consumerInfos[consumerId];

			consumerInfo.consumerId = consumerId;
			// 调整需求策略
			consumerInfo.demand = decideCurConsumerOutputs(consumerId);
			// 重置这一轮的输出
			consumerOutputs[consumerId] = 0;

			// 设置优先级
			if (Global.consumerTeamIds[consumerId] == Global.teamID) {
				// 自己抢到的
				consumerInfo.priority = 2;
			} else if (Global.consumerTeamIds[consumerId] == Global.emptyTeamID) {
				consumerInfo.priority = 0;
			} else {
				// 对手抢到的
				consumerInfo.priority = 1;
			}

		}
		Arrays.sort(consumerInfos);
	}

	/** 一个消费者的带宽费 */
	private static int curBandWidthCost = 0;

	private static void supply() {

		prepareConsumer();

//		long t = System.currentTimeMillis();
//
//		int supplyConsumerNum = 0;
		// 从需求小的开始
		for (ConsumerInfo consumerInfo : consumerInfos) {

			if (consumerInfo.demand == 0) {
				continue;
			}

			if (System.currentTimeMillis() > Global.TIME_OUT_OF_NORMAL) {
				break;
			}

			int consumerId = consumerInfo.consumerId;
			leftConsumerDemands[consumerId] = consumerInfo.demand;

			// 已有的服务器来服务这些消费者
			curBandWidthCost = 0;
			while (transferCost(consumerId))
				;
			if (curBandWidthCost == 0) {
				continue;
			}

			// 退流： 无法满足的消费者,或者 不划算的，只计算一轮的带宽费用
			if (leftConsumerDemands[consumerId] > 0 ){//|| curBandWidthCost >= consumerInfo.profit) {

				for (Server server : curServers) {
					if (server != null) {

						// 遍历具体服务信息，删除无用的
						int index = 0;
						for (int j = 0; j < server.size; ++j) {
							ServerInfo serverInfo = server.serverInfos[j];
							if (serverInfo.consumerId != consumerId) {
								// 保留
								server.serverInfos[index++] = serverInfo;
							} else {
								// 归还
								serverInfo.returnBandWidth();
								leftServerOutput[server.node] += serverInfo.provideBandWidth;
							}
						}
						// 通过设置数组大小来删除
						server.size = index;
					}
				}

				// 归还带宽费用
				leftMoneyOfBandwidth += curBandWidthCost;

			} else {

				consumerOutputs[consumerId] = consumerInfo.demand;
//				supplyConsumerNum++;
			}

			if (leftMoneyOfBandwidth == 0) {
				break;
			}

		}
		
		final int usedMoneyOfBandwidth = initMoneyOfBandwidth - leftMoneyOfBandwidth;

//		LogUtil.printLog("before zkw usedMoneyOfBandwidth:" + usedMoneyOfBandwidth);
//		LogUtil.printLog("supplyConsumerNum:" + supplyConsumerNum);
//		LogUtil.printLog("consumer use time:" + (System.currentTimeMillis() - t));

//		t = System.currentTimeMillis();

		Server[] zkwServers = Zkw.optimize(consumerOutputs, outputServerLevel, usedMoneyOfBandwidth);
//		LogUtil.printLog("Zkw use time:" + (System.currentTimeMillis() - t));

		if (zkwServers != null) {

//			LogUtil.printLog("Zkw cost:" + Zkw.zkwCost);

			for (int node = 0; node < Global.nodeNum; ++node) {
				if (zkwServers[node] != null) {
					curServers[node] = zkwServers[node];
				} else { // == null
							// 保留之前的服务器
					if (isServer[node] && !isNewServer[node]) {
						curServers[node] = new Server(node);
					} else {
						curServers[node] = null;
					}
				}
			}

			// 更新剩余的钱
			leftMoneyOfBandwidth = initMoneyOfBandwidth - Zkw.zkwCost;
		}

		// 检查没有输出的服务器，已经花钱买了，保留
		final int certainConsumerId = 0;
		final int zeroDeamnd = 0;
		for (int node : nodes) {
			if (isServer[node] && curServers[node].getDemand() == 0) {

				if (isNewServer[node]) { // 新的不保留

					curServers[node] = null;

				} else { // 旧的保留

					// 输出一条0的进行保留
					// 添加到当前服务器记录中
					tmpViaNodesSize = 0;
					int tNode = node;
					// 从服务器开始
					while (tNode != -1) {
						tmpViaNodes[tmpViaNodesSize++] = tNode;
						tNode = Global.allPreNodes[certainConsumerId][tNode];
					}
					int[] viaNodes = new int[tmpViaNodesSize];
					for (int i = 0; i < tmpViaNodesSize; ++i) {
						// 逆向，从消费者开始
						viaNodes[i] = tmpViaNodes[tmpViaNodesSize - 1 - i];
					}
					ServerInfo serverInfo = new ServerInfo(certainConsumerId, zeroDeamnd, viaNodes);
					curServers[node].addServerInfo(serverInfo);

				}
			}
		}

	}

	private static final int[] tmpViaNodes = new int[Global.nodeNum];
	private static int tmpViaNodesSize = 0;
	private static final int inf = Global.INFINITY;

	/**
	 * 将起始点需求分发到目的地点中，会改变边的流量<br>
	 * 
	 * @return 是否需要继续转移
	 */
	private static boolean transferCost(int consumerId) {

		// 还剩多少个服务节点未使用
		int leftServerNodeNum = 0;

		for (int i = 0; i < Global.nodeNum; ++i) {
			vis[i] = false;
			costs[i] = inf;
			if (isServer[i] && leftServerOutput[i] > 0) {
				leftServerNodeNum++;
			}
		}

		int fromNode = Global.consumerNodes[consumerId];

		// queue
		qSize = 0;

		// 自己到自己的距离为0
		costs[fromNode] = 0;
		preNodes[fromNode] = -1;

		queAdd(fromNode);

		// 是否找的一条减少需求的路
		boolean fromDemandSmaller = false;

		int node;
		int applyDemand;

		while (leftServerNodeNum > 0 && qSize > 0) {

			// 寻找下一个最近点
			node = quePoll();

			// 访问过了
			vis[node] = false;

			// 是服务器 && 申请的流量 = 本身需求和服务器还能够提供输出的最小值
			if (isServer[node] && leftServerOutput[node] > 0
					&& (applyDemand = Math.min(leftConsumerDemands[consumerId], leftServerOutput[node])) > 0) {
				int usedDemand = Global.getBandWidthCanbeUsed(applyDemand, node, preNodes);
				// 可以消耗
				if (usedDemand > 0) {

					int bandWidthCost = costs[node] * usedDemand;

//					if (bandWidthCost < 0) {
//						LogUtil.printLog("error:bandWidthCost" + bandWidthCost + " = " + costs[node] + " * " + usedDemand);
//					}

					if (leftMoneyOfBandwidth >= bandWidthCost) {
						// 消耗钱
						leftMoneyOfBandwidth -= bandWidthCost;
						// 该节点的带宽
						curBandWidthCost += bandWidthCost;

						// 消耗带宽
						Global.useBandWidthDirectly(usedDemand, node, preNodes);

						// 更新剩余的输出能力
						leftServerOutput[node] -= usedDemand;
						// 更新消费者需求
						leftConsumerDemands[consumerId] -= usedDemand;

						// 添加到当前服务器记录中
						tmpViaNodesSize = 0;
						int tNode = node;
						// 从服务器开始
						while (tNode != -1) {
							tmpViaNodes[tmpViaNodesSize++] = tNode;
							tNode = preNodes[tNode];
						}
						int[] viaNodes = new int[tmpViaNodesSize];
						for (int i = 0; i < tmpViaNodesSize; ++i) {
							// 逆向，从消费者开始
							viaNodes[i] = tmpViaNodes[tmpViaNodesSize - 1 - i];
						}
						ServerInfo serverInfo = new ServerInfo(consumerId, usedDemand, viaNodes);
						curServers[node].addServerInfo(serverInfo);

						fromDemandSmaller = true;
						break;
					}
				}
			}

			// 更新
			for (int toNode : Global.connections[node]) {
				// 反向流量
				Edge edge = Global.graph[toNode][node];
				if (edge.leftBandWidth == 0) {
					continue;
				}
				int newCost = costs[node] + edge.cost;
				if (newCost < costs[toNode]) {
					costs[toNode] = newCost;
					// 添加路径
					preNodes[toNode] = node;
					if (!vis[toNode]) {
						vis[toNode] = true;
						queAdd(toNode);
					}
				}
			}
		}

		if (leftConsumerDemands[consumerId] > 0 && fromDemandSmaller && leftServerNodeNum > 0
				&& leftMoneyOfBandwidth > 0) {
			return true;
		} else {
			return false;
		}
	}

	private static final boolean[] vis = new boolean[Global.nodeNum];
	private static final int[] costs = new int[Global.nodeNum];
	private static final int[] preNodes = new int[Global.nodeNum];

	// 实现最小堆
	private static final int[] queue = new int[Global.nodeNum];
	private static int qSize = 0;

	/** 添加节点 */
	private static final void queAdd(int x) {
		int k = qSize;
		qSize = k + 1;
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			int e = queue[parent];
			if (costs[x] > costs[e])
				break;
			queue[k] = e;
			k = parent;
		}
		queue[k] = x;
	}

	/** 弹出节点 */
	private static final int quePoll() {
		int s = --qSize;
		int result = queue[0];
		int x = queue[s];
		if (s != 0) {
			// 下沉操作
			int k = 0;
			int half = qSize >>> 1;
			while (k < half) {
				int child = (k << 1) + 1;
				int c = queue[child];
				int right = child + 1;
				if (right < qSize && costs[c] > costs[queue[right]])
					c = queue[child = right];
				if (costs[x] <= costs[c])
					break;
				queue[k] = c;
				k = child;
			}
			queue[k] = x;
		}
		return result;
	}

}
