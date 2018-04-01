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
* Map of extended relationships of classes involved in disjoint clauses with  *
* mappings from a given Alignment, which supports repair of that Alignment.   *
*                                                                             *
* @authors Daniel Faria & Emanuel Santos                                      *
******************************************************************************/
package aml.filter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringEscapeUtils;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.RelationshipMap;
import aml.settings.MappingRelation;
import aml.settings.MappingStatus;
import aml.util.Graph;
import aml.util.KG;
import aml.util.LinearProgram;
import aml.util.Pair;
import aml.util.Table2Set;
import aml.util.Table3List;
import aml.util.Table3Set;

public class RepairMapRefine implements Iterable<Integer>
{
	
//Attributes
	
	private AML aml;
	private RelationshipMap rels;
	private Alignment a;
	//The list of classes that are relevant for coherence checking
	private HashSet<Integer> classList;
	//The list of classes that must be checked for coherence
	private HashSet<Integer> checkList;
	//The  map of parents relations of checkList classes
	//(class Id, Set of parents class Id)
	private HashMap<Integer,Set<Integer>> parentMap;
	//The minimal map of ancestor relations of checkList classes
	//(checkList class Id, classList class Id, Path)	
	private Table3List<Integer,Integer,Path> ancestorMap;  //存储的path都是仅仅含有mapping 的path
	//The length of ancestral paths to facilitate transitive closure
	//(checklist class Id, Path length, classList class Id)
	
	//下面的数据结构可能都需要砍掉，甚至ancestorMap都需要优化一下
	private Table3Set<Integer,Integer,Integer> pathLengths;  //这个path，仅仅是含有mapping的path	
	//The number of paths to disjoint classes
	private int pathCount;
	//The list of conflict sets
	private Vector<Path> conflictSets;
	//The table of conflicts per mapping
	private Table2Set<Integer,Integer> conflictMappings;
	private Table2Set<Integer,Integer> mappingConflicts;

	//The available CPU threads
	private int threads;
	
	public Graph graph;
	//public HashSet<MIPP> MinimalConflictSet;
	public HashSet<Pair<ArrayList<Integer>,ArrayList<Integer>>> MinimalConflictSet;
	//public ConcurrentSkipListSet<Pair<ArrayList<Integer>,ArrayList<Integer>>> MinimalConflictSet;
	public HashMap<Integer,ArrayList<Pair<ArrayList<Integer>,ArrayList<Integer>>>> MapMinimalConflictSet;
	public ConcurrentHashMap<String,Boolean> checkedState;  	//it can reduce the time consumption
	public HashMap<Integer,Set<Integer>> ancestors;
	
	
//Constructors
	
	/**
	 * Constructs a new RepairMap
	 */
	public RepairMapRefine()
	{
		aml = AML.getInstance();
		rels = aml.getRelationshipMap();
		//We use a clone of the alignment to avoid problems if the
		//order of the original alignment is altered
		a = new Alignment(aml.getAlignment());
		//Remove the FLAGGED status from all mappings that have it
		for(Mapping m : a)
			if(m.getStatus().equals(MappingStatus.FLAGGED))
				m.setStatus(MappingStatus.UNKNOWN);
		threads = Runtime.getRuntime().availableProcessors();
		graph=new Graph();
		init();
	}
	
//Public Methods
	
	/**
	 * @param index: the index of the Mapping to get
	 * @return the conflict sets that contain the given Mapping index
	 */
	public Set<Integer> getConflicts(int index)
	{
		return mappingConflicts.get(index);
	}
	
	/**
	 * @param m: the Mapping to get
	 * @return the list of Mappings in conflict with this Mapping
	 */
	public Vector<Mapping> getConflictMappings(Mapping m)
	{
		int index = a.getIndex(m.getSourceId(), m.getTargetId());
		Vector<Mapping> confs = new Vector<Mapping>();
		if(!mappingConflicts.contains(index))
			return confs;
		for(Integer i : mappingConflicts.get(index))
		{
			for(Integer j : conflictMappings.get(i))
			{
				if(j == index)
					continue;
				Mapping n = a.get(j);
				//Get the Mapping from the original alignment
				n = aml.getAlignment().get(n.getSourceId(), n.getTargetId());
				if(!confs.contains(n))
					confs.add(n);
			}
		}
		return confs;
	}
	
	/**
	 * @return the list of conflict sets of mappings
	 * in the form of indexes (as per the alignment
	 * to repair)
	 */
	public Vector<Path> getConflictSets()
	{
		return conflictSets;
	}
	
	/**
	 * @return the list of minimal incoherence conflict set (as per the alignment
	 * to repair deeply)
	 */
	/*public HashSet<MIPP> getMinimalConflictSets()
	{
		return MinimalConflictSet;
	}*/
	
	public HashSet<Pair<ArrayList<Integer>, ArrayList<Integer>>> getMinimalConflictSets()
	{
		return MinimalConflictSet;
	}
	
	/**
	 * @return the list of minimal incoherence conflict set (as per the alignment
	 * to repair deeply)
	 */
	public HashMap<Integer, ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>>> getMapMinimalConflictSets()
	{
		return MapMinimalConflictSet;
	}
	
	/**
	 * @param m: the Mapping to search in the RepairMap
	 * @return the index of the Mapping in the RepairMap
	 */
	public int getIndex(Mapping m)
	{
		return a.getIndex(m.getSourceId(), m.getTargetId());
	}
	
	/**
	 * @param source: the id of the source class to search in the RepairMap
	 * @param target: the id of the target class to search in the RepairMap
	 * @return the index of the Mapping between source and target in
	 * the RepairMap
	 */
	public int getIndex(int source, int target)
	{
		return a.getIndex(source, target);
	}

	/**
	 * @param index: the index of the Mapping to get
	 * @return the Mapping at the given index
	 */
	public Mapping getMapping(int index)
	{
		Mapping m = a.get(index);
		return aml.getAlignment().get(m.getSourceId(), m.getTargetId());
	}
	
	/**
	 * @return whether the alignment is coherent
	 */
	public boolean isCoherent()
	{
		//return conflictSets == null || conflictSets.size() == 0;
		return MinimalConflictSet==null ||MinimalConflictSet.size()==0;
	}
	
	
	@Override
	public Iterator<Integer> iterator()
	{
		return mappingConflicts.keySet().iterator();
	}
	
