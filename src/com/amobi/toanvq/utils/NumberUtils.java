package com.amobi.toanvq.utils;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class NumberUtils {
    public static double [] randomLine(int n){
        double[] result=new double[n];
        double sum=1;
        for (int i=0; i<n-1; i++){
            double temp=Double.valueOf(Math.random());
            if (temp>sum) {
                int x=(int) (temp/sum);
                temp=temp-x*sum;
            }
            sum=sum-temp;
            result[i]=temp;
        }
        result[n-1]=sum;
        return result;
    }

    public static void addToList(List<Double> element, List<Double> host){
        for (Double str:element){
            host.add(str);
        }
    }

    public static List<Double> addZero(List<Double> list, int number){
        for (int i=0; i<number; i++){
            list.add(Double.valueOf(0));
        }
        return list;
    }

    public static double[] convertListToArray(List<Double> list){
        double array[]=new double[list.size()];

        for (int i=0; i<list.size(); i++){
            array[i]=list.get(i);
        }

        return array;
    }

    public static void printList(String fileName, List<?> list){
        try {
            File file = new File(fileName);

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(file);
            for (int i=0; i<list.size(); i++){
                fileWriter.write(i+" "+list.get(i)+"\n");
            }
            fileWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void toFile (String filename, String data){
        try {
            File file = new File(filename);

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(data);
            fileWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}

