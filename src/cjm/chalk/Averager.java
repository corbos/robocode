package cjm.chalk;

public class Averager {

	private int _depth;
	private int _index;
	private double _samples;
	private double[] _values;
	private double _totalValue;
	
	public Averager(int depth){
		_depth = depth;
		_values = new double[_depth];
	}
	
	public void addValue(double value){
		if(_index >= _depth){
			_totalValue -= _values[_index % _depth];
		}
		_totalValue += value;
		_values[_index % _depth] = value;
		_index++;
		 _samples = Math.min((double)_index, (double)_depth);
	}
	
	public double getAverage(){
		return _totalValue / _samples;
	}
	
	public double getTotalValue(){
		return _totalValue;
	}
}