	/**
	 * Sets a Mapping as incorrect and revise its conflicts from
	 * the RepairMapRefine (but does not actually remove the Mapping from
	 * the Alignment)
	 * @param index: the index of the Mapping to revise
	 */
	public boolean reviseJudge(int index)
	{
		ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>> mipps=MapMinimalConflictSet.get(index); 
		System.out.println("The number of related Minimal conflict sets is "+ mipps.size());
		ArrayList<KG> KGs= new ArrayList<KG>();		
	    for(Pair<ArrayList<Integer>, ArrayList<Integer>>  pair: MapMinimalConflictSet.get(index))
	    {
	    	/*System.out.println("-------------------------------");
	    	System.out.println("The two paths are:");
	    	for(Integer num:pair.getLeft())
	    		System.out.print(aml.getURIMap().getURI(num)+" ->");
	    	System.out.println();
	    	for(Integer num:pair.getRight())
	    		System.out.print(aml.getURIMap().getURI(num)+"->");
	    	System.out.println();
	    	System.out.println("The mappings with common entailments:");   	
	    	for(Integer num:pair.getCommonEntailment().keySet())
	    	{
	    		System.out.print(a.get(num).toString());
	    	}*/
	    	KG mipp=generateKG(pair.left,pair.right);
	    	KGs.add(mipp);
	    	//System.out.println("-------------------------------");
	    }	
		ArrayList<Integer> sourceConcepts=new ArrayList<Integer> ();
		ArrayList<Integer> targetConcepts=new ArrayList<Integer> ();
		Map<Integer, Set<Integer>> sourceRel =new HashMap<Integer, Set<Integer>>();
		Map<Integer, Set<Integer>> targetRel =new HashMap<Integer, Set<Integer>>();
		ArrayList<Integer> mappings=new ArrayList<Integer>();
				
		for(KG kg:KGs)
		{
			for(Integer concept:kg.getSourceConcept())
			{
				if(!sourceConcepts.contains(concept))
					sourceConcepts.add(concept);
			}
			for(Integer concept:kg.getTargetConcept())
			{
				if(!targetConcepts.contains(concept))
					targetConcepts.add(concept);
			}
			for(String relation:kg.getSourceRel())
			{
				String parts[]=relation.split("--");
				int sub=Integer.parseInt(parts[0]);
				int parent=Integer.parseInt(parts[1]);
				if (sourceConcepts.contains(parent)) 
				{
					if (sourceRel.keySet().contains(sub)) 
					{
						Set<Integer> set = sourceRel.get(sub);
						set.add(parent);
						sourceRel.put(sub, set);
					} else {
						Set<Integer> set = new HashSet<Integer>();
						set.add(parent);
						sourceRel.put(sub, set);
					}
				}				
			}
			for(String relation:kg.getTargetRel())
			{
				String parts[]=relation.split("--");
				int sub=Integer.parseInt(parts[0]);
				int parent=Integer.parseInt(parts[1]);
				if (targetConcepts.contains(parent)) 
				{
					if (targetRel.keySet().contains(sub)) 
					{
						Set<Integer> set = targetRel.get(sub);
						set.add(parent);
						targetRel.put(sub, set);
					} else {
						Set<Integer> set = new HashSet<Integer>();
						set.add(parent);
						targetRel.put(sub, set);
					}
				}		
				
			}
			for(Integer m:kg.getMappings())
			{
				if(!mappings.contains(m))
					mappings.add(m);
			}
		}
		
		Map<Integer, Set<Integer>> sourceDisRel =new HashMap<Integer, Set<Integer>>();
		Map<Integer, Set<Integer>> targetDisRel =new HashMap<Integer, Set<Integer>>();
		
		for(Integer con:sourceConcepts)
		{
			for(Integer dis:rels.getDisjoint(con))
			{
				if(sourceConcepts.contains(dis))  //不是模块化内部的概念不考虑
				{
					if(sourceDisRel.keySet().contains(con))
					{
						Set<Integer> map=sourceDisRel.get(con);
						map.add(dis);
						sourceDisRel.put(con, map);
					}
					else
					{
						Set<Integer> map =new HashSet<Integer>();
						map.add(dis);
						sourceDisRel.put(con, map);
					}
				}
			}
		}
		
		for(Integer con:targetConcepts)
		{
			for(Integer dis:rels.getDisjoint(con))
			{
				if(targetConcepts.contains(dis))  //不是模块化内部的概念不考虑
				{
					if(targetDisRel.keySet().contains(con))
					{
						Set<Integer> map=targetDisRel.get(con);
						map.add(dis);
						targetDisRel.put(con, map);
					}
					else
					{
						Set<Integer> map =new HashSet<Integer>();
						map.add(dis);
						targetDisRel.put(con, map);
					}
				}
			}
		}

		LinearProgram LP1= new LinearProgram();
		LinearProgram LP2= new LinearProgram();
		LinearProgram LPGlobal= new LinearProgram();

		if(sourceConcepts.size()>=25||targetConcepts.size()>25)  //因为计算复杂度而做的妥协
		{
			revise(index,0.1);
			return true;			
		}
		
		LP1.Intial(sourceConcepts);
		LP1.encodingConstraint(sourceRel,sourceDisRel);	
		LP1.generatePossbileWorld();
		LP1.generatePossbileWorldIndex();

		LP2.Intial(targetConcepts);
		LP2.encodingConstraint(targetRel,targetDisRel);
		LP2.generatePossbileWorld();
		LP2.generatePossbileWorldIndex();
		
		int MergedWorldNumber=LP1.getPossibleWordIndex().size()*LP2.getPossibleWordIndex().size();
		if(MergedWorldNumber>=10000)  //因为计算复杂度而做的妥协（这个值可以调整）
		{
			revise(index,0.1);
			return true;			
		}
		
		ArrayList<String> conditionalConstraints=new ArrayList<String>();
		ArrayList<String> initialMappings=new ArrayList<String>();
		
		for(Integer m:mappings)
		{
			//String string="";
			Mapping map=a.get(m);
			if(map.getRelationship().toString().equals("="))
				initialMappings.add(map.getSourceId()+","+map.getTargetId()+","+map.getRelationship().toString()+","+map.getSimilarity()+","+1.0);
			else if(map.getRelationship().toString().equals("<")) //主要是针对logMap
				initialMappings.add(map.getSourceId()+","+map.getTargetId()+","+"|"+","+map.getSimilarity()+","+1.0);
			else if(map.getRelationship().toString().equals(">")) //主要是针对logMap
				initialMappings.add(map.getTargetId()+","+map.getSourceId()+","+"|"+","+map.getSimilarity()+","+1.0);
			System.out.println("The index of the mapping is "+ m);
			System.out.println(map.getSourceId()+","+map.getTargetId()+","+map.getRelationship().toString()+","+map.getSimilarity()+","+1.0);
		}
		//在原始的冲突中做移除处理	
		LPGlobal.mergeConcept(sourceConcepts,targetConcepts);
		LPGlobal.mergePossbileWorldIndex(LP1.getPossibleWordIndex(),LP2.getPossibleWordIndex());
		LPGlobal.addhardConstraints(conditionalConstraints);	
		LPGlobal.addMapping(initialMappings);  //之前已经添加过一次了
		
		Boolean flag=LPGlobal.isProSatifiable(initialMappings);
		System.out.println("The mappings are coherent？:  "+flag);
		if(flag==false)  //不可解，需要进行修复
		{
			Mapping RevisedMap=a.get(index);
			String mapInformation=RevisedMap.getSourceId()+","+RevisedMap.getTargetId()+","+RevisedMap.getRelationship().toString()+","+RevisedMap.getSimilarity()+","+1.0;
			LPGlobal.Revised(mapInformation);				
			String parts[] = LPGlobal.getRevisedMapping().split(",");
			revise(index,Double.parseDouble(parts[4]));
			return true;
		}
		else //无矛盾，啥都不修改
		{
			remove(index,false);
			return false;
		}
			
	}

	/**
	 * Sets a Mapping as incorrect and removes its conflicts from
	 * the RepairMap (but does not actually remove the Mapping from
	 * the Alignment)
	 * @param index: the index of the Mapping to remove
	 */
	public void remove(int index,boolean flag)
	{
		System.out.println("The number of related Minimal conflict sets is "+ MapMinimalConflictSet.get(index).size());	
		HashSet<Pair<ArrayList<Integer>, ArrayList<Integer>>> MIPPSet= new HashSet(MapMinimalConflictSet.get(index));
		MinimalConflictSet.removeAll(MapMinimalConflictSet.get(index));
		MapMinimalConflictSet.remove(index);
		Iterator<Entry<Integer, ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>>>> iter= MapMinimalConflictSet.entrySet().iterator();
		while(iter.hasNext())
		{
			Integer  r=iter.next().getKey();	
				LinkedList<Pair<ArrayList<Integer>, ArrayList<Integer>>> leftMIPP=removeAllMIPP(MapMinimalConflictSet.get(r),MIPPSet);
				if(leftMIPP.isEmpty())  //为空则直接移除
				{
					//MapMinimalConflictSet.remove(r);
					iter.remove();
				}
				else  //更新原来的MIPPSets的大小
				{
					ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>> MIPPSets=new ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>>(leftMIPP);
					MapMinimalConflictSet.put(r, MIPPSets);
				}
		}
		if(flag==true)
		{
			Mapping m = a.get(index);
			m.setStatus(MappingStatus.INCORRECT);
			aml.getAlignment().get(m.getSourceId(), m.getTargetId()).setStatus(MappingStatus.INCORRECT);
		}
	}
	
