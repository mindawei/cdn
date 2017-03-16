package com.cacheserverdeploy.deploy;

import java.util.LinkedList;
import java.util.List;

public class Deploy
{
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
       
    	if(Global.IS_DEBUG){
        	Global.printNetworkInfo();
        }
    	
    	Global.initSolution();
     	
    	// Global.initRest();
    	
    	List<Optimizer> optimizers = new LinkedList<Optimizer>();
    	
    	// 局部最优
    	optimizers.add(new GreedyOptimizer());
    	optimizers.add(new HeuristicOptimizer());
    	
    	// 1 边界合并
    	for(Optimizer optimizer : optimizers){
    		// Global.reset();
    		optimizer.optimize();
    		if(Global.IS_DEBUG){
    			System.out.println(optimizer.getClass().getSimpleName());
    			System.out.println(Global.getTotalCost());
      		}
    	}
    	
    	String[] solution = Global.getBestSolution();
    	return solution;    
    }

}
