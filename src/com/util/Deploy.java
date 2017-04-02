package com.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

public class Deploy
{
    /**
     * 你需要完成的入口
     * <功能详细描述>
     * @param graphContent 用例信息文件
     * @return [参数说明] 输出结果信息
     * @see [类、类#方法、类#成员]
     */
	
	// serverSelected:目前用模拟退火选择的服务器组   clientList:消费节点数组
	static List<Integer> serverSelected= new ArrayList<Integer>() ,clientList= new ArrayList<Integer>();
	
	// serverSelectedCertain:确定要选择的服务器组 
	static List<Integer> serverSelectedCertain = new ArrayList<Integer>(),serverSelectedUNcertain = new ArrayList<Integer>();
	
	//总体费用  模拟退火内循环，外循环
	static int totalCost=0,ILOOP=1000,OLOOP=100000;
	//连续多少次不改变就认为达到局部最优解
	static int LIMIT =1000;	
	
	//退火初始温度
	static int T= 3000;
	
	//冷却速度
	static double DELTA=0.98;
	//精度
	static double EPS= 1e-3;
	//costMap,minMap用于存放输出结果	    
	static Map<LinkedHashSet<Integer>,Integer> costMap= new HashMap<LinkedHashSet<Integer>,Integer>() ,minMap= new HashMap<LinkedHashSet<Integer>,Integer>();
	static final int N =1009;
	static int[] pre = new int[N];
	static int[] supply = new int[N];
	static int[] need = new int[N];
	
	static Random rdm = new Random();
	static int curSupply=0;
	//计时器flag
	static boolean flag=true;

	static int [][]tmpMap;//保存当前宽带消耗
	
	//网络节点
	static class NODE{
	    int id;//
	    int bandWidth;
	    int costPath;
	}
	
    public static String[] deployServer(String[] graphContent)
    {
    	// 启用定时器机制
    	Timer timer = new Timer();
		timer.schedule(new MyTimerTask(timer), 87*1000);

		// numNode:网络节点数量
		// numPath:网络链路数量
		// numClient:消费节点数量
		String[] strs1 = graphContent[0].split(" ");
		int numNode = Integer.parseInt(strs1[0]);
		int numPath = Integer.parseInt(strs1[1]);
		int numClient = Integer.parseInt(strs1[2]);
		
		//服务器费用
		int costServer = Integer.parseInt(graphContent[2]);
	
		//costPath和bandWidth分别存放路径费用和带宽限制
		int[][] costPath = new int[numNode+2][numNode+2];
		int[][] bandWidth = new int[numNode+2][numNode+2];
		
		//建立消费节点ID与网络节点ID的映射
		Map<Integer,Integer> mapClient = new HashMap<Integer,Integer>();
		
		//多源多汇问题，定义一个超级源点s,一个超级汇点
		int s =numNode,e =numNode+1;
		//int [][]map = new int [numNode+2][numNode+2]; //带宽限制
		

		for(int i=0;i<numPath;i++){
			String msg = graphContent[4+i];
			String[] msgs = msg.split(" ");
			int u = Integer.parseInt(msgs[0]);
			int v = Integer.parseInt(msgs[1]);
			int bandW = Integer.parseInt(msgs[2]);
			int costP = Integer.parseInt(msgs[3]);
			costPath[u][v]= costP;
			costPath[v][u]= costP;
			bandWidth[u][v]= bandW;
			bandWidth[v][u]= bandW;
			supply[u]+=bandW;
			supply[v]+=bandW;

		}
		
		int totalSupply=0,totalNeed=0;
		
		for(int i=0;i<numClient;i++){
			String msg = graphContent[5+numPath+i];
			String[] msgs = msg.split(" ");
			
			int u = Integer.parseInt(msgs[0]);
			int v = Integer.parseInt(msgs[1]);
			int needC = Integer.parseInt(msgs[2]);
			mapClient.put(v, u);
			need[v]= needC;
			
			//定义消费节点到超级汇点的带宽 消费节点的需求带宽
			bandWidth[v][e]=needC;
			//统计所有节点的消费需求
			totalNeed+=needC;
			
			clientList.add(v);
			
			//如果需求大于供应，则这个消费节点必须建立服务器
			if(need[v]>supply[v]){
				serverSelectedCertain.add(v);
				bandWidth[s][v]=Integer.MAX_VALUE;
			}else{
				int maxCost=0,maxNodeCost=0,maxNodeBand=0;
				
				List<NODE> list = new ArrayList<NODE>();
				for(int j=0;j<numNode;j++){
					//Adj.set(v, element)
					if(costPath[v][j]!=0){
						NODE nodeJ= new NODE();
						nodeJ.id=j;
						nodeJ.bandWidth=bandWidth[v][j];
						nodeJ.costPath=costPath[v][j];
						list.add(nodeJ);
					}
				}
				Collections.sort(list,new Comparator<NODE>() {

					@Override
					public int compare(NODE o1, NODE o2) {
						// TODO Auto-generated method stub
						return o1.costPath-o2.costPath;
					}
				});
				int k=0;
				while(needC!=0){
					NODE tmpNODE = list.get(k); 
					if(needC-tmpNODE.bandWidth>0){
						maxCost+=tmpNODE.bandWidth*tmpNODE.costPath;
						k++;
						needC-=tmpNODE.bandWidth;
					}else{
						maxCost+=needC*tmpNODE.costPath;
						k++;
						needC=0;
					}
				}
				if(maxCost>costServer){
					serverSelectedCertain.add(v);
					bandWidth[s][v]=Integer.MAX_VALUE;
				}
			}

		}
		
		for(int client:clientList){
				if(!serverSelectedCertain.contains(client)){
					//初始解
					bandWidth[s][client]=Integer.MAX_VALUE;
					serverSelectedUNcertain.add(client);
				}
		}
		
		int[][] tmpBandWidth = new int [numNode+2][numNode+2];
		
		tmpBandWidth =copyArray2(bandWidth);
		
		totalCost=fond(s,e,tmpBandWidth,costPath,mapClient)+(serverSelectedUNcertain.size()+serverSelectedCertain.size())*costServer;
		
		minMap=(Map<LinkedHashSet<Integer>, Integer>) deepClone(costMap);

		SA(s,e,bandWidth,costPath,mapClient,costServer,totalNeed);
		
		int totalPath = minMap.size();
		
		String [] result = new String[totalPath+2];
		
		result[0]=String.valueOf(totalPath);
		result[1]="\r\n";
		
		int countResultLine=2;
		for(Entry<LinkedHashSet<Integer>,Integer> entry:minMap.entrySet()){
			LinkedHashSet<Integer> tset = entry.getKey();
			result[countResultLine]="";
			int tmpts=0;
			for(int ts:tset){
				result[countResultLine]+=String.valueOf(ts)+" ";
				tmpts=ts;
			}
			result[countResultLine]+=String.valueOf(mapClient.get(tmpts))+" ";
			result[countResultLine]+=String.valueOf(entry.getValue());
			countResultLine++;
		}
		//long endTime=System.currentTimeMillis();
		//System.out.println(endTime-startTime);
        /**do your work here**/
        return result;
    }
   
