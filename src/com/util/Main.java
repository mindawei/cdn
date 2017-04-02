package com.util;

import com.cacheserverdeploy.deploy.Deploy;
import com.cacheserverdeploy.deploy.Global;
import com.filetool.util.FileUtil;
import com.filetool.util.LogUtil;
import com.util.FilePath;

/**
 * 
 * 工具入口
 * 
 * @version  [版本号, 2017-1-9]
 * @see  [相关类/方法]
 * @since  [产品/模块版本]
 */
public class Main
{
    public static void main(String[] args)
    {
//        if (args.length != 2)
//        {
//            System.err.println("please input args: graphFilePath, resultFilePath");
//            return;
//        }
//        String graphFilePath = args[0];
//        String resultFilePath = args[1];


		 String graphFilePath = FilePath.graphFilePath;
	     String resultFilePath = FilePath.resultFilePath;
	     
	     System.out.println(graphFilePath+" "+resultFilePath);
	     
        LogUtil.printLog("Begin");

        // 读取输入文件
        String[] graphContent = FileUtil.read(graphFilePath, null);

        // 功能实现入口
        String[] resultContents = Deploy.deployServer(graphContent);

        // 写入输出文件
        if (hasResults(resultContents))
        {
            FileUtil.write(resultFilePath, resultContents, false);
        }
        else
        {
            FileUtil.write(resultFilePath, new String[] { "NA" }, false);
        }
        LogUtil.printLog("End");
    	
    }
    
    private static boolean hasResults(String[] resultContents)
    {
        if(resultContents==null)
        {
            return false;
        }
        for (String contents : resultContents)
        {
            if (contents != null && !contents.trim().isEmpty())
            {
                return true;
            }
        }
        return false;
    }

}