	public void revise(int index,double sim)
	{
		System.out.println("The number of related Minimal conflict sets is "+ MapMinimalConflictSet.get(index).size());	
		HashSet<Pair<ArrayList<Integer>, ArrayList<Integer>>> MIPPSet= new HashSet(MapMinimalConflictSet.get(index));
		MinimalConflictSet.removeAll(MapMinimalConflictSet.get(index));
		MapMinimalConflictSet.remove(index);
		Iterator<Entry<Integer, ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>>>> iter= MapMinimalConflictSet.entrySet().iterator();
		while(iter.hasNext())
		{
			Integer  r=iter.next().getKey();	
				LinkedList<Pair<ArrayList<Integer>, ArrayList<Integer>>> leftMIPP=removeAllMIPP(MapMinimalConflictSet.get(r),MIPPSet);
				if(leftMIPP.isEmpty())  //为空则直接移除
				{
					//MapMinimalConflictSet.remove(r);
					iter.remove();
				}
				else  //更新原来的MIPPSets的大小
				{
					ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>> MIPPSets=new ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>>(leftMIPP);
					MapMinimalConflictSet.put(r, MIPPSets);
				}
		}	
		Mapping m = a.get(index);
		m.setStatus(MappingStatus.UNKNOWN);			
		MappingRelation rel = MappingRelation.parseRelation(StringEscapeUtils.unescapeXml("?"));
		m.setRelationship(rel);
		m.setSimilarity(sim);
		aml.getAlignment().get(m.getSourceId(), m.getTargetId()).setSimilarity(sim);
		aml.getAlignment().get(m.getSourceId(), m.getTargetId()).setStatus(MappingStatus.UNKNOWN);
		aml.getAlignment().get(m.getSourceId(), m.getTargetId()).setRelationship(rel);
	}
	
	

	public LinkedList <Pair<ArrayList<Integer>, ArrayList<Integer>>> removeAllMIPP(ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>> src, HashSet<Pair<ArrayList<Integer>, ArrayList<Integer>>> othHash) 
	{
		LinkedList <Pair<ArrayList<Integer>, ArrayList<Integer>>> result = new LinkedList(src);// 大集合用linkedlist
		//HashSet othHash = new HashSet(oth);// 小集合用hashset
		Iterator iter = result.iterator();// 采用Iterator迭代器进行数据的操作
		while (iter.hasNext()) 
		{
			if (othHash.contains(iter.next())) 
			{
				iter.remove();
			}
		}
		return result;
	}
	
	
	/**
	 * Saves the list of minimal conflict sets to a text file
	 * @param file: the path to the file where to save
	 * @throws FileNotFoundException if unable to create/open file
	 */
	public void saveConflictSets(String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		int id = 1;
		for(Path p : conflictSets)
		{
			outStream.println("Conflict Set " + id++ + ":");
			for(Integer i : p)
				outStream.println(a.get(i).toString());
		}
		outStream.close();
	}
	
//Private Methods
	
	//Builds the RepairMap
	private void init()
	{
		System.out.println("Building Repair Map");
		long globalTime = System.currentTimeMillis()/1000;
		//Initialize the data structures
		classList = new HashSet<Integer>();
		checkList = new HashSet<Integer>();
		ancestorMap = new Table3List<Integer,Integer,Path>();	
		pathLengths = new Table3Set<Integer,Integer,Integer>();
		conflictSets = new Vector<Path>();
		parentMap=new HashMap<Integer,Set<Integer>>();
		ancestors=new HashMap<Integer,Set<Integer>>();
		MinimalConflictSet=new HashSet<Pair<ArrayList<Integer>,ArrayList<Integer>>>();
		//MinimalConflictSet=new ArrayList<>();
		//MinimalConflictSetMap=new ConcurrentHashMap<ArrayList<Integer>,MIPP>();
		//MinimalConflictSetMap=new ConcurrentSkipListSet<Integer>();
		//MapMinimalConflictSet=new HashMap<Integer,ArrayList<MIPP>>();
		MapMinimalConflictSet=new HashMap<Integer,ArrayList<Pair<ArrayList<Integer>,ArrayList<Integer>>>>();
		checkedState =new ConcurrentHashMap<String,Boolean>();
		
		//Build the classList, starting with the classes
		//involved in disjoint clauses
		classList.addAll(rels.getDisjoint());
		//If there aren't any, there is nothing else to do
		if(classList.size() == 0)
		{
			System.out.println("Nothing to repair!");
			return;
		}
		//Otherwise, add all classes involved in mappings
		for(Integer i : a.getSources())
			if(AML.getInstance().getURIMap().isClass(i))
				classList.add(i);
		for(Integer i : a.getTargets())
			if(AML.getInstance().getURIMap().isClass(i))
				classList.add(i);
		
		//Then build the checkList
		long localTime = System.currentTimeMillis()/1000;
		buildCheckList();
		System.out.println("Computed check list in " + 
				(System.currentTimeMillis()/1000-localTime) + " seconds");
		HashSet<Integer> t = new HashSet<Integer>(classList);
		t.addAll(checkList);	
		System.out.println("Core fragments: " + t.size() + " classes");
		t.clear();
		System.out.println("The number of subClassOf relationship is: " + parentMap.size());
		RefineParentMap();		
		//构建一个图
		graph.init(parentMap);	
		System.out.println("Check list: " + checkList.size() + " classes to check");
		//Build the ancestorMap with transitive closure
		localTime = System.currentTimeMillis()/1000;
		buildAncestorMap();
		//buildAncestorMap2();  //效率是十分低
		System.out.println("Computed ancestral paths in " + 
				(System.currentTimeMillis()/1000-localTime) + " seconds");
		System.out.println("Paths to process: " + pathCount);
		
		//And finally, get the list of conflict sets
		localTime = System.currentTimeMillis()/1000;
		//buildConflictSets();  //多线程的办法
		buildMIPPs();  //单线程的办法
		System.out.println("Computed minimal conflict sets in " + 
				(System.currentTimeMillis()/1000-localTime) + " seconds");
		System.out.println("Sets of conflicting mappings: " + conflictSets.size());
		int id=1;
		for(Path p : conflictSets)
		{
			System.out.println("Conflict Set " + id++ + ":");
			for(Integer i : p)
				System.out.println(a.get(i).toString());
		}	
		System.out.println("Repair Map finished in " +
				(System.currentTimeMillis()/1000-globalTime) + " seconds");
		//System.out.println("The minimal incoherence path-pairs are :");
		
		/*Iterator<MIPP> iterator = MinimalConflictSet.iterator();
		while (iterator.hasNext()) 
		{
			MIPP mipp=iterator.next();
			type type = (type) iterator.nextElement();
			
		}*/
		/*for(ArrayList<Integer> map:MinimalConflictSetMap.keySet())
		{
			MinimalConflictSet.add(MinimalConflictSetMap.get(map));
		}*/
		
		/*for(MIPP mipp:MinimalConflictSet)
		{
			mipp.print();
		}*/
		//buildClusters();
		System.out.println("The size of MinimalConflictSet is "+ MinimalConflictSet.size());
		System.out.println("The size of MapMinimalConflictSet is "+ MapMinimalConflictSet.size());
		
	}
	
