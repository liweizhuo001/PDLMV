package statistic;


import java.io.IOException;
import java.util.ArrayList;

import Tools.EvaluationForRevised;
import Tools.EvaluationLargeBio;
import Tools.MappingInfo;
import Tools.MappingInfoForLogMapLite;



public class standardStatisticResultcmd {
	public static void main(String args[]) throws IOException
	{			
		String orginalAlignment  = "testdata/"+args[0];
		String repairedAlignment  = "Results/"+args[1];
		String referencePath = "testdata/"+args[2];

		
		/*String orginalAlignment = "testdata/mappings/FCA_Map-largebio-fma_nci_small_2016-0.9.rdf";
		String repairedAlignment = "Results/PDLMV-FCA_Map-FMA-NCI-small-repaired-0.9.rdf";
		String referencePath = "testdata/referenceAlignment/oaei_FMA2NCI_UMLS_mappings_with_flagged_repairs.rdf";*/
			
				
		MappingInfo MappingInformation=new MappingInfo(repairedAlignment);	
		ArrayList<String> mappings= new ArrayList<String>();
		mappings=MappingInformation.getMappings();
		System.out.println(mappings.size());
		
		//MappingInfoForLogMapLite MappingInformation2=new MappingInfoForLogMapLite(orginalAlignment);	
		MappingInfo MappingInformation2=new MappingInfo(orginalAlignment);	
		ArrayList<String> mappings2= new ArrayList<String>();
		mappings2=MappingInformation2.getMappings();
		System.out.println(mappings2.size());
		
		ArrayList<String> referenceMappings= new ArrayList<String>();
		MappingInfo ReferenceInformation=new MappingInfo(referencePath);
		referenceMappings=ReferenceInformation.getMappings();
		
		System.out.println(referenceMappings.size());
		EvaluationForRevised cBefore = new EvaluationForRevised(mappings, referenceMappings);
		
		System.out.println("--------------------------------------------------------");
		System.out.println("before debugging (pre, rec, f): " + cBefore.toShortDesc());
		System.out.println("The number of total correct mappings in alignment:  " + cBefore.getCorrectAlignmentNum());
		System.out.println("The number of total incorrect mappings in alignment:  " + cBefore.getInCorrectAlignmentNum());		
		System.out.println("The number of total mappings in alignment:  " + cBefore.getMatcherAlignmentNum());  //ֻ��LogMap����ȷ��������֮�Ϳ��ܲ�Ϊ����
		
		System.out.println("--------------------------------------------------------");	
		//ע��mapping2��ԭʼ��alignment,ԭʼ�Ĺ�ϵ���ǵȺ�
		System.out.println("Removed mappings "+ (mappings2.size()-mappings.size()));
		mappings2.removeAll(mappings);
		//���ﻹ��Ҫ�Ƴ�һ��revise��������remove����ȷ����	
		System.out.println("The proportion of revised mappings are kept correctly: " + (cBefore.getRevisedCorrectNum())+"/"+cBefore.getRevisedAlignment().size());		
		
		mappings2.removeAll(cBefore.getRevisedAlignment());	
		EvaluationForRevised cBefore2 = new EvaluationForRevised(mappings2, referenceMappings);
		System.out.println("The number of mappings removed correctly in repaired mappings is " + (cBefore2.getInCorrectAlignmentNum())+"/"+mappings2.size());				
	}
	
}
