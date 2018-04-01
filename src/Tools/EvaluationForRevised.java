package Tools;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class EvaluationForRevised {
	int numOfRulesGold=0;
	int numOfRulesMatcher=0;
	int numOfRulesCorrect=0;
	int numOfRulesInCorrect=0;
	int numOfRevisedCorrect=0; //单独统计revise正确的个数
	Set <String> revised=new HashSet<String>();
	public EvaluationForRevised(ArrayList<String> mapping, ArrayList<String> reference) {
		// Mapping correct = reference.getIntersection(mapping); 
		Set <String> correct=new HashSet<String>();
		
		int subNumber=0;
		for (String r : reference) 
		{
			String rParts[]=r.split(",");
			for (String m : mapping) 
			{
				String mParts[]=m.split(",");
				if (rParts[0].equals(mParts[0])&& rParts[1].equals(mParts[1])&&mParts[2].equals("=")) 
				{
					correct.add(r);
				}
				else if(rParts[0].equals(mParts[1])&& rParts[1].equals(mParts[0])&&mParts[2].equals("="))			
				{
					correct.add(r);
				}
				//这里将revised 作为正确的情况考虑
				else if (rParts[0].equals(mParts[0])&& rParts[1].equals(mParts[1])&&mParts[2].equals("?")) 
				{
					correct.add(r);
					numOfRevisedCorrect++;
				}
				//这里将revised 作为正确的情况考虑
				else if(rParts[0].equals(mParts[1])&& rParts[1].equals(mParts[0])&&mParts[2].equals("?"))			
				{
					correct.add(r);
					numOfRevisedCorrect++;
				}				
				if(mParts[2].equals(">")||mParts[2].equals("<"))
					subNumber++;
			}
		}	
		//统计Revised的个数
		for (String m : mapping) 
		{
			String mParts[]=m.split(",");
			if(mParts[2].equals("?"))
				revised.add(mParts[0]+","+mParts[1]+","+"=");
				
		}
		
			
		this.numOfRulesGold = reference.size();		
		this.numOfRulesMatcher = mapping.size();
		this.numOfRulesCorrect = correct.size();
		this.numOfRulesInCorrect=mapping.size()-subNumber/reference.size()-correct.size();
		
	}	
	
	public String toShortDesc() {
		double precision = this.getPrecision();
		double recall = this.getRecall();
		double f = this.getFMeasure();

		return toDecimalFormat(precision) + "  " + toDecimalFormat(recall) + "  " + toDecimalFormat(f);
	}
	
	public double getPrecision() {
		//return (double)this.numOfRulesCorrect /  (double)this.numOfRulesMatcher;
		return (double)this.numOfRulesCorrect /  ((double)this.numOfRulesCorrect+(double)this.numOfRulesInCorrect);
	}
	
	public double getRecall() {
		return (double)this.numOfRulesCorrect /  (double)this.numOfRulesGold;
	}
	
	public double getFMeasure() {
		if ((this.getPrecision() == 0.0f) || (this.getRecall() == 0.0f)) { return 0.0f; }
		return (2 * this.getPrecision() * this.getRecall()) / (this.getPrecision() + this.getRecall());
	}
	
	public int getMatcherAlignmentNum() {
		return numOfRulesMatcher;
	}
	
	public int getCorrectAlignmentNum() {
		return numOfRulesCorrect;
	}
	
	public int getInCorrectAlignmentNum() {
		return numOfRulesInCorrect;
	}
	
	public int getRevisedAlignmentNum() {
		return revised.size();
	}
	
	public int getRevisedCorrectNum() {
		return numOfRevisedCorrect;
	}
	
	public Set <String> getRevisedAlignment() {
		return revised;
	}
	
	
	private static String toDecimalFormat(double precision) {
		DecimalFormat df = new DecimalFormat("0.000");
		return df.format(precision).replace(',', '.');
	}
		
}
