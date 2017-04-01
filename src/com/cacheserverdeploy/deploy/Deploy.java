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
//    		int nearestK = 1;//Global.consumerNum;
//    		int selectedNum = Global.consumerNum / 4;
//    		int maxMovePerRound = 2000;//8000;
//    		int maxUpdateNum = 1000;
//    		int minUpdateNum = 1000;
    		
    		int nearestK = Global.consumerNum ;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
        
    		int selectedNum = Global.consumerNum / 4;
    		int maxMovePerRound = 2000;
    		int maxUpdateNum = 9;
    		int minUpdateNum = 6;
//    	
//    		new GreedyOptimizerLeve2(nodes,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		maxMovePerRound = 1000;
    		maxUpdateNum = 6;
     		minUpdateNum = 3;
    		new GreedyOptimizerLeve3(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    	}else if(Global.isNpHard){
    		
    		int nearestK = 2 ;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
        	
    		int selectedNum = Global.consumerNum +1;
    		int maxMovePerRound = 2000;
    		int maxUpdateNum = 1000;
    		int minUpdateNum = 1000;
    		new GreedyOptimizerLeve1(nodes,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		optimizerMCMF.optimize();
			new GreedyOptimizerLeve4(optimizerMCMF,nodes,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimizeMCMF();
    	}else{
    		int nearestK = 2;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
    		
    		int selectedNum = Global.consumerNum +1;
    		int maxMovePerRound = 2000;
    		int maxUpdateNum = 1000;
    		int minUpdateNum = 1000;
			new GreedyOptimizerLeve0().optimize();
			optimizerMCMF.optimize();
			new GreedyOptimizerLeve4(optimizerMCMF,nodes,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimizeMCMF();
		}
    	
    	
    	optimizerMCMF.optimize();
    	
//    	if(Global.IS_DEBUG){
//    		Global.printBestSolution();
//      }
    
    	return Global.getBsetSolution();    
    }
}
