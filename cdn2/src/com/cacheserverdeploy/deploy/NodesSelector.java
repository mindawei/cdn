package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

public final class NodesSelector {

	private final class Info {
		/** 花费 */
		final int cost;
		/** 前向指针 */
		final int[] preNodes;
		/** 目标节点 */
		final int toNode;

		public Info(int cost, int[] preNodes, int toNode) {
			super();
			this.cost = cost;
			this.preNodes = preNodes;
			this.toNode = toNode;
		}
	}

	private int[] freqs1;
	private int[] freqs2;
	private int[] freqs3;
	private int[] freqs4;
	private int[] freqs1over4; // 1/4
	private int[] freqs1over3; // 1/3
	private int[] freqs1over2; // 1/2
	private int[] freqsAll;
	
	private static final int varNum = 19;
	
	/** 归一化 */
	private void normalization(){
		for(int col =0;col<varNum;++col){
			double min = Global.INFINITY;
			double max = -1;
			for(NodeInfo nodeInfo: nodeInfos){
				if(nodeInfo.vars[col]<min){
					min = nodeInfo.vars[col];
				}
				if(nodeInfo.vars[col]>max){
					max = nodeInfo.vars[col];
				}
			}
			
			double off = max - min;
			if(off==0){
				for(NodeInfo nodeInfo: nodeInfos){
					nodeInfo.vars[col] = 0;
				}
			}else{
				for(NodeInfo nodeInfo: nodeInfos){
					nodeInfo.vars[col] = (nodeInfo.vars[col]-min)/off;
				}
			}
		}
	}
	

	private final class NodeInfo implements Comparable<NodeInfo>{
	
		private final int node;
		double[] vars = new double[varNum];
		
		double score;
		
		public NodeInfo(int node) {
			this.node = node;
			double deployCost = Global.nodeDeployCosts[node];
			int degree = Global.connections[node].length;
			
			double output = 0;
			for(int toNode : Global.connections[node]){
				output+=Global.graph[node][toNode].initBandWidth;
			}
			
			double consumerDemndsSum = 0;
			double consumerCostSum = 0;
		
			double isConsumerNode = 0;
			double consumerCostMin = Global.INFINITY;
			double consumerCostMax = 0;
			double consumerDemandNear = 0;
			double consumerDemandFar = 0;
			for(int consumerID : Global.consumerIds){
				if(Global.consumerNodes[consumerID]==node){
					isConsumerNode =1;
				}
				
				int minBindWidth = Global.consumerDemands[consumerID];
				int[] preNodes = Global.allPreNodes[consumerID];
				
				int cost = 0;
				int node1 = node;
				int node0 = preNodes[node1];
				while(node0!=-1){
					Edge edge = Global.graph[node1][node0];
					if(edge.initBandWidth<minBindWidth){				
						minBindWidth = edge.initBandWidth;
					}
					cost+=edge.cost;
					node1 = node0;
					node0 = preNodes[node0];
				}
				
				consumerCostSum+=cost;
				consumerDemndsSum+=minBindWidth;
				if(cost<consumerCostMin){
					consumerCostMin = cost;
					consumerDemandNear = minBindWidth;
				}
				if(cost>consumerCostMax){
					consumerCostMax = cost;
					consumerDemandFar = minBindWidth;
				}
				
			}
		
			double consumerCostAvg = consumerCostSum / Global.consumerNum;
			
			double freq1 = freqs1[node];
			double freq2 = freqs2[node];
			double freq3 = freqs3[node];
			double freq4 = freqs4[node];
			double freq1over4 = freqs1over4[node];
			double freq1over3 = freqs1over3[node];
			double freq1over2 = freqs1over2[node];
			double freqAll= freqsAll[node];
			
			// this.tag = isServer[node];
			
			/** 节点部署费用 */
			vars[0] = deployCost;
			/** 节点的度 */
			vars[1] = degree;
			/** 出去的带宽*/
			vars[2] = output;
			
			/** 是否是消费者节点 */
			vars[3] = isConsumerNode;
			
			/** 到所有消费者的带宽之和 */
			vars[4] = consumerDemndsSum;
			/** 最近消费者的带宽 */
			vars[5] = consumerDemandNear;
			/** 最远消费者的带宽 */
			vars[6] = consumerDemandFar;
		
			/** 到所有消费者最短距离之和*/
			vars[7] = consumerCostSum;
			/** 最近消费者的费用 */
			vars[8] = consumerCostMin;
			/** 平均消费者的费用*/
			vars[9] = consumerCostAvg;
			/** 最远消费者的费用 */
			vars[10] = consumerCostMax;
			
			/** 链接频率 */
			vars[11] = freq1;
			vars[12] = freq2;
			vars[13] = freq3;
			vars[14] = freq4;
			vars[15] = freq1over4; // 1/4
			vars[16] = freq1over3; // 1/3
			vars[17] = freq1over2; // 1/2
			vars[18] = freqAll;
			
		}