	//Computes the list of classes that must be checked for coherence
	private void buildCheckList()
	{
		//Start with the descendants of classList classes that have
		//either 2+ parents with a classList class in their ancestral
		//line or are involved in a disjoint class and have 1+ parents
		HashSet<Integer> descList = new HashSet<Integer>();
		for(Integer i: classList)
		{
			//Store the parentRelations
			//parentMap.put(i, rels.getSuperClasses(i,false));
			//parentMap.put(i, rels.getSuperClasses(i,true));
			//Get the subClasses of classList classes
			for(Integer j : rels.getSubClasses(i,false))
			{
				//Count their parents
				Set<Integer> pars = rels.getSuperClasses(j, true);
				//Check if they have a disjoint clause
				int hasDisjoint = 0;
				if(rels.hasDisjoint(j))
					hasDisjoint = 1;
				//Exclude those that don't have at least two parents
				//or a parent and a disjoint clause
				if(pars.size() + hasDisjoint < 2)
					continue;
				//Count the classList classes in the ancestral
				//line of each parent (or until two parents with
				//classList ancestors are found)
				int count = hasDisjoint;
				for(Integer k : pars)
				{
					if(classList.contains(k))
						count++;
					else
					{
						for(Integer l : rels.getSuperClasses(k, false))
						{
							if(classList.contains(l))
							{
								count++;
								break;
							}
						}
					}
					if(count > 1)
						break;
				}
				//Add those that have at least 2 classList
				//classes in their ancestral line
				if(count > 1)
					descList.add(j);
			}
		}

		
	
		//System.out.println(parentMap.keySet().contains(12));
		
		
		//Filter out classes that have a descendant in the descList
		//or a mapped descendant
		HashSet<Integer> toRemove = new HashSet<Integer>();
		for(Integer i : descList)
		{
			for(Integer j : rels.getSubClasses(i, false))
			{
				if(descList.contains(j) || a.containsClass(j))
				{
					toRemove.add(i);
					break;
				}
			}
		}
		descList.removeAll(toRemove);
		//And those that have the same set or a subset of
		//classList classes in their ancestor line
		toRemove = new HashSet<Integer>();
		Vector<Integer> desc = new Vector<Integer>();
		Vector<Path> paths = new Vector<Path>();
		for(Integer i : descList)
		{
			//Put the classList ancestors in a path
			Path p = new Path();
			for(Integer j : rels.getSuperClasses(i,false))
				if(classList.contains(j))
					p.add(j);
			//Put the class itself in the path if it
			//is also in classList
			if(classList.contains(i))
				p.add(i);			
			
			boolean add = true;
			//Check if any of the selected classes
			for(int j = 0; j < desc.size() && add; j++)
			{
				//subsumes this class (if so, skip it)
				if(paths.get(j).contains(p))
					add = false;
				//is subsumed by this class (if so,
				//remove the latter and proceed)
				else if(p.contains(paths.get(j)))
				{
					desc.remove(j);
					paths.remove(j);
					j--;
				}
			}
			//If no redundancy was found, add the class
			//to the list of selected classes
			if(add)
			{
				desc.add(i);
				paths.add(p);
			}
		}
		//Add all selected classes to the checkList
		checkList.addAll(desc);
		
		//Complete the descendant Classes and their relationship
		//Complete the missing superClasses relationship
		
		
		
		//Now get the list of all mapped classes that are
		//involved in two mappings or have an ancestral
		//path to a mapped class, from only one side
		HashSet<Integer> mapList = new HashSet<Integer>();
		for(Mapping m : a)
		{
			int source = m.getSourceId();
			int target = m.getTargetId();
			//Check if there is no descendant in the checkList
			boolean isRedundant = false;
			HashSet<Integer> descendants = new HashSet<Integer>(rels.getSubClasses(source, false));
			descendants.addAll(rels.getSubClasses(target, false));
			
			//Store the parentRelations according to the mappings
			/*if(parentMap.keySet().contains(source))
				parentMap.get(source).add(target);
			else
			{
				Set<Integer> set=new HashSet<Integer>();
				set.add(target);
				parentMap.put(source,set);
			}
			if(parentMap.keySet().contains(target))
				parentMap.get(target).add(source);
			else
			{
				Set<Integer> set=new HashSet<Integer>();
				set.add(source);
				parentMap.put(target,set);
			}
			*/			
			for(Integer i : descendants)
			{
				if(checkList.contains(i))
				{
					isRedundant = true;
					break;
				}
			}
			if(isRedundant)
				continue;
			//Count the mappings of both source and target classes
			int sourceCount = a.getSourceMappings(source).size();
			int targetCount = a.getTargetMappings(target).size();
			//If the target class has more mappings than the source
			//class (which implies it has at least 2 mappings) add it
			if(targetCount > sourceCount)
				mapList.add(target);
			//If the opposite is true, add the target
			else if(sourceCount > targetCount || sourceCount > 1)
				mapList.add(source);
			//Otherwise, check for mapped ancestors on both sides
			else
			{
				for(Integer j : rels.getSuperClasses(source, false))
					if(a.containsSource(j))
						sourceCount++;
				for(Integer j : rels.getSuperClasses(target, false))
					if(a.containsTarget(j))
						targetCount++;
				if(sourceCount > 1 && targetCount < sourceCount)
					mapList.add(source);
				else if(targetCount > 1)
					mapList.add(target);
			}
		}
		toRemove = new HashSet<Integer>();
		for(Integer i : mapList)
		{
			for(Integer j : rels.getSubClasses(i, false))
			{
				if(mapList.contains(j))
				{
					toRemove.add(i);
					break;
				}
			}
		}
		mapList.removeAll(toRemove);
		//Finally, add the mapList to the checkList
		checkList.addAll(mapList);
		
		HashSet<Integer> t = new HashSet<Integer>(classList);
		t.addAll(checkList);
		for(Integer num:t)
		{
			if(!parentMap.keySet().contains(num))
				parentMap.put(num, rels.getSuperClasses(num,true));
		}	
		int size = 0;
		while (size < parentMap.size()) 
		{
			size = parentMap.size();
			HashMap<Integer, Set<Integer>> teMap = new HashMap<Integer, Set<Integer>>();
			teMap.putAll(parentMap);
			for (Integer child : teMap.keySet()) 
			{
				Set<Integer> parents = teMap.get(child);
				for (Integer parent : parents) 
				{
					if (!teMap.keySet().contains(parent)) 
					{						
						parentMap.put(parent, rels.getSuperClasses(parent, true));
					}
					if (checkList.contains(parent) || classList.contains(parent))
						continue;
				}
			}
			teMap.clear();
		}	
	}

	private void RefineParentMap()
	{
		HashSet<Integer> needConcetps=new HashSet<Integer>();
		needConcetps.addAll(classList);
		needConcetps.addAll(checkList);
		HashMap<Integer, Set<Integer>> SourceParentMap=new HashMap<Integer, Set<Integer>>();
		HashMap<Integer, Set<Integer>> TargetParentMap=new HashMap<Integer, Set<Integer>>();
		Set<Integer> sourceConcept=aml.getSource().getClasses();	
		for(Integer node: parentMap.keySet())
		{
			if(sourceConcept.contains(node))
			{
				SourceParentMap.put(node, parentMap.get(node));
			}
			else 
			{
				TargetParentMap.put(node, parentMap.get(node));
			}
		}	
		parentMap.clear();
		Graph sourceGraph=new Graph();	
		sourceGraph.init(SourceParentMap);		
		for(Integer node: SourceParentMap.keySet())
		{
			Set<Integer> CoreParents=new HashSet<Integer>();
			Set<Integer> ancestors=rels.getSuperClasses(node, false);		
			for(Integer anc: ancestors)
			{
				if(CoreParents.contains(anc)) //即该点的路径其实已经遍历过了(中间的结点通常在遍历祖先结点的时都访问到了)
					continue;
				List<ArrayList<Integer>> getPaths=sourceGraph.getPaths(node, anc);
				for(ArrayList<Integer> path: getPaths)
				{
					for(Integer n: path)
					{
						if (needConcetps.contains(n)&&n!=node) 
						{
							CoreParents.add(n);
							break;
						}
						if(CoreParents.contains(n))  //避免重复检测
						{
							break;
						}
					}
				}
			}		
			parentMap.put(node, CoreParents);		
		}		
		Graph targetGraph=new Graph();
		targetGraph.init(TargetParentMap);		
		for(Integer node: TargetParentMap.keySet())
		{
			Set<Integer> CoreParents=new HashSet<Integer>();
			Set<Integer> ancestors=rels.getSuperClasses(node, false);		
			for(Integer anc: ancestors)
			{
				if(CoreParents.contains(anc)) //即该点的路径其实已经遍历过了(中间的结点通常在遍历祖先结点的时都访问到了)
					continue;
				List<ArrayList<Integer>> getPaths=targetGraph.getPaths(node, anc);
				for(ArrayList<Integer> path: getPaths)
				{
					for(Integer n: path)
					{
						if (needConcetps.contains(n)&&n!=node) 
						{
							CoreParents.add(n);
							break;
						}
						if(CoreParents.contains(n))  //避免重复检测
						{
							break;
						}
					}
				}
			}
			parentMap.put(node, CoreParents);		
		}	
		sourceGraph.clear();
		targetGraph.clear();
		for(Mapping m : a)
		{
			int source = m.getSourceId();
			int target = m.getTargetId();		
			//Store the parentRelations according to the mappings
			if(parentMap.keySet().contains(source))
				parentMap.get(source).add(target);
			else
			{
				Set<Integer> set=new HashSet<Integer>();
				set.add(target);
				parentMap.put(source,set);
			}
			if(parentMap.keySet().contains(target))
				parentMap.get(target).add(source);
			else
			{
				Set<Integer> set=new HashSet<Integer>();
				set.add(source);
				parentMap.put(target,set);
			}

		}
	}

