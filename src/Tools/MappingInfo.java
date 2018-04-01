package Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;



public class MappingInfo {
	private ArrayList<String> mappings; 
	private ArrayList<ArrayList<String>> MinimcalConflictSet; 
	OntModel ontology = ModelFactory.createOntologyModel();
	
	public MappingInfo(String MappingPath) throws IOException 
	{
		if(MappingPath.contains(".rdf"))
		{
			mappings=getReference(MappingPath);
		}
		else
		{
			BufferedReader Alignment = new BufferedReader(new FileReader(new File(MappingPath)));
			mappings = new ArrayList<String>();
			String lineTxt = null;
			while ((lineTxt = Alignment.readLine()) != null) {
				String line = lineTxt.trim(); // 閸樼粯甯�鐎涙顑佹稉鏌ヮ浕娴ｅ秶娈戠粚鐑樼壐閿涘矂浼╅崗宥呭従缁岀儤鐗搁柅鐘冲灇閻ㄥ嫰鏁婄拠锟�
				// line=line.toLowerCase();//閸忋劑鍎撮崣妯诲灇鐏忓繐鍟�
				mappings.add(line);
			}
		}
		if(mappings.size()==0)
		{
			mappings=getReference2(MappingPath);
		}
	}
	

	
	public ArrayList<String> getMappings()
	{
		return mappings;
	}
	
	public ArrayList<String> getReference(String alignmentFile)
	{
		  ArrayList<String> references=new ArrayList<String>();  
		    Model model = ModelFactory.createDefaultModel();
			InputStream in = FileManager.get().open( alignmentFile );
	        if (in == null) {
	        	System.out.println("File: " + alignmentFile + " not found!");
	            throw new IllegalArgumentException( "File: " + alignmentFile + " not found");
	        }
	        model.read(in,"");
			//model.read(in,null);
	        //鐟欙絾鐎介弬鐟扮础1(闁藉牆顕甕AM++)
			/*Property alignmententity1, alignmententity2;
			alignmententity1 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity1");
			alignmententity2 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity2");
			Property value = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#measure");
			Property relation = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#relation");*/
			//OntClass temp;
	        //鐟欙綁鍣撮弬鐟扮础2(闁藉牆顕瓵ML++,reference alignments)
		    Property alignmententity1 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmententity1");
			Property alignmententity2 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmententity2");//alignment
			Property value = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmentmeasure");
			Property relation = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmentrelation");

	        ResIterator resstmt = model.listSubjectsWithProperty(alignmententity1);	//娑撱倛锟藉懏鏌熷▔鏇熸Ц娑擄拷閺嶉娈� 
		    Resource temp;
			while(resstmt.hasNext()){
				temp = resstmt.next();//temp閺勵垯绗侀崗鍐矋
				String entity1 = temp.getPropertyResourceValue(alignmententity1).getLocalName();//閼惧嘲褰囬張顑跨秼1閻ㄥ嫭婀版担锟�
				String entity2 = temp.getPropertyResourceValue(alignmententity2).getLocalName();//閼惧嘲褰囬張顑跨秼2閻ㄥ嫭婀版担锟�
				String Confidence="1";
				if(temp.getProperty(value)!=null)
				 Confidence=temp.getProperty(value).getObject().asLiteral().getString();//閼惧嘲褰囨穱鈥冲悍閸婏拷
				String Relation="=";
				if(temp.getProperty(relation)!=null)
				  Relation=temp.getProperty(relation).getObject().toString();//閼惧嘲褰囬崠褰掑帳閻ㄥ嫬鍙х化锟�
				
				//濮ｆ棁绶濈粭銊ф畱閺傝纭�
				/*String Confidence=temp.getProperty(value).getLiteral().toString();
				Confidence=Confidence.replace("^^xsd:float", "").replace("^^http://www.w3.org/2001/XMLSchema#float", "");
				System.out.println(Confidence);*/
				
				
			//	System.out.println(Relation);
			//	System.out.println(entity1+" "+entity2);
			//	System.out.println(entity1+" "+entity2+" "+Relation+" "+Confidence);
				
				//鏉堟挸鍤幍锟介張澶屾畱娑撳鍘撶紒锟�
		/*		StmtIterator stmt = model.listStatements();
				while(stmt.hasNext()){
					System.out.println(stmt.next());
				}*/
				//entity1=entity1.replace("-", "_");//娑撹桨绨￠悽璇叉禈閺傞�涚┒閿涘苯鐨�'-'閺囨寧宕查幋锟�'_'		
				//entity2=entity2.replace("-", "_");//娑撹桨绨￠悽璇叉禈閺傞�涚┒閿涘苯鐨�'-'閺囨寧宕查幋锟�'_'	
				BigDecimal   b   =   new   BigDecimal(Double.parseDouble(Confidence));  //閸ユ稖鍨楁禍鏂垮弳閻ㄥ嫭鏌熷锟�
				Double confidence =   b.setScale(2,  BigDecimal.ROUND_HALF_UP).doubleValue();  
				
			/*	if(Relation.equals("?"))
					continue;*/
			/*	if(!references.contains(entity1+","+entity2+","+Relation+","+confidence))
					references.add(entity1+","+entity2+","+Relation+","+confidence);//缂佺喍绔存潪顒�瀵查幋鎰毈閸愶拷
*/				
				if(Relation.equals("&gt;"))
					continue;
				references.add(entity1+","+entity2+","+Relation);//缂佺喍绔存潪顒�瀵查幋鎰毈閸愶拷
				//references.add(entity1+" = "+entity2);//缂佺喍绔存潪顒�瀵查幋鎰毈閸愶拷
			}
			return references;
	}
	