    static int[][] copyArray2(int[][] array){
		int [][] copyArray = new int[array.length][];
		for(int i=0;i<array.length;i++){
			copyArray[i]=new int[array[i].length];
			System.arraycopy(array[i], 0, copyArray[i], 0, array[i].length);
		}
		return copyArray;
	}
  
    static void SA(int s,int e,int[][]bandWidth,int[][]cost,Map<Integer,Integer> mapClient,int costServer,
    		int totalNeed){

    	double t = T;
    	int curCost = totalCost;
    	int newCost = totalCost;
    	int P_L=0;
    	int P_F=0;
 
    	while(flag){
    		for(int i=0;i<ILOOP;i++){
    			if(flag==false){
    				break;
    			}
    			
    	    	serverSelected=(List<Integer>) deepClone(serverSelectedUNcertain);
				double dd = rdm.nextDouble();
				
    			int serSize = serverSelected.size();
    			costMap.clear();
    			for(int f=0;f<bandWidth[s].length;f++){
    				bandWidth[s][f]=0;
					if(serverSelected.contains(f)||serverSelectedCertain.contains(f)){
						bandWidth[s][f]=Integer.MAX_VALUE;
					}
				}
    	
    			int clientSize = clientList.size()-serverSelectedCertain.size();
    			
    			//当服务器节点数选择比较少时，应该大概率进行加服务器操作，当服务器节点数量较多时大概率进行减服务器操作
				if(serSize==1&&(0<=dd&&dd<0.3)
						||1<serSize&&serSize<=clientSize/3&&0<=dd&&dd<(0.5-0.2*3*(clientSize/3-serSize)/clientSize)
						||serSize>clientSize/3&&serSize<=clientSize*2/3&&0<=dd&&dd<0.5
						||serSize>clientSize*2/3&&serSize<clientSize&&0<=dd&&dd<(0.5-0.2*3*(2*clientSize/3-serSize)/clientSize)
						)
				{
    				newCost = getNextExchange(s,e,bandWidth,cost,mapClient,totalNeed);
    				
    			}else if(serSize==1&&(0.3<=dd&&dd<1)||
    					1<serSize&&serSize<=clientSize/3&&(0.3+0.4*3*(serSize-1)/clientSize)<=dd&&dd<1||
    					serSize>clientSize/3&&serSize<=clientSize*2/3&&(0.7+0.1*3*(serSize-clientSize/3)/clientSize<=dd&&dd<1)||
    					serSize>clientSize*2/3&&serSize<clientSize&&(0.8+0.2*3*(serSize-clientSize*2/3)/clientSize<=dd&&dd<1)
    					)
    			{
    				
    				newCost = getNextADD(s,e,bandWidth,cost,mapClient,totalNeed);
    			
    			}else{
    				
    				newCost = getNextDESC(s,e,bandWidth,cost,mapClient,totalNeed);
    				
    			}
    			if(curSupply!=totalNeed){
    				i--;
    				continue;
    			}
    			
    			newCost = newCost+costServer*(serverSelected.size()+serverSelectedCertain.size());
    			double dE = newCost-curCost;
    			
    			if(dE<0){
    				
    				curCost=newCost;
    				//totalCost= newCost;
    				serverSelectedUNcertain=(List<Integer>) deepClone(serverSelected);
    			}else{
    				
    				double rd = rdm.nextDouble();
    				double ee = Math.exp(-dE/t);
    				
    				//避免陷入局部最优
    				if(ee>rd&&ee<1){
    					curCost=newCost;
        				serverSelectedUNcertain=(List<Integer>) deepClone(serverSelected);
    				}
    				P_L++;
    			}
    			
    			
    			
    			if(P_L>LIMIT){
    				P_F++;
    				break;
    			}
    		}
    		
    		if(curCost<totalCost){
				
				totalCost=curCost;
				minMap=(Map<LinkedHashSet<Integer>, Integer>) deepClone(costMap);
				
			}
    		
    		if(P_F>OLOOP||t<EPS){
    			
    			break;
    		}
    		t=t*DELTA;
    		
    	}
    }
    