	//Builds the map of ancestral relations between all classes
	//in the checkList and all classes in the classList, with
	//(breadth first) transitive closure
	private void buildAncestorMap()
	{
		//First get the "direct" relations between checkList
		//and classList classes, which are present in the
		//RelationshipMap, plus the relations through direct
		//mappings of checkList classes
		for(Integer i : checkList)
		{
			//System.out.println(i);
			//Direct relations
			Set<Integer> ancs = rels.getSuperClasses(i,false);
			for(Integer j : ancs)
				if(classList.contains(j))
					addRelation(i, j, new Path());
			//Mappings
			Set<Integer> maps = a.getMappingsBidirectional(i);
			for(Integer j : maps)
			{
				//Get both the mapping and its ancestors
				int index = a.getIndexBidirectional(i, j);
				HashSet<Integer> newAncestors = new HashSet<Integer>(rels.getSuperClasses(j,false));
				newAncestors.add(j);
				//And add them
				for(Integer m : newAncestors)
					if(classList.contains(m))
						addRelation(i,m,new Path(index));
			}
		}
		//Then add paths iteratively by extending paths with new
		//mappings, stopping when the ancestorMap stops growing
		int size = 0;
		for(int i = 0; size < ancestorMap.size(); i++)
		{
			size = ancestorMap.size();
			//For each class in the checkList
			for(Integer j : checkList)
			{
				//If it has ancestors through paths with i mappings
				if(!pathLengths.contains(j, i))
					continue;
				//We get those ancestors
				HashSet<Integer> ancestors = new HashSet<Integer>(pathLengths.get(j,i));
				//For each such ancestor
				for(Integer k : ancestors)
				{
					//Cycle check 1 (make sure ancestor != self)
					if(k == j)
						continue;
					//Get the paths between the class and its ancestor
					HashSet<Path> paths = new HashSet<Path>();
					for(Path p : ancestorMap.get(j, k))
						if(p.size() == i)
							paths.add(p);
					//Get the ancestor's mappings
					Set<Integer> maps = a.getMappingsBidirectional(k);
					//And for each mapping
					for(Integer l : maps)
					{
						//Cycle check 2 (make sure mapping != self)
						if(l == j)
							continue;
						//We get its ancestors
						int index = a.getIndexBidirectional(k, l);
						HashSet<Integer> newAncestors = new HashSet<Integer>(rels.getSuperClasses(l,false));
						//Plus the mapping itself
						newAncestors.add(l);
						//Now we must increment all paths between j and k
						for(Path p : paths)
						{
							//Cycle check 3 (make sure we don't go through the
							//same mapping twice)
							if(p.contains(index))
								continue;
							//We increment the path by adding the new mapping
							Path q = new Path(p);
							q.add(index);
							//And add a relationship between j and each descendant of
							//the new mapping (including the mapping itself) that is
							//on the checkList
							for(Integer m : newAncestors)
								//Cycle check 4 (make sure mapping descendant != self)
								if(classList.contains(m) && m != j)
									addRelation(j,m,q);
						}
					}
				}
			}
		}
		//Finally add relations between checkList classes and
		//themselves when they are involved in disjoint clauses
		//(to support the buildClassConflicts method)
		for(Integer i : checkList)
			if(rels.hasDisjoint(i))
				ancestorMap.add(i, i, new Path());
		
		
		/*for(Integer des: ancestorMap.keySet())
		{
			Set<Integer> concepts=new HashSet<>(ancestorMap.keySet(des));
			ancestors.put(des, concepts);
		}*/
		
		Graph tempGraph=new Graph();
		tempGraph.init(parentMap);
			
		for(Integer des: ancestorMap.keySet())
		{
			Set<Integer> concepts=new HashSet<>(ancestorMap.keySet(des));
			concepts.remove(des);
			for(Integer dis: rels.getDisjoint())
			{
				if(concepts.contains(dis)||dis==des)
					continue;
				List<ArrayList<Integer>> paths=tempGraph.getPaths(des, dis);
				if(!paths.isEmpty())
				{
					concepts.add(dis);											
				}
			}
			ancestors.put(des, concepts);
		}
		//对不相交的集合进行扩充
		
		
		ancestorMap.clear();
		pathLengths.clear();
		
	}
	
	/*private void buildAncestorMap2()
	{
		
		Graph tempGraph=new Graph();
		tempGraph.init(parentMap);
			
		HashSet<Integer> needConcetps=new HashSet<Integer>();
		needConcetps.addAll(classList);
		needConcetps.addAll(checkList);
		
		HashSet<Integer> tempList=new HashSet<Integer>();	
		for(Mapping m : a)
		{
			int source = m.getSourceId();
			int target = m.getTargetId();		
			tempList.add(source);
			tempList.add(target);
		}
				
		for(Integer i : checkList)
		{
			System.out.println("The checking ancestors node is: "+i);
			Set<Integer> set=new HashSet<Integer>();			
			HashSet<Integer> newAncestors = new HashSet<Integer>(rels.getSuperClasses(i,false)); //同源的祖先即祖先
			newAncestors.retainAll(classList);
			set.addAll(newAncestors);
			set.addAll(parentMap.get(i)); //父母即祖先
			ancestors.put(i, set);	
			for(Integer j: classList)
			{								
				if(ancestors.get(i).contains(j))
						continue;
				if(rels.getSubClasses(i, false).contains(j)) //儿子因为没有路径导向，不可能是当面结点的祖先
						continue;
										
				List<ArrayList<Integer>> paths=tempGraph.getPaths(i, j);
				if(!paths.isEmpty())
				{
					ancestors.get(i).add(j);											
				}
			}
		}				
	}*/
	
	//Adds a relation to the ancestorMap (and pathLengths)
	private void addRelation(int child, int parent, Path p)
	{
		if(ancestorMap.contains(child,parent))
		{
			Vector<Path> paths = ancestorMap.get(child,parent);
			for(Path q : paths)
				if(p.contains(q))
					return;
		}
		ancestorMap.add(child,parent,p);
		pathLengths.add(child, p.size(), parent);
		if(rels.hasDisjoint(parent))
			pathCount++;
	}
	
	
	
	private void buildMIPPs()
	{
		long num=0;
		for(Integer i : checkList)
		{
			//Get its minimized conflicts
			System.out.println("########################################");
			System.out.println("The check number is "+num+"/"+checkList.size());
			buildClassConflictPaths3(i);
			num++;			
		}
		//及时释放
		ancestors.clear();
		//System.out.println(aml.getURIMap().getIndex("http://human.owl#NCI_C12219"));
		checkedState.clear();
		for (Pair<ArrayList<Integer>, ArrayList<Integer>> mipp : MinimalConflictSet) 
		{
			mipp.expand(a, rels);
			Set<Integer> set=getMappings(mipp.left,mipp.right);
			//Set<Integer> set=mipp.getMappings();
			for(Integer map: set)
			{
				if(MapMinimalConflictSet.keySet().contains(map))
				    MapMinimalConflictSet.get(map).add(mipp);
				else
				{
					ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>> list=new ArrayList<Pair<ArrayList<Integer>, ArrayList<Integer>>>();
					list.add(mipp);
					MapMinimalConflictSet.put(map, list);
				}
			}
		}			
	}
	
