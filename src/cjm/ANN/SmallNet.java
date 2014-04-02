package cjm.ANN;

public class SmallNet {
	
	private int _inputCount;
	
	private double[] _hiddenLayer;
	private double[][] _hiddenWeights;
	private double[] _hiddenDelta;
	
	private double[] _output;
	private double[] _outputDelta;
	private double [][] _outputWeights;
	
	private double _mu;
	
	public SmallNet(int inputCount, int hiddenCount, int outputCount, double mu){
		
		_inputCount = inputCount;
		
		_hiddenLayer = new double[hiddenCount];
		_hiddenWeights = new double[hiddenCount][inputCount];
		_hiddenDelta = new double[hiddenCount];
		
		_output = new double[outputCount];
		_outputWeights = new double[outputCount][hiddenCount];
		_outputDelta = new double[outputCount];
		
		_mu = mu;
	
		for(int i = 0; i < _hiddenLayer.length; i++){
			for(int j = 0; j < _inputCount; j++){
				_hiddenWeights[i][j] = (Math.random() - 0.5) * 2.5;
			}
		}
		for(int i = 0; i < _output.length; i++){
			for(int j = 0; j < _hiddenLayer.length; j++){
				_outputWeights[i][j] = (Math.random() - 0.5) * 2.5;
			}
		}
	}
	
	public double[] getNext(double[] inputs){
		
		double value;
		 
		for(int i = 0; i < _hiddenLayer.length; i++){
			value = 0;
			for(int j = 0; j < _inputCount; j++){
				value += (inputs[j] * _hiddenWeights[i][j]);
			}
			_hiddenLayer[i] = transfer(value);
		}

		for(int i = 0; i < _output.length; i++){
			value = 0;
			for(int j = 0; j < _hiddenLayer.length; j++){
				value += (_hiddenLayer[j] * _outputWeights[i][j]);
			}
			_output[i] = transfer(value);
		}
		return _output;
	}
	
	public double[] train(double[] scans){
		
		double value;
		
		getNext(scans);
		
		for(int i = 0; i < _output.length; i++){
			_outputDelta[i] = dTransfer(_output[i]) * (scans[i + _inputCount] - _output[i]);
		}
		
		for(int i = 0; i < _hiddenLayer.length; i++){
			value = 0;
			for(int j = 0; j < _output.length; j++){
				value += _outputDelta[j] * _outputWeights[j][i];
			}
			_hiddenDelta[i] = dTransfer(_hiddenLayer[i]) * value;
		}
		for(int i = 0; i < _output.length; i++){
			for(int j = 0; j < _hiddenLayer.length; j++){
				_outputWeights[i][j] += (_outputDelta[i] * _hiddenLayer[j] * _mu);
			}
		}
		
		for(int i = 0; i < _hiddenLayer.length; i++){
			for(int j = 0; j < _inputCount; j++){
				_hiddenWeights[i][j] += (_hiddenDelta[i] * scans[j] * _mu);
			}
		}
		return _output;
	}

	private double transfer(double x){
		return (2 / (1 + Math.exp(-2 * x))) - 1;
	}
	
	private double dTransfer(double y){
		return 1 - y * y;
	}
}
