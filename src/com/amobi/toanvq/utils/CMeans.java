package com.amobi.toanvq.utils;


import com.amobi.toanvq.sql.AdReader;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.random.Normal;


public class CMeans {

    private BasicMatrix data;

    private int K;

    private double m;

    private BasicMatrix u;
    private BasicMatrix[] center;

    private BasicMatrix.Factory<PrimitiveMatrix> factory= PrimitiveMatrix.FACTORY;


    // constructor
    public CMeans(BasicMatrix data, int K, double m){
        this.K=K;
        this.data=data;
        this.m=m;
    }


    public void init(){
        MatrixUtilities.normalizeMatrix(data);

        initU();
        initCenter();
        
    }
    public void initU(){
        u=factory.makeRandom(data.countRows(), K, new Normal());
        u=MatrixUtilities.normalizeMatrix(u);
    }

    public void initCenter(){
        center=new BasicMatrix[K];
        
        for (int i=0; i<K; i++){

            center[i]=factory.makeRandom(0, data.countColumns(), new Normal());
            center[i]=MatrixUtilities.normalizeMatrix(center[i]);

        }
    }


    //Update u
    public void updateU(){

        u=factory.makeZero(data.countRows(), K);
        for (int i=0; i< data.countRows(); i++){
            
            BasicMatrix elementI=data.selectRows(i);
            for (int k=0; k<K; k++){

                double sum=0;
                double dist1=MatrixUtilities.distanceBetweenMatrix(center[k], elementI);

                for (int j=0; j<K; j++){
                    double dist2=MatrixUtilities.distanceBetweenMatrix(center[j], elementI);
                    sum=sum+Math.pow(dist1/dist2, 2.0/(m-1));
                    
                }

                u.add(i, k, 1.0 / sum);

            }
        }
    }



    /**
     * Center[i]= (Sigma u[i, j]^m*x[j])/(Sigma u[i, j]^m)
     */
    public void updateCenter(){
        for (int i=0; i<K; i++){

            // Calculate Sigma u[i, j]^m
            double sum=0;
            for (int j=0; j<data.countRows(); j++){
                sum=sum+Math.pow(u.get(j, i).doubleValue(), m);
            }

            // init sumVector=[0............0]
            BasicMatrix sumVector=factory.makeZero(1, data.countColumns());

            // sumVector=Sigma u[i, j]^m* x[j]
            for (int j=0; j<data.countRows(); j++){
                sumVector=sumVector.add(data.selectRows(j).multiply(Math.pow(u.get(j, i).doubleValue(), m)));
            }

            // calculate center[i]
            center[i]=sumVector.divide(sum);
            center[i]=MatrixUtilities.normalizeMatrix(center[i]);
        }
    }

    //Execute
    public BasicMatrix execute(int times){
        init();
        for (int i=0; i<times; i++) {
            //System.out.println ("Update center times "+i);
            updateCenter();
            //System.out.println ("Update measure BasicMatrix u times "+i);
            updateU();
        }

        return u;
    }
}

