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

public class Repairingcmd
{

//Main Method
	
	public static void main(String[] args) throws Exception
	{
		long time = System.currentTimeMillis()/1000;
						
		String sourcePath  = "testdata/"+args[0];
		String targetPath  = "testdata/"+args[1];
		String alignPath = "testdata/"+args[2];
		String outputPath="Results/"+args[3];	
				
		
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