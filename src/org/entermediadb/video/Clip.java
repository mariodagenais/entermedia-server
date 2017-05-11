package org.entermediadb.video;

import java.util.Map;

import org.openedit.data.ValuesMap;


public class Clip implements Comparable
{
	protected ValuesMap fieldData;
	
	public ValuesMap getData()
	{
		return fieldData;
	}
	public void setData(Map inData)
	{
		fieldData = new ValuesMap(inData);
	}
	public double getStart()
	{
		Double d = getData().getDouble("timecodestart");
		if( d == null)
		{
			return 0d;
		}
		return d;
	}
	public double getLength()
	{
		Double d = getData().getDouble("timecodelength");
		if( d == null)
		{
			return 0d;
		}
		return d;
	}
	
	public Object getValue(String inKey)
	{
		return getData().get(inKey);
	}
	@Override
	public int compareTo(Object inO)
	{
		Clip clip = (Clip)inO;
		double d1 = getStart();
		double d2 = clip.getStart();
		if(d1 < d2){
			return -1;
		}
		if(d2 < d1){
			return 1;
		}
		return 0;
	}
}