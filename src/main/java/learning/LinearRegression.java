package learning;

import org.la4j.LinearAlgebra;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;

/**
 * @author Abhinav Sharma
 */
public class LinearRegression {

  private static final int MAX_TRAINING_SETS = 1000;
  private final int        numFeatures;
  private final double     features[][];
  private final double     results[][];
  private int              currentPos        = 0;
  private final double     model[];

  public LinearRegression(int numFeatures) {
    this.numFeatures = numFeatures;
    this.features = new double[MAX_TRAINING_SETS][numFeatures + 1];
    this.results = new double[MAX_TRAINING_SETS][1];
    this.model = new double[numFeatures + 1];
  }

  public void reset() {
    this.currentPos = 0;
  }

  private Matrix solveMatrix() {
    Matrix fea = new Basic2DMatrix(features);
    Matrix res = new Basic2DMatrix(results);

    Matrix first = fea.transpose().multiply(fea);
    MatrixInverter inverter = first.withInverter(LinearAlgebra.INVERTER);

    Matrix firstinv = null;
    try {
      firstinv = inverter.inverse();
      Matrix second = fea.transpose().multiply(res);
      return firstinv.multiply(second);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public void computeModel() {
    Matrix modelCoeff = solveMatrix();
    for (int i = 0; i < numFeatures + 1; ++i) {
      model[i] = modelCoeff == null ? 0 : modelCoeff.get(i, 0);
    }
    System.out.println("Model: " + model.toString());
  }

  public double[] getModel() {
    return model;
  }

  public boolean consumeTrainingData(double result, double... features) throws TooManyTrainingSetsException {
    if (features.length != numFeatures)
      return false;
    if (currentPos == MAX_TRAINING_SETS) {
      throw new TooManyTrainingSetsException();
    }

    this.features[currentPos][0] = 1;
    for (int i = 1; i <= numFeatures; ++i) {
      this.features[currentPos][i] = features[i - 1];
    }

    this.results[currentPos][0] = result;
    ++currentPos;
    return true;
  }
}
