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
* Test-runs AgreementMakerLight in Eclipse.                                   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml;

public class RepairingExample
{

//Main Method
	
	public static void main(String[] args) throws Exception
	{
		long time = System.currentTimeMillis()/1000;
		
		//Anatomy
		/*String sourcePath  = "testdata/ontologies/mouse.owl";
		String targetPath  = "testdata/ontologies/human.owl";
		String alignPath = "testdata/mappings/AML_M-mouse-human.rdf";
		String outputPath="Results/PDLMV-AML_M-mouse-human-repaired.rdf";*/
		
		/*String sourcePath  = "testdata/ontologies/mouse.owl";
		String targetPath  = "testdata/ontologies/human.owl";
		String alignPath = "testdata/mappings/LogMapLite-mouse-human-C.rdf";
		String outputPath="Results/PDLMV-LogMapLite-mouse-human-repaired.rdf";*/
			
		/*String sourcePath  = "testdata/ontologies/mouse.owl";
		String targetPath  = "testdata/ontologies/human.owl";
		String alignPath = "testdata/mappings/FCA_Map-mouse-human.rdf";
		String outputPath="Results/PDLMV-FCA_Map-mouse-human-repaired.rdf";*/
		
		//Large-Biology ontology
		//AML_M
		/*String sourcePath  = "testdata/ontologies/oaei_FMA_small_overlapping_nci.owl";
		String targetPath  = "testdata/ontologies/oaei_NCI_small_overlapping_fma.owl";
		String alignPath = "testdata/mappings/AML_M-largebio-fma_nci_small.rdf";
		String outputPath="Results/PDLMV-AML_M-FMA-NCI-small-repaired.rdf";*/
		
		/*String sourcePath  = "testdata/ontologies/oaei_FMA_small_overlapping_snomed.owl";
		String targetPath  = "testdata/ontologies/oaei_SNOMED_small_overlapping_fma.owl";
		String alignPath = "testdata/mappings/AML_M-FMA-SNOMED-small.rdf";
		String outputPath="Results/PDLMV-AML_M-FMA-SNOMED-small-repaired.rdf";*/
		
		/*String sourcePath  = "testdata/ontologies/oaei_FMA_whole_ontology.owl";
		String targetPath  = "testdata/ontologies/oaei_NCI_whole_ontology.owl";
		String alignPath = "testdata/mappings/AML_M-FMA-NCI-whole.rdf";
		String outputPath="Results/PDLMV-AML_M-FMA-NCI-whole-repaired.rdf";*/

		/*String sourcePath  = "testdata/ontologies/oaei_FMA_whole_ontology.owl";
		String targetPath  = "testdata/ontologies/oaei_SNOMED_extended_overlapping_fma_nci.owl";
		String alignPath = "testdata/mappings/AML_M-FMA-SNOMED-whole.rdf";
		String outputPath="Results/PDLMV-AML_M-FMA-SNOMED-whole-repaired.rdf";*/
			
		//专业测试本体(LogMapLite)
		/*String sourcePath  = "testdata/ontologies/oaei_FMA_small_overlapping_nci.owl";
		String targetPath  = "testdata/ontologies/oaei_NCI_small_overlapping_fma.owl";
		String alignPath = "testdata/mappings/LogMapLite-largebio-fma_nci_small_2016-0.9.rdf";
		String outputPath="Results/PDLMV-LogMapLite-FMA-NCI-small-repaired-0.9.rdf";*/
			
		/*String sourcePath  = "testdata/ontologies/oaei_FMA_small_overlapping_snomed.owl";
		String targetPath  = "testdata/ontologies/oaei_SNOMED_small_overlapping_fma.owl";
		String alignPath = "testdata/mappings/LogMapLite-largebio-fma_snomed_small_2016-0.9.rdf";
		String outputPath="Results/PDLMV-LogMapLite-FMA-SNOMED-small-repaired-0.9.rdf";*/
		
		
		/*String sourcePath  = "testdata/ontologies/oaei_FMA_whole_ontology.owl";
		String targetPath  = "testdata/ontologies/oaei_NCI_whole_ontology.owl";
		String alignPath = "testdata/mappings/alignments/LogMapLite-largebio-fma_nci_whole_2016-0.9.rdf";
		String outputPath="Results/PDLMV-LogMapLite-FMA-NCI-whole-repaired-0.9.rdf";*/
		
		/*String sourcePath  = "testdata/ontologies/oaei_FMA_whole_ontology.owl";
		String targetPath  = "testdata/ontologies/oaei_SNOMED_extended_overlapping_fma_nci.owl";
		String alignPath = "testdata/mappings/alignments/LogMapLite-largebio-fma_snomed_whole_2016-0.9.rdf";
		String outputPath="Results/PDLMV-LogMapLite-FMA-SNOMED-whole-repaired.rdf";*/
		
		
		//专业测试本体(FCA_Map)
		String sourcePath  = "testdata/ontologies/oaei_FMA_small_overlapping_nci.owl";
		String targetPath  = "testdata/ontologies/oaei_NCI_small_overlapping_fma.owl";
		String alignPath = "testdata/mappings/FCA_Map-largebio-fma_nci_small_2016-0.9.rdf";
		String outputPath="Results/PDLMV-FCA_Map-FMA-NCI-small-repaired-0.9.rdf";
		
		/*String sourcePath  = "testdata/ontologies/oaei_FMA_small_overlapping_snomed.owl";
		String targetPath  = "testdata/ontologies/oaei_SNOMED_small_overlapping_fma.owl";
		String alignPath = "testdata/mappings/alignments/FCA_Map-largebio-fma_snomed_small_2016-0.9.rdf";
		String outputPath="Results/PDLMV-FCA_Map-FMA-SNOMED-small-repaired-0.9.rdf";*/
			
		AML aml = AML.getInstance();
		aml.openOntologies(sourcePath, targetPath);
		//aml.matchAuto();  //ontology matching process
		if(!alignPath.equals(""))
		{
			aml.openAlignment(alignPath);
			aml.repairRevise();  //Repair based on PDL semantics	
		}

		
		System.out.println("The whole repair by our method is " + (System.currentTimeMillis()/1000-time) + " seconds");
		if(!outputPath.equals(""))
			aml.saveAlignmentRDF(outputPath);
	}
}