	public ArrayList<String> getReference2(String alignmentFile)
	{
		  ArrayList<String> references=new ArrayList<String>();  
		    Model model = ModelFactory.createDefaultModel();
			InputStream in = FileManager.get().open( alignmentFile );
	        if (in == null) {
	        	System.out.println("File: " + alignmentFile + " not found!");
	            throw new IllegalArgumentException( "File: " + alignmentFile + " not found");
	        }
	        model.read(in,"");
			//model.read(in,null);
	        //鐟欙絾鐎介弬鐟扮础1(闁藉牆顕甕AM++)
			Property alignmententity1, alignmententity2;
			alignmententity1 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity1");
			alignmententity2 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity2");
			Property value = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#measure");
			Property relation = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#relation");
			//OntClass temp;
	        //鐟欙綁鍣撮弬鐟扮础2(闁藉牆顕瓵ML++,reference alignments)
		   /* Property alignmententity1 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmententity1");
			Property alignmententity2 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmententity2");//alignment
			Property value = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmentmeasure");
			Property relation = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmentrelation");*/

	        ResIterator resstmt = model.listSubjectsWithProperty(alignmententity1);	//娑撱倛锟藉懏鏌熷▔鏇熸Ц娑擄拷閺嶉娈� 
		    Resource temp;
			while(resstmt.hasNext()){
				temp = resstmt.next();//temp閺勵垯绗侀崗鍐矋
				String entity1 = temp.getPropertyResourceValue(alignmententity1).getLocalName();//閼惧嘲褰囬張顑跨秼1閻ㄥ嫭婀版担锟�
				String entity2 = temp.getPropertyResourceValue(alignmententity2).getLocalName();//閼惧嘲褰囬張顑跨秼2閻ㄥ嫭婀版担锟�
				String Confidence="1";
				if(temp.getProperty(value)!=null)
					 Confidence=temp.getProperty(value).getObject().asLiteral().getString();//閼惧嘲褰囨穱鈥冲悍閸婏拷
					String Relation="=";
				if(temp.getProperty(relation)!=null)
					  Relation=temp.getProperty(relation).getObject().toString();//閼惧嘲褰囬崠褰掑帳閻ㄥ嫬鍙х化锟�
				
				//濮ｆ棁绶濈粭銊ф畱閺傝纭�
				/*String Confidence=temp.getProperty(value).getLiteral().toString();
				Confidence=Confidence.replace("^^xsd:float", "").replace("^^http://www.w3.org/2001/XMLSchema#float", "");
				System.out.println(Confidence);*/
				
				
			//	System.out.println(Relation);
			//	System.out.println(entity1+" "+entity2);
			//	System.out.println(entity1+" "+entity2+" "+Relation+" "+Confidence);
				
				//鏉堟挸鍤幍锟介張澶屾畱娑撳鍘撶紒锟�
		/*		StmtIterator stmt = model.listStatements();
				while(stmt.hasNext()){
					System.out.println(stmt.next());
				}*/
				//entity1=entity1.replace("-", "_");//娑撹桨绨￠悽璇叉禈閺傞�涚┒閿涘苯鐨�'-'閺囨寧宕查幋锟�'_'		
				//entity2=entity2.replace("-", "_");//娑撹桨绨￠悽璇叉禈閺傞�涚┒閿涘苯鐨�'-'閺囨寧宕查幋锟�'_'	
				BigDecimal   b   =   new   BigDecimal(Double.parseDouble(Confidence));  //閸ユ稖鍨楁禍鏂垮弳閻ㄥ嫭鏌熷锟�
				Double confidence =   b.setScale(2,  BigDecimal.ROUND_HALF_UP).doubleValue();  
				
			/*	if(Relation.equals("?"))
					continue;*/
				/*if(!references.contains(entity1+","+entity2+","+Relation+","+confidence))
					references.add(entity1+","+entity2+","+Relation+","+confidence);*/
				if(Relation.equals("&gt;"))
					continue;
					references.add(entity1+","+entity2+","+Relation);
				//references.add(entity1+" = "+entity2);//缂佺喍绔存潪顒�瀵查幋鎰毈閸愶拷
			}
			return references;
	}
}
