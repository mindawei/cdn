package com.cacheserverdeploy.deploy;

import java.util.Arrays;

/**
 * 启发式搜素
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public class HeuristicOptimizer extends Optimizer {
	
	private int[] arr;
	private int maxServerNum;
	
	private void walk(int leftStep,int index) {
		
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
		walk(leftStep-1, index+1);
		
		arr[index] = 0;
		walk(leftStep, index+1);	
	}


	@Override
	void optimize() {
		
		maxServerNum = Global.bestServerNum;
		if(maxServerNum==0){
			return;
		}
	
		arr = new int[Global.nodeNum];
		Arrays.fill(arr, 0);

		walk(maxServerNum, 0);
	}
}