	//Builds the minimal conflict sets for a given checkList class
/*	private Vector<Path> buildClassConflicts(int classId)
	{
		Vector<Path> minimalConflicts = new Vector<Path>();
		//检索过的点就不用再用来检索
		if(checkedState.get(classId))
			return minimalConflicts;
		
		//First get all ancestors involved in disjoint clauses	
		HashSet<Integer> disj = new HashSet<Integer>();
		for(Integer i : ancestorMap.keySet(classId))
			if(rels.hasDisjoint(i))
				disj.add(i);
		
		//Plus the class itself, if it has a disjoint clause
		if(rels.hasDisjoint(classId))
			disj.add(classId);	
		//System.out.println(classId);
		//Then test each pair of ancestors for disjointness
		Vector<Path> classConflicts = new Vector<Path>();
		for(Integer i : disj)
		{
			for(Integer j : rels.getDisjoint(i))
			{
				if(i > j || !disj.contains(j))  //i>j不是很懂为什么要这样设计，可能是对称性的关系，只需要考虑一半即可
					continue;
				if(checkedState.containsKey(classId+"->"+i+"&"+j)||checkedState.containsKey(classId+"->"+j+"&"+i))
					continue;	
				if(checkedState.keySet().contains(classId+"-"+i+"-"+j))
						continue;
				if(checkedState.keySet().contains(classId+"-"+j+"-"+i))
						continue;	
				
				System.out.println("-------------------------------------------------------------");
				System.out.println("不可满足的概念的ID为："+classId+":"+AML.getInstance().getURIMap().getLocalName(classId));
				//可以利用 AML.getInstance().getURIMap().contains(classId)来进行区别类
				System.out.println("不相交的结点1："+i+":"+AML.getInstance().getURIMap().getLocalName(i));
				System.out.println("不相交的结点2："+j+":"+AML.getInstance().getURIMap().getLocalName(j));
				System.out.println("-------------------------------------------------------------");
				System.out.println(classId+"对应的路径为："); //其实是包括所有的父亲结点				
				ArrayList<Integer> Path1 =new ArrayList<Integer>();
				ArrayList<Integer> Path2 =new ArrayList<Integer>();			
				List<ArrayList<Integer>> Paths1=new ArrayList<ArrayList<Integer>>();
				List<ArrayList<Integer>> Paths2=new ArrayList<ArrayList<Integer>>();				
				
				//单线程的做法
				Paths1=graph.getPaths(classId, i);
				Paths2=graph.getPaths(classId, j);
				
				//多线程的做法
				Graph4Path gPath=new Graph4Path();
				gPath.init(graph.getParentMap());
				try
				{	
					
					Paths1=gPath.getPaths(classId, i);
					//graph.printResult();
					//Path1=graph.getPath();
					//printPath(Path1);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				try
				{
					Paths2=gPath.getPaths(classId, j);
					//graph.printResult();
					//Path2=graph.getPath();
					//printPath(Path2);
				}						
				catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("**********************");
				
				//Path,其实指的是对应mapping的编号
				for(Path p : ancestorMap.get(classId, i))
				{
					for(Path q : ancestorMap.get(classId, j))
					{
						Path merged = new Path(p);
						merged.merge(q);
						//Adding the merged path to the list of classConflicts
						//classConflicts.add(merged);
					}
				}
			
				Paths1=FilterPaths(Paths1,ancestorMap.get(classId, i));
				Paths2=FilterPaths(Paths2,ancestorMap.get(classId, j));
				
				//one unsatisfied mapping may cause more than one mipp
				for(ArrayList<Integer> p1:Paths1)
				{					
					for(ArrayList<Integer> p2:Paths2)
					{
						//因为是指针，当Path1的取值发生改变时，会对后面的操作有所影响
						ArrayList<Integer> temp1=new ArrayList<Integer>();
						temp1.addAll(p1);
						//因为是指针，当Path2的取值发生改变时，会对后面的操作有所影响
						ArrayList<Integer> temp2=new ArrayList<Integer>();
						temp2.addAll(p2);
						ArrayList<Integer> common=RefinePath(temp1,temp2); 
														
						MIPP mipp=generateMIPP(temp1,temp2);
						MinimalConflictSet.add(mipp);
						
						ArrayList<Integer> pathIndex=new ArrayList<Integer>();
						pathIndex.addAll(temp1);
						pathIndex.addAll(temp2);	
						int key=pathIndex.hashCode();		
						
						if(MinimalConflictSetMap.add(key))	
						{
							MIPP mipp=generateMIPP2(temp1,temp2);
							MinimalConflictSet.add(mipp);	
							//根据路径的索引方式，我们可以判断从classId到精炼后Path的起点之间的点都已经完全检索过了(注意路径的约束)。
							for(Integer concept: common)
							{
								if(checkList.contains(concept))
									checkedState.put(concept+"-"+i+"-"+j, true);
							}
							int realUnsatisfied=temp1.get(0);
							if(checkList.contains(realUnsatisfied))
								checkedState.put(realUnsatisfied+"-"+i+"-"+j, true);
						}
						//MinimalConflictSetMap.put(key, mipp);
					}
				}

				System.out.println("过滤后的路径为:"); //其实是包括所有的父亲结点
				System.out.println("**********************");
				printResult(Paths1);
				System.out.println("**********************");
				printResult(Paths2);
				System.out.println("**********************");
				System.out.println(checkedState.size());
								
			}
		}
		//Then sort that list
		Collections.sort(classConflicts);
		//And turn it into a minimal list
		//Vector<Path> minimalConflicts = new Vector<Path>();
		for(Path p : classConflicts)
			addConflict(p, minimalConflicts);	
		return minimalConflicts;
	}*/
	
/*	private void buildClassConflictPaths(int classId)
	{	
		//First get all ancestors involved in disjoint clauses	
		HashSet<Integer> disj = new HashSet<Integer>();
		for(Integer i : ancestorMap.keySet(classId))
			if(rels.hasDisjoint(i))
				disj.add(i);
		
		
		for(Integer i : ancestors.get(classId))
			if(rels.hasDisjoint(i))
				disj.add(i);
		
		//Plus the class itself, if it has a disjoint clause
		if(rels.hasDisjoint(classId))
			disj.add(classId);	
		//System.out.println(classId);
		for(Integer i : disj)
		{
			List<ArrayList<Integer>> Paths1=new ArrayList<ArrayList<Integer>>();
			Paths1=graph.getPaths(classId, i);
			Paths1=FilterPaths(Paths1);
			for(Integer j : rels.getDisjoint(i))
			{
				if(!disj.contains(j))  //i>j不是很懂为什么要这样设计，可能是对称性的关系，只需要考虑一半即可
					continue;
				if(i > j || !disj.contains(j))  //i>j不是很懂为什么要这样设计，可能是对称性的关系，只需要考虑一半即可
					continue;
				if(checkedState.keySet().contains(classId+"-"+i+"-"+j))
						continue;
				if(checkedState.keySet().contains(classId+"-"+j+"-"+i))
						continue;	
				
				System.out.println("-------------------------------------------------------------");
				System.out.println("不可满足的概念的ID为："+classId+":"+AML.getInstance().getURIMap().getLocalName(classId));
				//可以利用 AML.getInstance().getURIMap().contains(classId)来进行区别类
				System.out.println("不相交的结点1："+i+":"+AML.getInstance().getURIMap().getLocalName(i));
				System.out.println("不相交的结点2："+j+":"+AML.getInstance().getURIMap().getLocalName(j));
				System.out.println("-------------------------------------------------------------");			
				ArrayList<Integer> Path1 =new ArrayList<Integer>();
				ArrayList<Integer> Path2 =new ArrayList<Integer>();			
				
				List<ArrayList<Integer>> Paths2=new ArrayList<ArrayList<Integer>>();											
				Paths2=graph.getPaths(classId, j);				
				Paths2=FilterPaths(Paths2);
				
				//one unsatisfied mapping may cause more than one mipp
				for(ArrayList<Integer> p1:Paths1)
				{					
					for(ArrayList<Integer> p2:Paths2)
					{
						//因为是指针，当Path1的取值发生改变时，会对后面的操作有所影响
						ArrayList<Integer> temp1=new ArrayList<Integer>();
						temp1.addAll(p1);
						//因为是指针，当Path2的取值发生改变时，会对后面的操作有所影响
						ArrayList<Integer> temp2=new ArrayList<Integer>();
						temp2.addAll(p2);
						ArrayList<Integer> common=RefinePath(temp1,temp2); 																		
						Pair<ArrayList<Integer>,ArrayList<Integer>> mipp=new Pair<ArrayList<Integer>, ArrayList<Integer>>(temp1,temp2);					
						if(!MinimalConflictSet.contains(mipp))	
						{
							MinimalConflictSet.add(mipp);	
							//根据路径的索引方式，我们可以判断从classId到精炼后Path的起点之间的点都已经完全检索过了(注意路径的约束)。
							for(Integer concept: common)
							{
								if(checkList.contains(concept))
									checkedState.put(concept+"-"+i+"-"+j, true);
							}
							int realUnsatisfied=temp1.get(0);
							if(checkList.contains(realUnsatisfied))
								checkedState.put(realUnsatisfied+"-"+i+"-"+j, true);
						}
						//MinimalConflictSetMap.put(key, mipp);
					}
				}
				System.out.println("过滤后的路径为:"); //其实是包括所有的父亲结点
				System.out.println("**********************");
				printResult(Paths1);
				System.out.println("**********************");
				printResult(Paths2);
				System.out.println("**********************");
				//System.out.println(checkedState.size());							
			}
		}	
	}*/
	