    static int getNextExchange(int s,int e,int[][]bandWidth,int[][]cost,Map<Integer,Integer> mapClient,int totalNeed){
    	
    	int len = bandWidth.length;
    	tmpMap =copyArray2(bandWidth);
    	
    	int x = (int) rdm.nextInt(len-2);
    	while(serverSelected.contains(x)||serverSelectedCertain.contains(x)){
    		x = (int) rdm.nextInt(len-2);
    	}
    	
    	int y = rdm.nextInt(serverSelected.size());
    	int z = serverSelected.remove(y);
    	tmpMap[s][z]=0;
    	serverSelected.add(x);
    	tmpMap[s][x]=0;
    	tmpMap[s][x]=Integer.MAX_VALUE;
    	int totalsupply=0;
    	int tmpTotalNeed = totalNeed;
    	for(int i=0;i<tmpMap[0].length;i++){
    		if(tmpMap[s][i]==Integer.MAX_VALUE){
    			if(!clientList.contains(i)){
    				totalsupply+=supply[i];
    			}else{
    				totalsupply+=supply[i];
    				tmpTotalNeed=tmpTotalNeed-need[i];
    			}
    		}
    	}
    	if(totalsupply<tmpTotalNeed){
    		curSupply=0;
    		return 0;
    	}
    	return fond(s,e,tmpMap,cost,mapClient);
    }
    
    static int getNextADD(int s,int e,int[][]map,int[][]cost,Map<Integer,Integer> mapClient,int totalNeed){
    	
    	int len = map.length;
    	tmpMap  =copyArray2(map);
    	
    	int x = (int) rdm.nextInt(len-2);
    	while(serverSelected.contains(x)||serverSelectedCertain.contains(x)){
    		x = (int) rdm.nextInt(len-2);
    	}
    	
    	serverSelected.add(x);
    	tmpMap[s][x]=Integer.MAX_VALUE;
    	
    	int totalsupply=0;
    	int tmpTotalNeed = totalNeed;
    	
    	for(int i=0;i<tmpMap[0].length;i++){
    		if(tmpMap[s][i]==Integer.MAX_VALUE){
    			if(!clientList.contains(i)){
    				totalsupply+=supply[i];
    			}else{
    				totalsupply+=supply[i];
    				tmpTotalNeed=tmpTotalNeed-need[i];
    			}
    		}
    	}
    	
    	if(totalsupply<tmpTotalNeed){
    		curSupply=0;
    		return 0;
    	}
    	
    	return fond(s,e,tmpMap,cost,mapClient);
    }
   
