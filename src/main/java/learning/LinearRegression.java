package learning;

import org.la4j.LinearAlgebra;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;

/**
 * @author Abhinav Sharma
 * 
 */
public class LinearRegression {

  private static final int MAX_TRAINING_SETS = 1000;
  private int numFeatures;
  private double features[][];
  private double results[][];
  private int currentPos;
  private double model[];

  public LinearRegression(int numFeatures) {
    this.numFeatures = numFeatures;
    this.features = new double[MAX_TRAINING_SETS][numFeatures];
    this.results = new double[MAX_TRAINING_SETS][1];
    this.model = new double[numFeatures];
  }
  
  public void reset() {
    this.currentPos = 0;
  }

  private Matrix solveMatrix() {
    Matrix dim = new Basic2DMatrix(features);
    Matrix f = new Basic2DMatrix(results);

    Matrix first = dim.transpose().multiply(dim);
    MatrixInverter inverter = first.withInverter(LinearAlgebra.GAUSS_JORDAN);

    Matrix firstinv = null;
    try {
      firstinv = inverter.inverse();
      Matrix second = dim.transpose().multiply(f);
      return firstinv.multiply(second);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public void computeModel() {
    Matrix modelCoeff = solveMatrix();
    for (int i = 0; i < numFeatures; ++i) {
      model[i] = modelCoeff == null ? 0 : modelCoeff.get(i, 0);
    }
  }
  
  public double[] getModel() {
    return model;
  }

  public boolean consumeTrainingData(double result, double... features) throws TooManyTrainingSetsException {
    if (features.length != numFeatures)
      return false;
    if (currentPos++ > MAX_TRAINING_SETS) {
      throw new TooManyTrainingSetsException();
    }

    for (int i = 0; i < numFeatures; ++i) {
      this.features[currentPos][i] = features[i];
    }

    this.results[currentPos][0] = result;
    return true;
  }
}
