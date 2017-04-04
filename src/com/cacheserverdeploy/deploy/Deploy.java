package com.cacheserverdeploy.deploy;

public class Deploy{
    /**
     * 你需要完成的入口
     * <功能详细描述>
     * @param graphContent 用例信息文件
     * @return [参数说明] 输出结果信息
     * @see [类、类#方法、类#成员]
     */
    public static String[] deployServer(String[] graphContent)
    {
        /**do your work here**/
    	Parser.buildNetwork(graphContent);
    
    	Global.init();
    	OptimizerMCMF optimizerMCMF = new OptimizerMCMF(graphContent);
    	if(Global.isNpHardest){
    		
    		int nearestK = Global.consumerNum ;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
     
    		int selectedNum = Global.consumerNum / 4 + 5;
    		int maxMovePerRound = 1800;
    		int maxUpdateNum = 30;
    		int minUpdateNum = 20;
    		new OptimizerSimpleLimit(nodes,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		maxMovePerRound = 400;
    		maxUpdateNum = 6;
     		minUpdateNum = 3;
     		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    	}else if(Global.isNpHard){
    		
    		int nearestK = 2 ;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
    		//int[] nodes = NodesSelector.selectMoveNodesByRank(nearestK);
    		// 68559
    		int maxMovePerRound = 1000;
    		int maxUpdateNum = 200;
    		int minUpdateNum = 100;
    		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    		// 68313
    		optimizerMCMF.optimizeGlobalBest();
	
    		
//    		int minRank = 1000000;
//    		int maxRank = -1;
//    		for(Server server : Global.getBestServers()){
//        		if(server==null){
//        			break;
//        		}
//        		
//        		System.out.println(NodesSelector.nodeFreqs2[server.node] );
//        		if(NodesSelector.nodeFreqs2[server.node].rank<minRank){
//        			minRank = NodesSelector.nodeFreqs2[server.node].rank;
//        		}
//        		if(NodesSelector.nodeFreqs2[server.node].rank >maxRank){
//        			maxRank = NodesSelector.nodeFreqs2[server.node].rank;
//        		}
//        		
//        		
//        	}
    		
    		// minRank:2 maxRAnk:180
    		// 
    		//System.out.println("minRank:"+minRank+" maxRAnk:"+maxRank);
    		
    		maxUpdateNum = 6;
    		minUpdateNum = 3;
			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
//			
    	}else{
//
//			new OptimizerMiddle().optimize();
//			optimizerMCMF.optimizeGlobalBest();		
//			
//			int nearestK = 5;
//    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
//    	
//    		int maxMovePerRound = 3000;
//    		int maxUpdateNum = 1000;
//    		int minUpdateNum = 1000;
//			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		// 29082
			new OptimizerMiddle().optimize();
			optimizerMCMF.optimizeGlobalBest();		
			
			int nearestK = 5;
    		// int[] nodes = NodesSelector.selectMoveNodes(nearestK);
    		//int[] nodes = NodesSelector.selectAllNodes(nearestK);
    		int[] nodes = NodesSelector.selectAllNodesInReverse(nearestK);
    		
    		
    		int maxMovePerRound = 500*1000;
    		int maxUpdateNum = 1000;
    		int minUpdateNum = 1000;
			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
		
//    		// 22276
//    		int nearestK = 5; /// 4
//    		//int[] nodes = NodesSelector.selectMoveNodes(nearestK);
//    		int[] nodes = NodesSelector.selectAllNodes(nearestK);
//
//    		int maxMovePerRound = 3000;
//    		int maxUpdateNum = 20;
//    		int minUpdateNum = 12;
//    		int selectedNum = Global.consumerNum / 4;
//    		new OptimizerRandomLimit(optimizerMCMF,nodes, selectedNum, maxMovePerRound, maxUpdateNum, minUpdateNum).optimize();
	
    		
    	}
    	
    	
    	optimizerMCMF.optimizeGlobalBest();
    	
    	
    	
//    	if(Global.IS_DEBUG){
//    		Global.printBestSolution();
//      }
    
    	return Global.getBsetSolution();    
    }
}
