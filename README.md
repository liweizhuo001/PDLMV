PDLMV is a tool for mapping validation by probabilistic reasoning and belief revision. The framework of project is based on AML (AgreementMakerLight) Eclipse Project.

Time: 1/4/2018  Author: Weizhuo Li   Mail: liweizhuo2014@amss.ac.cn 

Software: Java 1.7 or higher.   
Hardware: 16GB RAM or more. The CPU is not limited, but we still hope that the CPU in your computer is as efficient as possible, which can reduce a lot of time consumption. 

Data sets: We just provide partial data sets and their reference alignments for your testing. The whole dataset and alignment should be downloaded in Ontology Alignment Evaluation Initiative (OAEI) http://oaei.ontologymatching.org/.

Usage:   
a)If you install Eclipse, you can import this project directly and run the aml.RepairingExample.java. There are lots of mappings and ontologies saved in testdata and you can use them to test PDLMV.

b)If you want to run the program by command line. you just need finish three steps:

1)use command line to enter the root directory of project. 

2)For repair, ensure that your tested ontologies and mapping have been listed in "testdata/ontologies" and "testdata/mappings". For evaluation, ensure that your tested repaired mapping, orginal mapping and reference alignment have been listed in "testdata/ontologies", "testdata/mappings", "testdata/referenceAlignment" respectively.

3.run command sentences are listed below:  
//Repair Example   
java -Xms10000m -Xmx16000m -classpath bin;src/lib/*;ExternalLib/*; aml.Repairingcmd ontologies/oaei_FMA_small_overlapping_nci.owl ontologies/oaei_NCI_small_overlapping_fma.owl mappings/AML_M-largebio-fma_nci_small.rdf test.rdf

//Evaluation Example(Standard)    
java -classpath bin;src/lib/*;ExternalLib/*; statistic.standardStatisticResultcmd mappings/FCA_Map-largebio-fma_nci_small_2016-0.9.rdf PDLMV-FCA_Map-FMA-NCI-small-repaired-0.9.rdf referenceAlignment/oaei_FMA2NCI_UMLS_mappings_with_flagged_repairs.rdf

//Evaluation Example(OAEI)
java -classpath bin;src/lib/*;ExternalLib/*; statistic.OAEIStatisticResultcmd mappings/FCA_Map-largebio-fma_nci_small_2016-0.9.rdf PDLMV-FCA_Map-FMA-NCI-small-repaired-0.9.rdf referenceAlignment/oaei_FMA2NCI_UMLS_mappings_with_flagged_repairs.rdf

In linux, these commands ";" need to change ":".

The key functions introduction:  
aml.Repairingcmd: call PDLMV to repair mappings.    
statistic.OAEIStatisticResultcmd: evaluation repaired mapping According to the standard precision, recall, F1.    
statistic.OAEIStatisticResultcmd: evaluation repaired mapping According to the OAEI precision, recall, F1.  (This evaluation criterion differs only in reference alignment of large biomedical ontologies)   

Instructions:    
For other repair tools, you can download the Alcomo, LogMap, AMLR as follows:  
Alcomo: http://web.informatik.uni-mannheim.de/alcomo/   runing file: ExampleXYZ.java in default package  
LogMap: https://github.com/ernestojimenezruiz/logmap-matcher  runing file: LogMap2_RepairFacility.java in uk.ac.ox.krr.logmap2 package  
AMLR: https://github.com/AgreementMakerLight/AML-Project  runing file: Test.java in aml package. Note that sentence "aml.matchAuto();" needs to be commented out, And add the following sentences.   
String alignPath = "XXXX"; //The path of the mapping will be repaired.   
if(!alignPath.equals(""))  {  
	aml.openAlignment(alignPath);  
	aml.repair();  //Repair based on PDL semantics    	
}  
    
More details for Reading:   
[1] Weizhuo Li, Songmao Zhang "Repairing mappings across biomedical ontologies by probabilistic reasoning and belief revision" available at: