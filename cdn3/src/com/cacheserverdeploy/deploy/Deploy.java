package com.cacheserverdeploy.deploy;

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
    
    	Global.round++;
    	    	
    	Global.startTime = System.currentTimeMillis();
    	
    	Global.deal(graphContent);
//		LogUtil.printLog("leftMoney: "+Global.leftMoney);
	
		if(Global.isFirstRound()){
			DeployProxy.init();
			Global.TIME_OUT_OF_NORMAL = Global.startTime +  7000L; 
		}else{
			Global.TIME_OUT_OF_NORMAL = Global.startTime +  6000L; 
		}
		// 费用流超时
		Global.TIME_OUT_OF_MCMF = Global.startTime + 8700L;
		
		DeployProxy.deploy();
		
		String[] bestSolutions = Global.getBsetSolution();
	
//		LogUtil.printLog("soloution:\n"+bestSolutions[0]+"\n");
//		LogUtil.printLog("timeCost :"+(System.currentTimeMillis()-Global.startTime));
		
		return bestSolutions;
    }

}
