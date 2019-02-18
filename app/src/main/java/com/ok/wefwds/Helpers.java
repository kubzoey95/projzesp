package com.ok.wefwds;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static java.lang.Math.min;

/**
 * Created by kubzoey on 2019-02-18.
 */

public class Helpers {
    public static double[] distances(double[] points){
        int len = points.length;
        double[] dists = new double[len];
        for(int i = 1;i<len;i++){
            dists[i-1] = points[i] - points[i-1];
        }
        return dists;
    }

    public static double mean(double[] points){
        double sum=0;
        double len = points.length;
        for(int i=0;i<len;i++){
            sum += points[i];
        }
        return sum / len;
    }

    public static double[] clusters(double[] points, int k){
        Arrays.sort(points);
        int len = points.length;
        ArrayList<Double[]> centers;
        int actual_len;
        double[] dists = distances(points);
        centers = new ArrayList<>();
        double mean_dist = mean(dists);
        int first = 0;
        Comparator<Double[]> comp = new Comparator<Double[]>() {
            @Override
            public int compare(Double[] o1, Double[] o2) {
                return Double.compare(o1[1], o2[1]);
            }
        };
        for (int i = 1; i < len; i++) {
            if (points[i] - points[i - 1] >= mean_dist) {
                Double[] entity = new Double[2];
                entity[0] = (points[first] + points[i - 1]) / 2.;
                entity[1] = (double)(i - first);
                centers.add(entity);
                first = i;
            }
        }
        Double[] entity = new Double[2];
        entity[0] = (points[first] + points[len - 1]) / 2.;
        entity[1] = (double)((len - 1) - first);
        centers.add(entity);
        Collections.sort(centers, comp);
        actual_len = centers.size();
        len = min(k, actual_len);
        points = new double[len];
        for(int i=0;i<k;i++){
            points[len-1 - i] = centers.get(actual_len - 1 - i)[0];
        }
        return points;
    }

    public static double[] getH(Mat l) {
        double[] lines = new double[l.height()];

        for (int i = 0; i < l.height(); i++) {
            lines[i] = l.get(i, 0)[1];
        }

        return Helpers.clusters(lines, 5);
    }
}
