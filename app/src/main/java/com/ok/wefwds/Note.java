package com.ok.wefwds;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by kubzoey on 2018-12-19.
 */

public class Note {
    double x;
    double y;
    public Note(double x, double y){
        this.x = x;
        this.y = y;
    }

    public static List<Note> batchOfNotes(List<Point> points){
        Comparator<Point> comp = new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return Double.compare(o1.x, o2.x);
            }
        };
        Collections.sort(points, comp);
        List<Note> notes = new ArrayList<>();
        for (Point point : points) {
            notes.add(new Note(point.x, point.y));
        }
        return notes;
    }
}