		@Override
		public int compareTo(NodeInfo o) {
			if(score<o.score){
				return 1;
			}else if(score==o.score){
				return 0;
			}else{
				return -1;
			}
		}
	}

	private NodeInfo[] nodeInfos;
	
	int[] select() {
		
		freqs1 = new int[Global.nodeNum];
		initFreqs(freqs1,1);
		
		freqs2 = new int[Global.nodeNum];
		initFreqs(freqs2,2);
		
		freqs3 = new int[Global.nodeNum];
		initFreqs(freqs3,3);
		
		freqs4 = new int[Global.nodeNum];
		initFreqs(freqs4,4);
		
		freqs1over4 = new int[Global.nodeNum]; // 1/4
		initFreqs(freqs1over4,Global.consumerNum/4);
		
		freqs1over3 = new int[Global.nodeNum]; // 1/3
		initFreqs(freqs1over3,Global.consumerNum/3);
		
		freqs1over2 = new int[Global.nodeNum]; // 1/2
		initFreqs(freqs1over2,Global.consumerNum/2);
		
		freqsAll = new int[Global.nodeNum];
		initFreqs(freqsAll,Global.consumerNum);
		
		nodeInfos = new NodeInfo[Global.nodeNum];	
		
		
		// 创建基本数据
		for(int node =0;node<Global.nodeNum;++node){
			nodeInfos[node] = new NodeInfo(node);
			 
		}
		
		// 归一化
		normalization();
		
		// 计算分数
		calculateScore();

		// 排序
		Arrays.sort(nodeInfos);
		
		// 输出结果
		int[] nodes = new int[Global.nodeNum];
		for(int i =0;i<Global.nodeNum;++i){
			nodes[i] = nodeInfos[i].node;
		}
		return nodes;
		
	}
		
	private final void initFreqs(int[] freqs,int nearestK) {

		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {

			int fromConsumerNode = Global.consumerNodes[consumerId];

			// 最大堆
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>(nearestK + 1, new Comparator<Info>() {
				@Override
				public int compare(Info o1, Info o2) {
					return o2.cost - o1.cost;
				}
			});

			for (int toConsumerNode : Global.consumerNodes) {
				// 自己不到自己
				if (toConsumerNode == fromConsumerNode) {
					continue;
				}
				int cost = Global.allCost[consumerId][toConsumerNode];
				int[] preNodes = Global.allPreNodes[consumerId];
				Info info = new Info(cost, preNodes, toConsumerNode);
				priorityQueue.add(info);
				if (priorityQueue.size() > nearestK) {
					// 去掉最大的，保留 k个最小的
					priorityQueue.poll();
				}
			}

			// 统计频率
			Info info = null;
			while ((info = priorityQueue.poll()) != null) {
				int[] preNodes = info.preNodes;
				for (int node = info.toNode; node != -1; node = preNodes[node]) {
					freqs[node]++;
				}
			}
		}
	}
	