    static int getNextDESC(int s,int e,int[][]map,int[][]cost,Map<Integer,Integer> mapClient,int totalNeed){
    	int len = map.length;
    	tmpMap  =copyArray2(map);

    	int y = rdm.nextInt(serverSelected.size());
    	int x = serverSelected.remove(y);
    	tmpMap[s][x]=0;
    	int totalsupply=0;
    	int tmpTotalNeed = totalNeed;
    	for(int i=0;i<tmpMap[0].length;i++){
    		if(tmpMap[s][i]==Integer.MAX_VALUE){
    			if(!clientList.contains(i)){
    				totalsupply+=supply[i];
    			}else{
    				totalsupply+=supply[i];
    				tmpTotalNeed=tmpTotalNeed-need[i];
    			}
    		}
    	}
    	if(totalsupply<tmpTotalNeed){
    		curSupply=0;
    		return 0;
    	}
    	return fond(s,e,tmpMap,cost,mapClient);
    }

    
	public static boolean spfa(int s,int e,int[][]map,int[][]cost){
		
		int dis[] = new int[N];
		boolean vis[] = new boolean[N];
		int i;
		Queue<Integer> q = new LinkedList<Integer>();
		for(i=0;i<=e;i++){
			vis[i]=false;
			dis[i]= Integer.MAX_VALUE;
		}
		vis[s]=true;
		dis[s]=0;
		q.offer(s);
		while(!q.isEmpty()){
			int index = q.peek();
			vis[index]=true;
			q.poll();
			for(i=0;i<=e;i++){
				if(map[index][i]!=0&&dis[i]>dis[index]+cost[index][i]){
					dis[i]=dis[index]+cost[index][i];
					pre[i]=index;
					if(vis[i]==false){
						vis[i]=true;
						q.offer(i);
					}
					
				}
			}
		}
		if(dis[e]!=Integer.MAX_VALUE){
			return true;
		}else{
			return false;
		}
		
	}
	
	public static int fond(int s,int e,int[][]map,int[][]cost,Map<Integer,Integer> mapClient){
		int i,j;
		int Min = Integer.MAX_VALUE;
		j=0;
		int []post = new int[N];
		curSupply=0;
		while(spfa(s,e,map,cost)&&flag){
			LinkedHashSet<Integer> linkedHashSet = new LinkedHashSet<Integer>();
			for(i=e;i!=s;i=pre[i]){
				Min=Math.min(Min, map[pre[i]][i]);
			}
			int ppre=0;
			for(i=e;i!=s;){
				post[pre[i]]=i;
				ppre=i;
				i=pre[i];
			}
			post[s]=ppre;
			for(i=s;i!=e;i=post[i]){
				if(i!=s){
					linkedHashSet.add(i);
				}				
			}
			//linkedHashSet.add(mapClient.get(pre[e]));
			for(i=e;i!=s;i=pre[i]){
				map[pre[i]][i]-=Min;
				map[i][pre[i]]+=Min;
				j+=cost[pre[i]][i]*Min;
			}
			curSupply+=Min;
			costMap.put(linkedHashSet, Min+(costMap.get(linkedHashSet)==null?0:costMap.get(linkedHashSet)));
		}
		return j;
		
	}

	 // 用序列化与反序列化实现深克隆  
   public static  Object deepClone(Object src)  
   {  
       Object o = null;  
       try  
       {  
           if (src != null)  
           {  
               ByteArrayOutputStream baos = new ByteArrayOutputStream();  
               ObjectOutputStream oos = new ObjectOutputStream(baos);  
               oos.writeObject(src);  
               oos.close();  
               ByteArrayInputStream bais = new ByteArrayInputStream(baos  
                       .toByteArray());  
               ObjectInputStream ois = new ObjectInputStream(bais);  
               o = ois.readObject();  
               ois.close();  
           }  
       } catch (IOException e)  
       {  
           e.printStackTrace();  
       } catch (ClassNotFoundException e)  
       {  
           e.printStackTrace();  
       }  
       return o;  
   }  
	static class MyTimerTask extends TimerTask { 
		
		private Timer timer;
		
		public MyTimerTask(Timer timer){
			
			this.timer= timer;
		}
		
        public void run() {  
        	
        	flag=false;
        	
        	this.timer.cancel();
        }  
    }   
}