	private void buildClassConflictPaths3(int classId)
	{	
		//First get all ancestors involved in disjoint clauses	
		HashSet<Integer> dis = new HashSet<Integer>();
		
		if(ancestors.keySet().contains(classId) )  //存在祖先
		{
			for (Integer i : ancestors.get(classId))
				if (rels.hasDisjoint(i))
					dis.add(i);
		}
		
		//Refine the disjoint set
		HashSet<Integer> disi = new HashSet<>(dis);
		disi=RefineDisjointSet(disi);
		//Plus the class itself, if it has a disjoint clause
		if(rels.hasDisjoint(classId))
			disi.add(classId);	
		//存储部分临时的结果,避免重复计算
		HashMap<String, List<ArrayList<Integer>>> tempMap=new HashMap<String, List<ArrayList<Integer>>> ();
		for(Integer i : disi)
		{
			List<ArrayList<Integer>> Paths1=new ArrayList<ArrayList<Integer>>();
			if(tempMap.keySet().contains(classId+"-"+i))
				Paths1=tempMap.get(classId+"-"+i);
			else
				Paths1=graph.getPaths(classId, i);
			Paths1=FilterPaths(Paths1);			
			HashSet<Integer> disj = (HashSet<Integer>) rels.getDisjoint(i);
			disj=RefineDisjointSet(disj);			
			for(Integer j : disj)
			{
				if(!dis.contains(j) )  //i>j不是很懂为什么要这样设计，可能是对称性的关系，只需要考虑一半即可
					continue;
				/*if(i > j || !dis.contains(j))  //i>j不是很懂为什么要这样设计，可能是对称性的关系，只需要考虑一半即可
					continue;*/
				if(checkedState.keySet().contains(classId+"-"+i+"-"+j))
						continue;
				if(checkedState.keySet().contains(classId+"-"+j+"-"+i))
						continue;	
				
				System.out.println("-------------------------------------------------------------");
				System.out.println("The ID of the unsatisfiable concept is:"+classId+":"+AML.getInstance().getURIMap().getLocalName(classId));
				//可以利用 AML.getInstance().getURIMap().contains(classId)来进行区别类
				System.out.println("Disjoint Nodes 1："+i+":"+AML.getInstance().getURIMap().getLocalName(i));
				System.out.println("Disjoint Nodes 2："+j+":"+AML.getInstance().getURIMap().getLocalName(j));
				System.out.println("-------------------------------------------------------------");			
				/*ArrayList<Integer> Path1 =new ArrayList<Integer>();
				ArrayList<Integer> Path2 =new ArrayList<Integer>();*/			
				
				List<ArrayList<Integer>> Paths2=new ArrayList<ArrayList<Integer>>();
				
				if(tempMap.keySet().contains(classId+"-"+j))
					Paths2=tempMap.get(classId+"-"+j);
				else
					Paths2=graph.getPaths(classId, j);				
				Paths2=FilterPaths(Paths2);
				
				//one unsatisfied mapping may cause more than one mipp
				for(ArrayList<Integer> p1:Paths1)
				{					
					for(ArrayList<Integer> p2:Paths2)
					{
						//因为是指针，当Path1的取值发生改变时，会对后面的操作有所影响
						ArrayList<Integer> temp1=new ArrayList<Integer>();
						temp1.addAll(p1);
						//因为是指针，当Path2的取值发生改变时，会对后面的操作有所影响
						ArrayList<Integer> temp2=new ArrayList<Integer>();
						temp2.addAll(p2);
						ArrayList<Integer> common=RefinePath(temp1,temp2); 																		
						Pair<ArrayList<Integer>,ArrayList<Integer>> mipp=new Pair<ArrayList<Integer>, ArrayList<Integer>>(temp1,temp2);					
						if(!MinimalConflictSet.contains(mipp))	
						{
							MinimalConflictSet.add(mipp);	
							//根据路径的索引方式，我们可以判断从classId到精炼后Path的起点之间的点都已经完全检索过了(注意路径的约束)。
							for(Integer concept: common)
							{
								if(checkList.contains(concept))
									checkedState.put(concept+"-"+i+"-"+j, true);
							}
							int realUnsatisfied=temp1.get(0);
							if(checkList.contains(realUnsatisfied))
							{
								checkedState.put(realUnsatisfied+"-"+i+"-"+j, true);					
							}						
						}				
					}
								
				}
				if(!Paths1.isEmpty())
					tempMap.put(classId+"-"+i, Paths1);	
				if(!Paths2.isEmpty())
					tempMap.put(classId+"-"+j, Paths2);	
				
				/*System.out.println("过滤后的路径为:"); //其实是包括所有的父亲结点
				System.out.println("**********************");
				printResult(Paths1);
				System.out.println("**********************");
				printResult(Paths2);
				System.out.println("**********************");*/
				//System.out.println(checkedState.size());							
			}
		}	
	}
	
	
	private void buildClassConflictPaths2(int classId)
	{	
		//First get all ancestors involved in disjoint clauses	
		HashSet<Integer> dis = new HashSet<Integer>();
		
		if(ancestors.keySet().contains(classId) )  //存在祖先
		{
			for (Integer i : ancestors.get(classId))
				if (rels.hasDisjoint(i))
					dis.add(i);
		}
		
		//Refine the disjoint set
		HashSet<Integer> disi = new HashSet<>(dis);
		disi=RefineDisjointSet(disi);
		//Plus the class itself, if it has a disjoint clause
		if(rels.hasDisjoint(classId))
			disi.add(classId);	
		for(Integer i : disi)
		{
			List<ArrayList<Integer>> Paths1=new ArrayList<ArrayList<Integer>>();
			Paths1=graph.getPaths(classId, i);
			Paths1=FilterPaths(Paths1);			
			HashSet<Integer> disj = (HashSet<Integer>) rels.getDisjoint(i);
			//disj=RefineDisjointSet(disj);			
			for(Integer j : disj)
			{
				if(!dis.contains(j) )  //i>j不是很懂为什么要这样设计，可能是对称性的关系，只需要考虑一半即可
					continue;
				/*if(i > j || !dis.contains(j))  //i>j不是很懂为什么要这样设计，可能是对称性的关系，只需要考虑一半即可
					continue;*/
				if(checkedState.keySet().contains(classId+"-"+i+"-"+j))
						continue;
				if(checkedState.keySet().contains(classId+"-"+j+"-"+i))
						continue;	
				
				System.out.println("-------------------------------------------------------------");
				System.out.println("不可满足的概念的ID为："+classId+":"+AML.getInstance().getURIMap().getLocalName(classId));
				//可以利用 AML.getInstance().getURIMap().contains(classId)来进行区别类
				System.out.println("不相交的结点1："+i+":"+AML.getInstance().getURIMap().getLocalName(i));
				System.out.println("不相交的结点2："+j+":"+AML.getInstance().getURIMap().getLocalName(j));
				System.out.println("-------------------------------------------------------------");			
				/*ArrayList<Integer> Path1 =new ArrayList<Integer>();
				ArrayList<Integer> Path2 =new ArrayList<Integer>();*/			
				
				List<ArrayList<Integer>> Paths2=new ArrayList<ArrayList<Integer>>();											
				Paths2=graph.getPaths(classId, j);				
				Paths2=FilterPaths(Paths2);
				
				//one unsatisfied mapping may cause more than one mipp
				for(ArrayList<Integer> p1:Paths1)
				{					
					for(ArrayList<Integer> p2:Paths2)
					{
						//因为是指针，当Path1的取值发生改变时，会对后面的操作有所影响
						ArrayList<Integer> temp1=new ArrayList<Integer>();
						temp1.addAll(p1);
						//因为是指针，当Path2的取值发生改变时，会对后面的操作有所影响
						ArrayList<Integer> temp2=new ArrayList<Integer>();
						temp2.addAll(p2);
						ArrayList<Integer> common=RefinePath(temp1,temp2); 																		
						Pair<ArrayList<Integer>,ArrayList<Integer>> mipp=new Pair<ArrayList<Integer>, ArrayList<Integer>>(temp1,temp2);					
						if(!MinimalConflictSet.contains(mipp))	
						{
							MinimalConflictSet.add(mipp);	
							//根据路径的索引方式，我们可以判断从classId到精炼后Path的起点之间的点都已经完全检索过了(注意路径的约束)。
							for(Integer concept: common)
							{
								if(checkList.contains(concept))
									checkedState.put(concept+"-"+i+"-"+j, true);
							}
							int realUnsatisfied=temp1.get(0);
							if(checkList.contains(realUnsatisfied))
								checkedState.put(realUnsatisfied+"-"+i+"-"+j, true);	
	
						}
						//MinimalConflictSetMap.put(key, mipp);
					}
				}
				/*System.out.println("过滤后的路径为:"); //其实是包括所有的父亲结点
				System.out.println("**********************");
				printResult(Paths1);
				System.out.println("**********************");
				printResult(Paths2);
				System.out.println("**********************");*/
				//System.out.println(checkedState.size());							
			}
		}	
	}
	