	// 20xm   20x20
	private final double[][] w1 = {{3.818577,-1.114248,-0.190677,0.577248,3.265212,5.403979,-0.792329,-2.438975,2.052548,0.562693,0.750932,1.310094,0.992635,0.040861,-1.527403,0.837756,-0.277287,1.399190,-0.196142,-1.134649},
			{-3.784013,0.884142,0.600727,-4.949908,0.182481,-7.159309,-2.687147,-2.074221,-2.172379,-2.665542,-2.170743,3.891180,-0.512351,0.496036,-0.530057,1.153465,2.750862,-2.809599,-1.347134,-1.389751},
			{-0.525505,-1.454653,0.544382,-0.303429,1.623569,-2.751492,0.570353,-0.378094,-1.843626,-9.414315,-0.798751,1.484127,-0.766722,0.647768,-0.496054,0.255803,-0.844007,1.788271,1.103622,-0.133512},
			{-0.223666,1.002208,0.993679,1.988954,1.539331,0.028963,-0.328212,-0.472247,-1.160631,-7.718531,1.312526,-0.890242,0.747323,0.700730,-0.563066,0.193374,-1.037432,1.237461,0.773265,-0.477113},
			{3.624530,2.594752,-0.231391,-0.344640,-1.513679,1.089648,-0.760248,-0.813918,1.072862,0.408232,-1.342509,-2.494522,0.703252,-2.149315,-2.160430,0.395263,0.222220,-2.469099,-0.963731,3.390309},
			{-1.542651,-1.459117,-0.273049,2.181935,0.046560,-1.833073,0.575883,1.181765,0.988344,0.887462,-0.259115,-1.159201,0.390958,0.609091,0.481880,0.572664,-1.521900,-2.210793,0.162914,-2.499069},
			{-1.573954,-1.919288,1.042174,-3.813475,-2.608357,4.021176,0.828056,1.011002,-0.358516,-2.876510,-1.813953,-2.777611,-0.829913,1.299178,1.400422,-0.314814,3.036891,0.471393,-0.232729,3.220969},
			{-0.425564,0.488184,0.985156,1.348754,-1.035616,-1.034467,0.362984,0.885693,-2.235653,0.071510,-0.032926,-1.970443,-1.338774,-0.439087,-1.422761,-0.130159,0.684479,-1.163452,-1.371485,-0.723294},
			{2.619533,-1.278197,-1.616391,-2.302780,0.195110,1.179195,-1.293003,0.643304,0.627143,1.115072,-1.819903,-0.140846,-0.261727,-0.855094,-1.227504,0.968199,1.413876,2.664793,-0.318568,0.202005},
			{-0.161005,-1.158767,0.649307,0.115556,-0.495371,1.301036,0.440012,-1.259476,1.958979,1.033118,-0.899870,-1.256643,-1.363989,-1.461723,0.259665,-0.316222,1.458663,-0.480554,-1.153656,-0.156991},
			{2.090021,-0.260627,-0.681741,-1.753643,-0.412627,2.854636,-2.070632,-1.218457,-0.185676,1.671094,0.847770,-0.736744,-1.145122,0.775352,-0.963065,0.874648,2.253799,2.468432,0.911462,-1.507860},
			{1.412677,1.031716,-0.332287,-0.494407,0.400065,4.316827,-0.990323,-0.369908,-0.561565,-1.313994,-0.387896,0.760894,-0.200341,0.476973,-0.285312,0.184197,0.967598,0.668107,1.156810,-1.760884},
			{-1.397416,-1.161482,0.598541,-0.887484,0.626315,2.228362,-0.545426,-0.589991,2.082146,-4.982033,-0.158744,-2.340263,-0.506562,0.372393,1.594033,0.970296,1.278371,-1.326262,0.495913,0.008099},
			{-1.105662,2.228588,0.267537,-1.854109,-1.912162,-0.953530,0.574233,-0.084603,2.024273,-4.723883,0.344051,-1.527811,-0.684164,-0.559175,1.489744,0.599377,0.075362,1.588535,0.471581,0.327007},
			{0.297095,2.635038,0.886031,1.826853,-0.884998,1.352938,0.768640,1.240660,1.755528,-1.549527,-0.803581,0.301127,-0.757083,-1.154028,1.150030,0.603449,-0.132389,4.229189,1.311877,-3.177763},
			{-1.939803,2.104316,-0.756373,1.989305,-1.659215,-0.323209,0.091050,2.107500,1.291348,0.107388,-0.120362,1.544711,0.635890,-1.243709,2.739338,1.090895,0.096714,0.259185,0.821434,-1.499811},
			{0.081969,-0.258530,1.390841,-2.187833,0.290416,-1.008038,0.866177,-0.869049,-0.014923,-1.805273,-0.689786,-0.844926,-0.444903,0.417956,0.363558,-1.160064,1.941135,1.389038,1.104385,0.823684},
			{-0.094153,-1.012414,-0.794030,-0.306755,0.264898,-1.687506,-1.426460,-0.919798,-0.985579,-0.409459,1.046341,-0.068241,-0.750242,0.127133,0.267362,-0.961459,1.789250,2.193764,0.551344,-1.806583},
			{1.005893,-1.302752,0.570932,-1.217734,-1.175044,1.159086,-0.787679,1.579520,0.664909,-1.277439,1.369600,-3.247014,0.798954,1.169268,-0.383645,0.472819,2.015221,1.895040,-0.329496,-0.045494},
			{0.040301,-2.534159,-0.310154,0.368905,-0.895294,2.040624,-0.212897,1.018500,-1.082862,-1.415163,-0.676501,-3.952057,-0.414053,0.319904,-0.147838,1.356111,-0.217938,2.323733,0.014645,0.785493}};

	
	// (m+1)x2  = 21x2
	private final double[][] w2 = {{1.397455,-2.923974},
			{-3.014180,3.056460},
			{1.717975,-2.977732},
			{-0.226652,0.407284},
			{-4.855861,4.452024},
			{2.530132,-2.528496},
			{-5.689305,5.759453},
			{0.042562,2.196920},
			{-1.881650,2.220534},
			{-2.372392,2.193217},
			{11.929135,-12.200110},
			{-0.771533,1.295279},
			{4.083772,-3.849957},
			{0.323556,-0.547305},
			{-1.233402,-0.786762},
			{1.695959,-2.518579},
			{1.640968,-0.077057},
			{5.234888,-3.969174},
			{-3.631259,3.138500},
			{0.413297,1.206663},
			{-2.910673,3.129565}};

	
	/** 计算分数 */
	private final void calculateScore(){
		// 初始化矩阵 1200x19
		double[][] a = new double[Global.nodeNum][varNum];
		for(int i=0;i<Global.nodeNum;++i){
			double[] vars = nodeInfos[i].vars;
			for(int j=0;j<vars.length;++j){
				a[i][j]= vars[j];
			}
		}
		
		// 扩展一列 1200 x 20
		a = expend1(a);
		// System.out.println(a.length+" "+a[0].length);
		
		// 矩阵相乘 1200x20 x 20xm  = 1200xm
		a = mutilMatrix(a, w1); 
		
		// sigmod
		sigmod(a);
		
		// 扩展一列 1200x(m+1)
		a = expend1(a);
		
		// 矩阵相乘 1200x(m+1) x (m+1)x2  = 1200x2
		a = mutilMatrix(a,w2);
		
		// sigmod
		sigmod(a);
		
		for(int i=0;i<Global.nodeNum;++i){
			nodeInfos[i].score = a[i][1]; // 放服务器的概率
		}
		
	}
	
	/** 最前面加一列 */
	private final  double[][] expend1(double[][] a){
		int n = a.length;
		int m = a[0].length;
		double[][] b = new double[n][m+1];
		for(int i=0;i<n;++i){
			for(int j=0;j<m;++j){
				b[i][j+1] = a[i][j];
			}
		}
		for(int i=0;i<n;++i){
			b[i][0] = 1;
		}
		return b;
		
	}
	
	/** 矩阵相乘 */
	
	// 1200x(m+1) x 2x(m+1)  = 
	private final double[][] mutilMatrix(double[][] a, double[][] b) {
		int n = a.length;
		int m = b[0].length;
		double c[][] = new double[n][m];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				double temp = 0;
				for (int k = 0; k < a[0].length; k++) {
					temp += a[i][k] * b[k][j];
				}
				c[i][j] = temp;
			}
		}
		return c;
	}
	
	/** simgmod */
	private final void sigmod(double[][] arr){
		for(int i=0;i<arr.length;++i){
			for(int j=0;j<arr[0].length;++j){
				arr[i][j] = 1.0/(1+Math.exp(-arr[i][j]));
			}
		}
	}
	
}
