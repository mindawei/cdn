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
//    		int nearestK = 1;//Global.consumerNum;
//    		int selectedNum = Global.consumerNum / 4;
//    		int maxMovePerRound = 2000;//8000;
//    		int maxUpdateNum = 1000;
//    		int minUpdateNum = 1000;
    		
    		int nearestK = Global.consumerNum ;
    		int selectedNum = Global.consumerNum / 4;
    		int maxMovePerRound = 2000;
    		int maxUpdateNum = 1000;
    		int minUpdateNum = 1000;
    		new GreedyOptimizerLeve2(nearestK,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		 maxMovePerRound = 1000;
    		 maxUpdateNum = 6;
     		 minUpdateNum = 3;
    		new GreedyOptimizerLeve3(nearestK,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    	}else if(Global.isNpHard){
    		int nearestK = 2;
    		int selectedNum = Global.consumerNum +1;
    		int maxMovePerRound = 2000;
    		int maxUpdateNum = 1000;
    		int minUpdateNum = 1000;
    		new GreedyOptimizerLeve1(nearestK,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();	
    	}else{
			new GreedyOptimizerLeve0().optimize();
		}
    	
    	new OptimizerMCMF(graphContent).optimize();
    	
   //    	if(Global.IS_DEBUG){
//    		Global.printBestSolution();
//      }
    
    	return Global.getBsetSolution();    
    }
}
