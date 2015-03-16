package com.amobi.toanvq.predict;

import com.amobi.toanvq.sql.AdReader;
import org.ojalgo.matrix.BasicMatrix;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by dangchienhsgs on 30/01/2015.
 */
public class PredictAds {
    private int numCluster;
    private List<Integer> listID;
    private List<Integer> listCluster;
    public PredictAds(String clusterFiles, String idFiles) throws IOException{
        FileInputStream fileInputStream=new FileInputStream(idFiles);
        Scanner scanner=new Scanner(fileInputStream);

        listID=new ArrayList<Integer>();
        listCluster=new ArrayList<Integer>();

        // finish reading id Files
        while (scanner.hasNextLine()){
            String line=scanner.nextLine();

            String args[]=line.split(" ");
            listID.add(Integer.parseInt(args[1]));
        }

        fileInputStream.close();
        scanner.close();


        fileInputStream=new FileInputStream(clusterFiles);
        scanner=new Scanner(fileInputStream);

        numCluster=Integer.parseInt(scanner.nextLine());
        while (scanner.hasNextLine()){
            String line=scanner.nextLine();
            String args[]=line.split(" ");
            int label=0;

            for (int i=0; i<args.length; i++){
                if (Float.parseFloat(args[i])==1){
                    label=i;
                }
            }
            listCluster.add(label);
        }
    }

    public void predict(){
        AdReader adReader=new AdReader();
        BasicMatrix<Double> matrix=adReader.read();

        ArrayList<Attribute> attributes=new ArrayList<Attribute>();
        for (int i=0; i<matrix.countColumns(); i++){
            attributes.add(new Attribute("ATTR "+i));
        }

        Attribute classAttr=new Attribute("Class");
        attributes.add(classAttr);
        Instances data=new Instances("AdData", attributes, 0);

        data.setClass(classAttr);
        for (int i=0; i<matrix.countRows(); i++){
            Instance instance=new DenseInstance((int) matrix.countColumns());
            instance.setDataset(data);


            for (int j=0; j<matrix.countColumns(); j++){
                instance.setValue(j, matrix.get(i, j));
            }

            if (listID.contains(adReader.idList.get(i))){
                instance.setClassValue(listCluster.get(listID.indexOf(adReader.idList.get(i))));
            } else {
                instance.setClassMissing();
            }
        }


    }

}