	//Adds a path to a list of conflict sets if it is a minimal path
	//(this only results in a minimal list of paths if paths are added
	//in order, after sorting)
	private void addConflict(Path p, Vector<Path> paths)
	{
		for(Path q : paths)
			if(p.contains(q))
				return;
		paths.add(p);
	}
	
	
	public List<ArrayList<Integer>> FilterPaths(List<ArrayList<Integer>> paths)
	{				
		//建立相应冲突个数的映射关系
		if(paths.isEmpty())
			return paths;
		int min=Integer.MAX_VALUE;
		List<ArrayList<Integer>> newPaths=new ArrayList<ArrayList<Integer>>();
		HashMap<Integer, List<ArrayList<Integer>>> pathMap=new HashMap<Integer, List<ArrayList<Integer>>>();
		HashMap<Integer, Vector<Path>> conflictMap=new HashMap<Integer, Vector<Path>>();
		for(ArrayList<Integer> pa:paths)
		{
			//Vector<Path> conflict =new Vector<Path>();
			Path conflict=new Path();
			for(int i=0;i<pa.size()-1;i++)
			{
				int index=-1;
				int node1=pa.get(i);
				int node2=pa.get(i+1);		
				index=a.getIndexBidirectional(node1, node2);
				if(index!=-1)
				{
					//conflict.add(new Path(index));	
					conflict.add(index);	
				}
			}
			if(conflictMap.containsKey(conflict.size()))
			{
				Vector<Path> set=conflictMap.get(conflict.size());
				set.add(conflict);
				conflictMap.put(conflict.size(), set);
				
				List<ArrayList<Integer>> tempPath=pathMap.get(conflict.size());
				tempPath.add(pa);
				pathMap.put(conflict.size(), tempPath);			
			}
			else
			{
				Vector<Path> set=new Vector<Path>();
				List<ArrayList<Integer>> tempPath=new ArrayList<ArrayList<Integer>>();
				set.add(conflict);
				conflictMap.put(conflict.size(), set);
				tempPath.add(pa);
				pathMap.put(conflict.size(), tempPath);
			}
			if(min>conflict.size())
				min=conflict.size();			
		}			
		newPaths=pathMap.get(min);
		return newPaths;
	}
	//Refine the minimal conflict paths
	public HashSet<Integer> RefineDisjointSet(HashSet<Integer> dis)
	{
		/*Iterator<Integer> iter= dis.iterator();
		while(iter.hasNext())
		{
			Integer  r=iter.next();
			Set<Integer> ancestors=new HashSet<>(rels.getSuperClasses(r, false));
			ancestors.retainAll(dis); //看祖先中是否有dis的结点
			for(Integer node:ancestors)
			{
				if(!a.containsClass(node))
					iter.remove();
			}
		}	
		return dis;	*/	
		
		HashSet<Integer> disjointness= new HashSet<>(dis);
		for(Integer r:disjointness)
		{
			Set<Integer> ancestors=new HashSet<>(rels.getSuperClasses(r, false));
			ancestors.retainAll(dis); //看祖先中是否有dis的结点
			for(Integer node:ancestors)
			{
				if(!a.containsClass(node))
					dis.remove(node);
			}
		}
		return dis;
		
	}
	
	//Refine the minimal conflict paths
	public ArrayList<Integer> RefinePath(ArrayList<Integer> Path1,ArrayList<Integer> Path2)
	{	
		ArrayList<Integer> commonNums=new ArrayList<Integer>();
		if(Path1.size()==1||Path2.size()==1)  //长度为1,无须精简
			return commonNums;	
		Iterator<Integer> iterator1 = Path1.iterator();
		Iterator<Integer> iterator2 = Path2.iterator();
		
		while(iterator1.hasNext()&&iterator2.hasNext())
		{
			int num1=iterator1.next();
			int num2=iterator2.next();
			if(num1==num2)
			{
				commonNums.add(num1);
			}	
			else if(!commonNums.isEmpty()) //隐含了不相等的情况
			{
				commonNums.remove(commonNums.size()-1);
				break;
			}
		}
		//
		if(commonNums.size()==Path1.size()||commonNums.size()==Path1.size()) //两条路径有重叠，保留最后一个元素
		{
			commonNums.remove(commonNums.size()-1);
		}
		if(!commonNums.isEmpty())
		{
			Path1.removeAll(commonNums);
			Path2.removeAll(commonNums);
		}	
		return commonNums;
		
	}
	
	public KG generateKG(ArrayList<Integer> Path1,ArrayList<Integer> Path2)
	{	
		KG kg=new KG();

		Set<Integer> mapping=new HashSet<Integer>();
		List<Integer> sourceConcept=new ArrayList<Integer>() ;
		List<Integer> targeConcept=new ArrayList<Integer>() ;
		ArrayList<String> sourceRel=new ArrayList<String>();
		ArrayList<String> targetRel=new ArrayList<String>();
	
		Set<Integer> source=aml.getSource().getClasses();		
	
		int unsatisfiedConcept=Path1.get(0);
		if(source.contains(unsatisfiedConcept))
		{
			sourceConcept.add(unsatisfiedConcept);
		}
		else
		{
			targeConcept.add(unsatisfiedConcept);
		}
		
		for(int i=0;i<Path1.size()-1;i++)
		{
			int index=-1;
			int node1=Path1.get(i);
			int node2=Path1.get(i+1);		
			index=a.getIndexBidirectional(node1, node2);
			if(index!=-1)
			{	
				mapping.add(index);
			}
			if(source.contains(node2))
			{
				sourceConcept.add(node2);
			}
			else
			{
				targeConcept.add(node2);		
			}
		}
		
		for(int i=0;i<Path2.size()-1;i++)
		{
			int index=-1;
			int node1=Path2.get(i);
			int node2=Path2.get(i+1);		
			index=a.getIndexBidirectional(node1, node2);
			if(index!=-1)
			{	
				mapping.add(index);
			}
			if(source.contains(node2))
			{
				sourceConcept.add(node2);
			}
			else
			{
				targeConcept.add(node2);		
			}
		}
		
		for(int con:sourceConcept)
		{
			for(int parent:parentMap.get(con))
			{
				if(sourceConcept.contains(parent))
					sourceRel.add(con+"--"+parent);
			}
		}
		
		for(int con:targeConcept)
		{
			for(int parent:parentMap.get(con))
			{
				if(targeConcept.contains(parent))
					targetRel.add(con+"--"+parent);
			}
		}
		
		kg.init(sourceConcept,targeConcept,mapping,sourceRel,targetRel);
		return kg;
	}
	
	
	public Set<Integer> getMappings(ArrayList<Integer> Path1,ArrayList<Integer> Path2)
	{
		Set<Integer> mapping=new HashSet<Integer>();
		for(int i=0;i<Path1.size()-1;i++)
		{
			int index=-1;
			int node1=Path1.get(i);
			int node2=Path1.get(i+1);		
			index=a.getIndexBidirectional(node1, node2);
			if(index!=-1)
			{	
				mapping.add(index);
			}
		}
		
		for(int i=0;i<Path2.size()-1;i++)
		{
			int index=-1;
			int node1=Path2.get(i);
			int node2=Path2.get(i+1);		
			index=a.getIndexBidirectional(node1, node2);
			if(index!=-1)
			{	
				mapping.add(index);
			}
		}
		return mapping;
	}

	public void printPath(ArrayList<Integer> Path)
	{
		StringBuilder sb = new StringBuilder();
		for (Integer i : Path) {
			sb.append(i + "->");
		}
		sb.append("#");
		System.out.println(sb.toString().replace("->#", ""));		
	}
	
	public void printResult(List<ArrayList<Integer>> paths)
	{
		for(ArrayList<Integer> path:paths)
		{
			StringBuilder sb = new StringBuilder();
			for (Integer i : path) {
				sb.append(i + "->");
			}
			sb.append("#");
			System.out.println(sb.toString().replace("->#", ""));
		}
	}
}