package aml.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {  
	
	public Set<Integer> nodes; //存放点的集合  
	public HashMap<Integer,Set<Integer>> parentMap;
	private List<ArrayList<Integer>> result;

	public Graph() 
	{
		nodes=new HashSet<Integer>();
		parentMap = new HashMap<Integer,Set<Integer>>();
		result=new ArrayList<ArrayList<Integer>>();
	}
	
	public void init(HashMap<Integer,Set<Integer>> Node2Parent) 
	{
		this.nodes=Node2Parent.keySet();
		this.parentMap = Node2Parent;
	}
	
	public HashMap<Integer,Set<Integer>> getParentMap()
	{
		return parentMap;
	}
	
	public List<ArrayList<Integer>> getPaths(Integer start, Integer end)
	{
		result.clear(); 
		//Path.clear();
	    Map<Integer, Boolean> states = new HashMap<Integer, Boolean>();
	    for (Integer i: nodes) 
	    {
			states.put(i, false);
		}    
	    ArrayList<Integer> path = new ArrayList<>();  
		DFS(start,end,path,states);	
		List<ArrayList<Integer>> Paths=new ArrayList<ArrayList<Integer>>();
		Paths.addAll(result);
		return  Paths;
	}
	
	
	private void DFS(Integer start,Integer end, ArrayList<Integer> path,Map<Integer, Boolean> states) 
	{   
        // 遍历相邻的结点  
		//System.out.println(start==end);
        if (start.intValue() == end.intValue()) 
        {
			// 存储该路径
        	path.add(start);
        	ArrayList<Integer> validatePath=new ArrayList<Integer>();
        	for(Integer node:path)
        		validatePath.add(node);
        	result.add(validatePath);
        	return;
		} 
        if (!states.containsKey(start))  
		{
			//path.remove(start);
			//return;
        	System.out.println("---------------");
        	System.out.println(start);
        	System.out.println(parentMap.keySet().contains(start));
        	System.out.println("---------------");
		} 	
        if(path.contains(start))
		{
			path.add(start); //其实是为了移除
			return;
		}
        else if (!states.containsKey(start)||states.get(start) == true)  
		{
			//path.remove(start);
			return;
		} 	
        else if (parentMap.get(start) == null||!parentMap.keySet().contains(start)) 
        {
        	//path.remove(start);
        	path.add(start); //新增的语句
			return;
		}
		else 
		{
			path.add(start);
			states.put(start, true);		 
			for (Integer next : parentMap.get(start)) // 打印真实的路径
			{
				states.put(next, false);			
				DFS(next, end, path, states);
				//path.remove(next);			
				if(path.size()!=0)
					path.remove(path.size()-1);	
				/*if(path.size()!=0&&path.size()!=1)
				path.remove(path.size()-1);	*/
				states.put(next, true);
			}
		}       
    }
	
	public void clear()
	{
		parentMap.clear();
	}

}
