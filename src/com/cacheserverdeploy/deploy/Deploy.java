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
     
    		int maxMovePerRound = 1200;
    		int maxUpdateNum = 9;
    		int minUpdateNum = 6;
    		new OptimizerSimpleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		maxMovePerRound = 200;
    		maxUpdateNum = 2;
     		minUpdateNum = 2;
     		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    	}else if(Global.isNpHard){
    		
    		int nearestK = 5 ;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
    		
    	//	int maxMovePerRound = 2000;
    	//	int maxUpdateNum = 10;
    	//	int minUpdateNum = 10;
    	//	new OptimizerSimpleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		int maxMovePerRound = 1000;
    		int maxUpdateNum = 30;
    		int minUpdateNum = 20;
    		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    		optimizerMCMF.optimizeGlobalBest();
    		maxMovePerRound = 800;
    		maxUpdateNum = 2;
    		minUpdateNum = 2;
			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
		
    	}else{

//			new OptimizerMiddle().optimize();
//			optimizerMCMF.optimizeGlobalBest();	
//			int nearestK = 5;
//    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
//    		int maxMovePerRound = 500*1000;
//    		int maxUpdateNum = 1000;
//    		int minUpdateNum = 1000;
//			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
		
//    		int nearestK = 1; 
//    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
//
//    		int simpleMaxMovePerRound = 2000;
//    		int simpleMaxUpdateNum = 1;
//    		
//    		int middleMaxMovePerRound = 1000;
//    		int middleMaxUpdateNum = 1;
//    		int selectedNum = Global.consumerNum/4 ;
    		
    		// k = 1 30706
    		// k = 2 30899
    		// k = 3 30890
    		
    		int nearestK = 1; 
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);

    		// 2500 = 
    		// 2000 = 30692
    		// 1500 = 30702
    		// 1250 = 30692,30647
    		// 1000 = 30694,30629,30729,30588,30843,30740
    		// 800  = 30844
    	
    	
    		int simpleMaxMovePerRound = 1000;
    		int simpleMaxUpdateNum = 1;
    		
    		// = 2000 30706
    		// = 1500 30694,30629
    		// = 1350 30845
    		// = 1200 30783
    		// = 1000 30706
    		// = 750 30805
    		// = 500 30805
    		// = 0  32613
    		
    		// 1000 29155
    		// 800 29160
    		// 
    		int middleMaxMovePerRound = 1000;
    		
    		int middleMaxUpdateNum = 1;
    		int selectedNum = Global.consumerNum/5 ;
    		
    		new OptimizerRandomLimit(optimizerMCMF,nodes, 
    				selectedNum,
    				simpleMaxMovePerRound,simpleMaxUpdateNum,
    				middleMaxMovePerRound,middleMaxUpdateNum).optimize();

    	}
    	
    	
    	optimizerMCMF.optimizeGlobalBest();
    	
    	
    	
//    	if(Global.IS_DEBUG){
//    		Global.printBestSolution();
//      }
    
    	return Global.getBsetSolution();    
    }
}
