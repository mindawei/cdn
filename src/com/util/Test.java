package com.util;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class Test {
	
	 

	    private static long seedUniquifier() {
	        // L'Ecuyer, "Tables of Linear Congruential Generators of
	        // Different Sizes and Good Lattice Structure", 1999
	        for (;;) {
	            long current = seedUniquifier.get();
	            long next = current * 181783497276652981L;
	            if (seedUniquifier.compareAndSet(current, next))
	                return next;
	        }
	    }

	    private static final AtomicLong seedUniquifier
	        = new AtomicLong(8682522807148012L);

	    
	public static void main(String[] args) {
		long seed =  seedUniquifier() ^ System.nanoTime();
		//System.out.println(seed);
		// -3282049572188798842
		long timeOff1 = 48*60*60*1000*1000*1000;
		long timeOff2 = -25*60*1000*1000*1000;
//		System.out.println(System.nanoTime()-timeOff1);
		System.out.println(seedUniquifier() ^ (System.nanoTime()-timeOff1));
//		System.out.println(System.nanoTime()-timeOff2);
//		System.out.println(seedUniquifier() ^ (System.nanoTime()-timeOff2));
		//long v = seedUniquifier() ^ -3282049572188798842
		
		
	}
	
	
	
	

}
