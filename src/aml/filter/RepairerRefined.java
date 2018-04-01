/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* A filtering algorithm based on logical coherence.                           *
*                                                                             *
* @author Daniel Faria & Emanuel Santos                                       *
******************************************************************************/
package aml.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import aml.AML;
import aml.match.Mapping;
import aml.settings.MappingStatus;
import aml.util.InteractionManager;
import aml.util.KG;
import aml.util.MIPP;
import aml.util.Pair;

public class RepairerRefined implements Flagger
{
	
//Attributes
	
	private AML aml;
	//private RepairMap rMap;
	private RepairMapRefine rMap;
	private InteractionManager im;
	
//Constructors
	
	/**
	 * Constructs a Repairer for automatic repair
	 */
	public RepairerRefined()
	{
		aml = AML.getInstance();
		rMap = aml.getRefinedRepairMap();
		if(rMap == null)
			rMap = aml.buildRefinedRepairMap();
		im = aml.getInteractionManager();
	}

	public void RevisedMappings()
	{
		if(rMap.isCoherent())
		{
			System.out.println("Alignment is coherent");
			return;
		}
		System.out.println("Repairing Alignment");
		long time = System.currentTimeMillis()/1000;
		int revisedCount = 0;
		int reservedCount = 0;
		int removedCount = 0;
		ArrayList<Integer> revisedMappings=new ArrayList<Integer>();
		ArrayList<Integer> reservedMappings=new ArrayList<Integer>();
		ArrayList<Integer> removedMappings=new ArrayList<Integer>();
		//Loop until no more mappings can be removed
		//buildCheckListrMap.getConflictSets();
		int iteration=0;
		while(true)
		{
			System.out.println("The iteration is "+(iteration++));
			//int worstMapping = getIncorrectMapping();
			int worstMapping = getIncorrectMapping2();
			if(worstMapping != -1)
			{
				System.out.println("The index of wrong mapping is: "+ worstMapping);
				System.out.println(aml.getAlignment().get(worstMapping).toString());
				if(ExistCommonEntailment2(worstMapping))  //如果存在，worstMapping可能都会有
				{
					
					//Revise操作
					if(rMap.reviseJudge(worstMapping))
					{
						System.out.println("The mapping needs to be revised.");
						revisedMappings.add(worstMapping);
						revisedCount++;
					}
					else
					{
						System.out.println("The mapping needs to be reserved.");
						reservedMappings.add(worstMapping);
						reservedCount++;
					}
					
				}
				else  //Remove操作
				{	
					System.out.println("The mapping needs to be removed.");
					/*for(Pair<ArrayList<Integer>, ArrayList<Integer>>  pair: rMap.getMapMinimalConflictSets().get(worstMapping))
				    {
				    	System.out.println("-------------------------------");
				    	System.out.println("The two paths are:");
				    	for(Integer num:pair.getLeft())
				    		System.out.print(aml.getURIMap().getURI(num)+" ->");
				    	System.out.println();
				    	for(Integer num:pair.getRight())
				    		System.out.print(aml.getURIMap().getURI(num)+"->");
				    	System.out.println();
				    	System.out.println("-------------------------------");
				    }	*/
					
					rMap.remove(worstMapping,true);
					//rMap.revise(worstMapping);	
					removedMappings.add(worstMapping);
					removedCount++;
				}
				//单纯的remove操作
				/*rMap.remove(worstMapping,true);	
				removedMappings.add(worstMapping);
				removedCount++;*/
			}
			else //已经不存在错误的mapping了
			{
				break;
			}
			//
		}
		System.out.println("The removed mappings is listed as followed:");
		for(int m:removedMappings)
		{	
			System.out.println(aml.getAlignment().get(m).toString());
		}
		System.out.println("removed " + removedCount + " mappings");
		System.out.println("The revised mappings is listed as followed:");
		for(int m:revisedMappings)
		{	
			System.out.println(aml.getAlignment().get(m).toString() + " "+aml.getAlignment().get(m).getSourceId()+" "+aml.getAlignment().get(m).getTargetId());
		}
		System.out.println("revised " + revisedCount + " mappings");
		System.out.println("The reserved mappings is listed as followed:");
		for(int m:reservedMappings)
		{	
			System.out.println(aml.getAlignment().get(m).toString() + " "+aml.getAlignment().get(m).getSourceId()+" "+aml.getAlignment().get(m).getTargetId());
		}
		System.out.println("reserved " + reservedCount + " mappings");		
		aml.removeIncorrect();
		System.out.println("Finished Repair in " + 
				(System.currentTimeMillis()/1000-time) + " seconds");
		
	}
	
