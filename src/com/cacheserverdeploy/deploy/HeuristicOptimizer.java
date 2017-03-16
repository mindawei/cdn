package com.cacheserverdeploy.deploy;

import java.util.Arrays;

/**
 * 启发式搜素
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public class HeuristicOptimizer extends Optimizer {

	private void walk(int[] arr,int leftStep,int index) {
		
		if (Global.isTimeOut()||index==arr.length) {
			return;
		}
		
		if(leftStep==0){
			// 移动
			Global.reset();
			move(arr);
			new GreedyOptimizer().optimize();
			return;
		}
		
		arr[index] = 1;
		walk(arr, leftStep-1, index+1);
		
		arr[index] = 0;
		walk(arr, leftStep, index+1);	
	}


	@Override
	void optimize() {
		
		int maxServerNum = Global.bestServerNum;
		
		if(maxServerNum==0){
			return;
		}
	
		int[] arr = new int[Global.nodeNum];
		Arrays.fill(arr, 0);

		walk(arr, maxServerNum, 0);
	}
}
