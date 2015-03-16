package com.amobi.toanvq.utils;


import com.amobi.toanvq.sql.AdReader;

import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.random.Normal;


public class KMeans {
    private int K;

    @SuppressWarnings("rawtypes")
	private BasicMatrix data;
    @SuppressWarnings("rawtypes")
	private BasicMatrix center[];
    @SuppressWarnings("rawtypes")
	private BasicMatrix x[];
    private double w[][];

    private long numRows;
    private long dimension;

    double oldResult=0;

    private BasicMatrix.Factory<PrimitiveMatrix> factory= PrimitiveMatrix.FACTORY;


    @SuppressWarnings("rawtypes")
	public KMeans(int K, BasicMatrix data) {
        this.K = K;
        this.data = data;
    }

    public void init() {

        // get number of rows and columns
        numRows = data.countRows();
        dimension = data.countColumns();

        // assign array of matrix x to array of rows in data
        x=new BasicMatrix[(int) numRows];
        for (int i = 0; i < numRows; i++) {
            x[i] = data.selectRows(i);
        }

        // random array of center
        center = new BasicMatrix[K];
        for (int i=0; i<K; i++){
            center[i]=factory.makeRandom(1, dimension, new Normal());
            center[i]=MatrixUtilities.normalizeMatrix(center[i]);
        }

        // random array w
        w = new double[(int) numRows][K];
        for (int i = 0; i < numRows; i++) {
            w[i] = NumberUtils.randomLine(K);
        }


    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public void eStep() {
        for (int i = 0; i < K; i++) {

            BasicMatrix sum = x[0].multiply(w[0][i]);
            double temp = w[0][i];

            for (int j = 1; j < numRows; j++) {
                sum = sum.add(x[j].multiply(w[j][i]));
                temp = temp + w[j][i];
            }
            if (temp != 0) {
                center[i] = sum.divide(temp);
            }
        }
    }

    public void mStep() {
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < K; j++) {
                w[i][j] = 0;
            }
        }
        for (int i = 0; i < numRows; i++) {


            Double min = MatrixUtilities.cosinBetweenMatrix(x[i], center[0]);
            int index = 0;
            for (int j = 1; j < K; j++) {


                //Double temp = MatrixUtilities.distanceBetweenMatrix(x[i], center[j]);
                Double temp = MatrixUtilities.cosinBetweenMatrix(x[i], center[j]);

                if (temp.compareTo(min) > 0) {
                    min = temp;
                    index = j;
                }
            }

            w[i][index] = 1;
        }

    }

    public double computeResult(){
        double result=0;
        for (int i=0; i<data.countRows();i++){

            for (int j=0; j<K; j++){
                if (w[i][j]==1){
                    // doc i in cluster j
                    result=result+MatrixUtilities.cosinBetweenMatrix(x[i], center[j]);
                }
            }
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
	public BasicMatrix execute(int times) {
        init();

        int i=0;

        do{
            i=i+1;
      //      System.out.println("KMeans has been in time " + i);
            eStep();
            mStep();

            double result=computeResult();
            //System.out.println (result);
            if (Math.abs(result-oldResult)<0.0001){
                break;
            } else {
                oldResult=result;
            }

            if (i > times){
                break;
            }
        } while (true);

        return factory.rows(w);
    }

    @SuppressWarnings("rawtypes")
	public static void main(String args[]){
        AdReader adReader = new AdReader();
        BasicMatrix matrix = adReader.read();
        
        int times2run = 300;
        if (args.length > 0) {
			times2run = Integer.parseInt(args[0]);
		}
        
        new KMeans(5, matrix).execute(times2run);
    }

}
