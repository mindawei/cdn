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
    	
    	if(Global.isNpHardest){
    		int nearestK = 1;
    		int selectedNum = Global.consumerNum / 4;
    		int maxMovePerRound = 2000;
    		int maxUpdateNum = 6;
    		int minUpdateNum = 3;
    		new GreedyOptimizerLeve2(nearestK,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    	}else if(Global.isNpHard){
    		int nearestK = Global.consumerNum;
    		int selectedNum = Global.consumerNum / 4;
    		int maxMovePerRound = 100000000;
    		int maxUpdateNum = 100;
    		int minUpdateNum = 100;
    		new GreedyOptimizerLeve1(nearestK,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();	
    	}else{
			new GreedyOptimizerLeve0().optimize();
		}
    	
    	MCMF.optimizeBestServers();
    
//    	if(Global.IS_DEBUG){
//    		Global.printBestSolution();
//      }
    
    	return Global.getBsetSolution();    
    }
}
