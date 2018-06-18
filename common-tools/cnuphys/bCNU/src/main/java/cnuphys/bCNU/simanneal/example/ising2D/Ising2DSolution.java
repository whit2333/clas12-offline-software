package cnuphys.bCNU.simanneal.example.ising2D;

import java.util.Random;

import cnuphys.bCNU.simanneal.Solution;

public class Ising2DSolution extends Solution {
	
	//min and max cities
	private static final int MIN_DIM = 4;
	private static final int MAX_DIM = 200;
	
	//the data
	private int _spins[][];
	
	//the dimensions
	private int _numRow;
	private int _numColumn;
	
	//random number generator
	private static Random _rand = new Random();

	//the simulation owner
	private Ising2DSimulation _simulation;

	public Ising2DSolution(Ising2DSimulation simulation, int numRow, int numColumn) {
		
	}
	
	public void reset(int numRow, int numColumn) {
		_numRow = Math.max(MIN_DIM, Math.min(MAX_DIM, numRow));
		_numColumn = Math.max(MIN_DIM, Math.min(MAX_DIM, numColumn));
		
		_spins = new int[numRow][numColumn];
		
		for (int row = 0; row < _numRow; row++) {
			for (int col = 0; col < _numColumn; col++) {
				_spins[row][col] = (_rand.nextDouble() < 0.5) ? 1 : -1;
			}			
		}
	}
	
	//get the spins of the neighbors using wrap around bc
	private void getNeighborsWrap(int row, int col, int[] neighbors) {
		int rowm1 = (row == 0) ? (_numRow-1) : row-1;
		int rowp1 = (row == (_numRow-1)) ? 0 : row+1;
		int colm1 = (col == 0) ? (_numColumn-1) : col-1;
		int colp1 = (col == (_numColumn-1)) ? 0 : col+1;
		neighbors[0] = _spins[rowm1][colm1];
		neighbors[1] = _spins[rowm1][colp1];
		neighbors[2] = _spins[rowp1][colm1];
		neighbors[3] = _spins[rowp1][colp1];
	}

	@Override
	public double getEnergy() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Solution getRearrangement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Solution copy() {
		// TODO Auto-generated method stub
		return null;
	}

}
