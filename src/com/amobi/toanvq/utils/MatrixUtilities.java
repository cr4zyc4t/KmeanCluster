package com.amobi.toanvq.utils;



import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.random.Weibull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;


public class MatrixUtilities {

    private static BasicMatrix.Factory<PrimitiveMatrix> factory= PrimitiveMatrix.FACTORY;

    /**
     * Combine two matrices by place them next to each others
     * @param matrix1
     * @param matrix2
     * @return combined BasicMatrix
     * @throws IllegalArgumentException
     */
    public static BasicMatrix combineMatrix(BasicMatrix matrix1, BasicMatrix matrix2, double weight1, double weight2) throws IllegalArgumentException {
        if (matrix1.countRows() != matrix2.countRows()) {
            // If two BasicMatrix do not have same rows
            // throw an exception
            throw new IllegalArgumentException("Two BasicMatrix do not have same number of rows");
        } else {
            int numRows=(int) matrix1.countRows();
            int numColumns=(int) (matrix1.countColumns()+matrix2.countColumns());
            BasicMatrix matrix3=factory.makeZero(numRows, numColumns);

            matrix3=matrix3.add(0, 0, matrix1.multiply(weight1));
            matrix3=matrix3.add(0, (int) matrix1.countColumns(), matrix2).multiply(weight2);

            return matrix3;
        }
    }


    /**
     * Normalize a BasicMatrix
     * @param matrix
     * @return normalized BasicMatrix
     */
    public static BasicMatrix normalizeMatrix(BasicMatrix matrix) {

        // get the dimension of the BasicMatrix
        long numRows = matrix.countRows();
        long numColumns = matrix.countColumns();

        // init the vectors
        BasicMatrix result=factory.makeZero(numRows, numColumns);


        for (int i=0; i<numRows;i++){
            double sum=0;

            for (int j=0; j<numColumns; j++){
                sum=sum+matrix.get(i, j).doubleValue();
            }

            if (sum!=0){
                BasicMatrix matrixRow=matrix.selectRows(i);
                matrixRow=matrixRow.divide(sum);
                result.add(i, 0, matrixRow);
            } else {
                for (int j=0; j<numColumns; j++){
                    matrix.add(i, j, Double.valueOf(1)/numColumns);
                }
            }

        }
        
        return matrix;
    }





    /**
     * Export an BasicMatrix to file
     * @param name
     * @param matrix
     */
    public static void toFile(String name, BasicMatrix matrix) {
        try {
            File file = new File(name);

            if (!file.exists()) {
                file.createNewFile();
            }
            
            FileWriter fileWriter = new FileWriter(file);


            for (int i=0; i<matrix.countRows(); i++){
                for (int j=0; j<matrix.countColumns(); j++){
                    fileWriter.write (matrix.get(i, j)+" ");
                }
                fileWriter.write("\n");
            }

            fileWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Compute the divergence between two vector (the difference between two vector)
     * @param denseVector1
     * @param denseVector2
     * @return
     * @throws IllegalArgumentException
     */
    public static Double divergenceVector(BasicMatrix denseVector1, BasicMatrix denseVector2) throws IllegalArgumentException {
        if (denseVector1.countColumns() != denseVector2.countColumns()) {
            throw new IllegalArgumentException("2 vector khong cung chieu");
        } else {
            int dimension = (int) denseVector1.countColumns();
            Double result = Double.valueOf(0);

            for (int i = 0; i < dimension; i++) {
                Double temp1 = denseVector1.get(0, i).doubleValue();
                Double temp2 = denseVector2.get(0, i).doubleValue();


                Double value = temp1*(Double.valueOf(Math.log(temp1/temp2)));
                
                result = result+(value);
            }
            return result;
        }
    }


    /**
     * Compute the divergence between all pair of vectors in an BasicMatrix
     * @param matrix
     * @return
     */
    public static Double divergenceMatrix(BasicMatrix matrix) {
        int numRows = (int) matrix.countRows();
        Double result = Double.valueOf(0);
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numRows; j++) {
                if (i != j) {
                    Double value = divergenceVector(matrix.selectRows(i), matrix.selectRows(j));
                    result = result+(value);

                }
            }
        }
        return result/((numRows)*(numRows - 1));
    }




    public static Double distanceBetweenMatrix(BasicMatrix matrix1, BasicMatrix matrix2) throws IllegalArgumentException{
        if (matrix1.countColumns() != matrix2.countColumns() || matrix1.countRows()!=matrix2.countRows()) {
            throw new IllegalArgumentException("Two matrix do not have the same size");
        } else {
            double result=0.0;
            BasicMatrix subMatrix=matrix1.subtract(matrix2);
            for (int i=0; i<subMatrix.countRows(); i++){
                for (int j=0; j<subMatrix.countColumns(); j++){
                    result=result+Math.pow(subMatrix.get(0, 0).doubleValue(), 2);
                }
            }
            return Math.sqrt(result);
        }
    }

    public static Double cosinBetweenMatrix(BasicMatrix matrix1, BasicMatrix matrix2) throws IllegalArgumentException{
        if (matrix1.countColumns() != matrix2.countColumns() || matrix1.countRows()!=matrix2.countRows()) {
            throw new IllegalArgumentException("Two matrix do not have the same size");
        } else {
            double result=0.0;
            double product=0;
            double sum1=0;
            double sum2=0;
            for (int i=0; i<matrix1.countColumns(); i++){
                product=product+matrix1.get(0, i).doubleValue()*matrix2.get(0, i).doubleValue();
                sum1=sum1+Math.pow(matrix1.get(0, i).doubleValue(), 2);
                sum2=sum2+Math.pow(matrix2.get(0, i).doubleValue(), 2);
            }


            return product/(Math.sqrt(sum1)*Math.sqrt(sum2));
        }
    }


    public static String printMatrix(BasicMatrix matrix){
        String s="";
        for (int i=0; i<matrix.countRows(); i++){
            for (int j=0; j<matrix.countColumns(); j++){
                s=s+matrix.get(i, j)+" ";
            }
            s=s+"/n";
        }
        return s;
    }

}
