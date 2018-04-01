package statistic;


import java.io.IOException;
import java.util.ArrayList;

import Tools.EvaluationForRevised;
import Tools.EvaluationLargeBio;
import Tools.MappingInfo;
import Tools.MappingInfoForLogMapLite;



public class standardStatisticResult {
	public static void main(String args[]) throws IOException
	{			
		//Anatomy
		/*String orginalAlignment = "testdata/mappings/AML_M-mouse-human.rdf";
		String repairedAlignment = "Results/PDLMV-AML_M-mouse-human-repaired.rdf";
		String referencePath = "testdata/referenceAlignment/reference_2015.rdf";*/
				
		/*String orginalAlignment = "testdata/mappings/LogMapLite-mouse-human-C.rdf";
		String repairedAlignment = "Results/PDLMV-LogMapLite-mouse-human-repaired.rdf";
		String referencePath = "testdata/referenceAlignment/reference_2015.rdf";*/
		
		/*String orginalAlignment = "testdata/mappings/FCA_Map-mouse-human.rdf";
		String repairedAlignment = "Results/PDLMV-FCA_Map-mouse-human-repaired.rdf";
		String referencePath = "testdata/referenceAlignment/reference_2015.rdf";*/	
		
		//LargeBio
		/*String orginalAlignment = "testdata/mappings/AML_M-largebio-fma_nci_small.rdf";
		String repairedAlignment = "Results/PDLMV-AML_M-FMA-NCI-small-repaired.rdf";
		String referencePath = "testdata/referenceAlignment/oaei_FMA2NCI_UMLS_mappings_with_flagged_repairs.rdf";*/
		
		/*String orginalAlignment = "testdata/mappings/LogMapLite-largebio-fma_nci_small_2016-0.9.rdf";
		String repairedAlignment = "Results/PDLMV-LogMapLite-FMA-NCI-small-repaired-0.9.rdf";
		String referencePath = "testdata/referenceAlignment/oaei_FMA2NCI_UMLS_mappings_with_flagged_repairs.rdf";*/
		
		String orginalAlignment = "testdata/mappings/FCA_Map-largebio-fma_nci_small_2016-0.9.rdf";
		String repairedAlignment = "Results/PDLMV-FCA_Map-FMA-NCI-small-repaired-0.9.rdf";
		String referencePath = "testdata/referenceAlignment/oaei_FMA2NCI_UMLS_mappings_with_flagged_repairs.rdf";
			
				
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
		System.out.println("The number of total mappings in alignment:  " + cBefore.getMatcherAlignmentNum());  //只有LogMap中正确的与错误的之和可能不为总数
		
		System.out.println("--------------------------------------------------------");	
		//注意mapping2是原始的alignment,原始的关系都是等号
		System.out.println("Removed mappings "+ (mappings2.size()-mappings.size()));
		mappings2.removeAll(mappings);
		//这里还需要移除一下revise，才能算remove的正确个数	
		System.out.println("The proportion of revised mappings are kept correctly: " + (cBefore.getRevisedCorrectNum())+"/"+cBefore.getRevisedAlignment().size());		
		
		mappings2.removeAll(cBefore.getRevisedAlignment());	
		EvaluationForRevised cBefore2 = new EvaluationForRevised(mappings2, referenceMappings);
		System.out.println("The number of mappings removed correctly in repaired mappings is " + (cBefore2.getInCorrectAlignmentNum())+"/"+mappings2.size());				
	}
	
}
