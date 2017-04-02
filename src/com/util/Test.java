package com.util;

import java.util.Arrays;

public class Test {
	
	private static final int[] arr = new int[59];
	
	/** 多少个1 */
	private static void increaseOne() {
		int index = arr.length-1;
		int jw = 1;
		while(jw==1&&index>=0){
			if(arr[index]==0){
				arr[index] = 1;
				jw = 0;
			}else{ // =1
				arr[index] = 0;
				jw = 1;
			}
			index--;
		}
	}
	private static int oneNum() {
		int n = 0;
		for(int val : arr){
			n+=val;
		}
		return n;
	}
//	public static void main(String[] args) {
//		// 4 的组合
//		long t =System.currentTimeMillis();
//		while(true){
//			// System.out.println(Arrays.toString(arr));
//			increaseOne();
//			int oneNums = oneNum();
//			if(oneNums==arr.length){
//				break;
//			}
//			
//		}
//		long all = System.currentTimeMillis()- t;
//		System.out.println(all);
//	}

	
	public static void main(String[] args) {
		int n = 3;
		int[] arr = new int[59];
		walk(arr,n,0);
		
	}
	
	private static void walk(int[] arr, int leftStep,int index) {
		if(leftStep==0){
			System.out.println(Arrays.toString(arr));
			return;
		}
		if(index==arr.length){
			return;
		}
		
		arr[index] = 1;
		walk(arr, leftStep-1, index+1);
		arr[index] = 0;
		walk(arr, leftStep, index+1);	
	}


	
	
	
	
	

}