	@Override
	public void flag()
	{
		System.out.println("Running Coherence Flagger");
		long time = System.currentTimeMillis()/1000;
		for(Integer i : rMap)
			if(rMap.getMapping(i).getStatus().equals(MappingStatus.UNKNOWN))
				rMap.getMapping(i).setStatus(MappingStatus.FLAGGED);
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
	
	/*private int getIncorrectMapping()
	{			
		int worstMapping = -1;
		int maxCard = 0;	
		HashMap<Integer,ArrayList<Integer>> CardMapping=new HashMap<Integer,ArrayList<Integer>>();
		for(Integer map: rMap.getMapMinimalConflictSets().keySet())
		{		
			int card = rMap.getMapMinimalConflictSets().get(map).size();
			if(card > maxCard )			
			{
				maxCard = card;			
			}
			if(CardMapping.keySet().contains(card))
			{
				CardMapping.get(card).add(map);
			}
			else
			{
				ArrayList<Integer> list=new ArrayList<Integer>();
				list.add(map);
				CardMapping.put(card, list);
			}
		}
		if(maxCard==0) //no MIPPs
			return -1;
			
		ArrayList<Integer> maxCardMapping=new ArrayList<Integer>();
		maxCardMapping=CardMapping.get(maxCard);
		if(maxCardMapping.size()==1)
		{
			worstMapping=maxCardMapping.get(0);		
		}
		else
		{
			ArrayList<Integer> commonEntailment=new ArrayList<Integer>();
			commonEntailment=getMinimalEntailment(maxCardMapping);
			if(commonEntailment.size()==1)
				worstMapping=commonEntailment.get(0);
			else
			{
				ArrayList<Integer> WeightsMapping=new ArrayList<Integer>();
				WeightsMapping=getminimalWeight(commonEntailment);
				worstMapping=WeightsMapping.get(0);
			}
		}
		return worstMapping;
	}*/
	
	private int getIncorrectMapping2()
	{	
		int worstMapping = -1;
		Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
		for(Integer mappings: rMap.getMapMinimalConflictSets().keySet())
		{
			map.put(mappings,rMap.getMapMinimalConflictSets().get(mappings).size());		
		}
		
		List<Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer,Integer>>(map.entrySet());
        //然后通过比较器来实现排序
        Collections.sort(list,new Comparator<Map.Entry<Integer,Integer>>() {
            //降序排序
            public int compare(Entry<Integer, Integer> o1,
                    Entry<Integer, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }         
        });
		
		ArrayList<Integer> CardMapping=new ArrayList<Integer>();
		int maxCard = 0;
		 for(Entry<Integer, Integer> mapping:list)
		{ 
            int key=mapping.getKey();
            if(maxCard==0)
         	{
         		maxCard=mapping.getValue();
         		CardMapping.add(key);
         	}
            else if(maxCard==mapping.getValue())
        		CardMapping.add(key);
        	else 
        	{
				break;
			}     
        } 
		if(maxCard==0) //no MIPPs
			return -1;
		if(CardMapping.size()==1)
		{
			worstMapping=CardMapping.get(0);		
		}
		else
		{
			ArrayList<Integer> commonEntailment=new ArrayList<Integer>();
			commonEntailment=getMinimalEntailment2(CardMapping);
			if(commonEntailment.size()==1)
				worstMapping=commonEntailment.get(0);
			else
			{
				ArrayList<Integer> WeightsMapping=new ArrayList<Integer>();
				WeightsMapping=getminimalWeight2(commonEntailment);
				worstMapping=WeightsMapping.get(0);
			}
		}
		return worstMapping;
	}
	
	/*private ArrayList<Integer> getMinimalEntailment(ArrayList<Integer> maxCardMapping) 
	{
		int commonNum=Integer.MAX_VALUE;
		HashMap<Integer,ArrayList<Integer>> CommonMapping=new HashMap<Integer,ArrayList<Integer>>();
		for(Integer map:maxCardMapping)
		{
			int num=0;
			for(Pair<ArrayList<Integer>, ArrayList<Integer>> mipp:rMap.getMapMinimalConflictSets().get(map))
			{
				//HashMap<Path, Set<Integer>> commonEntailment=getCommonEntailment(mipp);
				//HashMap<Path, Set<Integer>> commonEntailment=mipp.getCommonEntailment();
				for(Path path:commonEntailment.keySet())
				{
					if(path.contains(map))
						num=num+commonEntailment.get(path).size();
				}
			}
			if(commonNum>num)
			{
				commonNum=num;
			}
			if(CommonMapping.keySet().contains(num))
			{
				CommonMapping.get(num).add(map);
			}
			else
			{
				ArrayList<Integer> list=new ArrayList<Integer>();
				list.add(map);
				CommonMapping.put(num, list);
			}			
		}
		ArrayList<Integer> commonEntailmentMappping=new ArrayList<Integer>();
		commonEntailmentMappping=CommonMapping.get(commonNum);
		return commonEntailmentMappping;
	}*/
	
	private ArrayList<Integer> getMinimalEntailment2(ArrayList<Integer> maxCardMapping) 
	{
		Map<Integer, Integer> map = new TreeMap<Integer, Integer>();	
		for(Integer mappings:maxCardMapping)
		{
			int num=0;
			for(Pair<ArrayList<Integer>, ArrayList<Integer>> mipp:rMap.getMapMinimalConflictSets().get(mappings))
			{
				/*HashMap<Integer, Set<Integer>> commonEntailment=mipp.getCommonEntailment();
				for(Integer path:commonEntailment.keySet())
				{
					if(path==mappings)
						num=num+commonEntailment.get(path).size();
				}*/
				/*HashMap<Integer, Integer> commonEntailment=mipp.getCommonEntailment();
				for(Integer path:commonEntailment.keySet())
				{
					if(path==mappings)
						num=num+commonEntailment.get(path);
				}*/
				HashMap<Integer, Boolean> commonEntailment=mipp.getCommonEntailment();
				for(Integer path:commonEntailment.keySet())
				{
					if(path==mappings)
						num++;
				}
			}
			map.put(mappings, num);
		}
		
		List<Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer,Integer>>(map.entrySet());
        //然后通过比较器来实现排序
        Collections.sort(list,new Comparator<Map.Entry<Integer,Integer>>() {
            //升序排序
            public int compare(Entry<Integer, Integer> o1,
                    Entry<Integer, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }         
        });
		
		ArrayList<Integer> commonEntailmentMappping=new ArrayList<Integer>();
		int commonNum = -1;
		 for(Entry<Integer, Integer> mapping:list)
		{ 
            int key=mapping.getKey();
            if(commonNum==-1)
         	{
            	commonNum=mapping.getValue();
         		commonEntailmentMappping.add(key);
         	}
            else if(commonNum==mapping.getValue())
            	commonEntailmentMappping.add(key);
        	else 
        	{
				break;
			}     
        }
		return commonEntailmentMappping;
	}
	
	private ArrayList<Integer> getminimalWeight(ArrayList<Integer> commonEntailment) 
	{
		double minWeight=Integer.MAX_VALUE;
		HashMap<Double,ArrayList<Integer>> weightMapping=new HashMap<Double,ArrayList<Integer>>();
		for(Integer map:commonEntailment)
		{
			double weight=rMap.getMapping(map).getSimilarity();
			if(minWeight>weight)
			{
				minWeight=weight;
			}
			if(weightMapping.keySet().contains(weight))
			{
				weightMapping.get(weight).add(map);
			}
			else
			{
				ArrayList<Integer> list=new ArrayList<Integer>();
				list.add(map);
				weightMapping.put(weight, list);
			}	
		}
		ArrayList<Integer> WeightsMapping=new ArrayList<Integer>();
		WeightsMapping=weightMapping.get(minWeight);
		return WeightsMapping;		
	}
	
	private ArrayList<Integer> getminimalWeight2(ArrayList<Integer> commonEntailment) 
	{
		Map<Integer, Double> map = new TreeMap<Integer, Double>();
		for(Integer mappings:commonEntailment)
		{
			double weight=rMap.getMapping(mappings).getSimilarity();
			map.put(mappings, weight);
		}
		List<Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer,Double>>(map.entrySet());
        //然后通过比较器来实现排序
        Collections.sort(list,new Comparator<Map.Entry<Integer,Double>>() {
            //升序排序
            public int compare(Entry<Integer, Double> o1,
                    Entry<Integer, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }         
        });
		
		ArrayList<Integer> WeightsMapping=new ArrayList<Integer>();
		double minWeight = -1;
		 for(Entry<Integer, Double> mapping:list)
		{ 
            int key=mapping.getKey();
            if(minWeight==-1)
         	{
            	minWeight=mapping.getValue();
            	WeightsMapping.add(key);
         	}
            else if(minWeight==mapping.getValue())
            	WeightsMapping.add(key);
        	else 
        	{
				break;
			}     
        }
		return WeightsMapping;		
	}
	
	
	/*private boolean ExistCommonEntailment(int mapping)
	{
		int num=0;
		boolean flag=true;
		for(Pair<ArrayList<Integer>, ArrayList<Integer>> mipp: rMap.getMapMinimalConflictSets().get(mapping))
		{
			HashMap<Path, Set<Integer>> commonEntailment=mipp.getCommonEntailment();
			boolean hasEntailments=false;
			for(Path path:commonEntailment.keySet())
			{
				if(path.contains(mapping))
				{
					hasEntailments=true;
					num=num+commonEntailment.get(path).size();
				}
			}	
			flag=flag&&hasEntailments;  //其实这个条件比较草率
			if(!flag)
				break;
		}	
		return flag;
	}*/
	
	private boolean ExistCommonEntailment2(int mapping)
	{
		int num=0;
		for(Pair<ArrayList<Integer>, ArrayList<Integer>> mipp: rMap.getMapMinimalConflictSets().get(mapping))
		{
			HashMap<Integer, Boolean> commonEntailment=mipp.getCommonEntailment();
			if(commonEntailment.keySet().contains(mapping))  //其实这个条件比较草率
			{
				num++;
			}
			//System.out.println("%%%%%%%");
			//getCommonEntailment(mipp);
		}
		//double ratio=(double)num/(double)rMap.getMapMinimalConflictSets().get(mapping).size();
		
		if(num!=0)
			return true;
		else
			return false;
	}
	
	
	
	public HashMap<Path, Set<Integer>> getCommonEntailment(Pair<ArrayList<Integer>, ArrayList<Integer>> mipp)
	{
		HashMap<Path, Integer> conflictTailIndex1=new HashMap<Path, Integer>();
		HashMap<Path, Integer> conflictTailIndex2=new HashMap<Path, Integer>();
		for(int i=0;i<mipp.left.size()-1;i++)
		{
			int index=-1;
			int node1=mipp.left.get(i);
			int node2=mipp.left.get(i+1);		
			index=aml.getAlignment().getIndexBidirectional(node1, node2);
			if(index!=-1)
			{	
				conflictTailIndex1.put(new Path(index),node2);
			}
		}
		
		for(int i=0;i<mipp.right.size()-1;i++)
		{
			int index=-1;
			int node1=mipp.right.get(i);
			int node2=mipp.right.get(i+1);		
			index=aml.getAlignment().getIndexBidirectional(node1, node2);
			if(index!=-1)
			{	
				conflictTailIndex2.put(new Path(index),node2);
			}
		}
		
		HashMap<Path, Set<Integer>> commonEntailment=new HashMap<Path, Set<Integer>>();
		for(Path path1:conflictTailIndex1.keySet())
		{
			int tail1=conflictTailIndex1.get(path1);
			Set<Integer> ancestor1=aml.getRelationshipMap().getSuperClasses(tail1,false);
			for(Path path2:conflictTailIndex2.keySet())
			{
				int tail2=conflictTailIndex2.get(path2);
				Set<Integer> ancestor2=aml.getRelationshipMap().getSuperClasses(tail2,false);
				ancestor2.retainAll(ancestor1);
				//ancestor2.remove(5892);
				if(!ancestor2.isEmpty())
				{
					
					for(Integer num:ancestor2)
					{
						/*if(aml.getURIMap().getURI(num).equals("http://human.owl#NCI_C12219"))
							continue;*/
						System.out.println("***************************");
						System.out.println(num); 
						System.out.println(aml.getURIMap().getURI(num));
						System.out.println("***************************");
					}
					commonEntailment.put(path1, ancestor2);
					commonEntailment.put(path2, ancestor2);
				}			
			}
		}
		return commonEntailment;
	}
}




