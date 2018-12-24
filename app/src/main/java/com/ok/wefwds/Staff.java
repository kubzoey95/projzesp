package com.ok.wefwds;

import org.billthefarmer.mididriver.MidiDriver;

import java.util.Arrays;

import static java.lang.Math.round;
import static java.lang.Math.signum;

/**
 * Created by kubzoey on 2018-12-19.
 */

public class Staff {
    double[] lines;
    double c_height;
    double line_interval;
    final MidiDriver midi;
    public Staff(double[] lines){
        midi = new MidiDriver();
        Arrays.sort(lines);
        this.lines = lines;
        double sum = 0;
        double len = lines.length;
        double down = lines[(int)(len - 1)];
        for(int i=1;i<len;i++){
            sum += lines[i] - lines[i - 1];
        }
        sum = sum / (len - 1);
        line_interval = sum / 2;
        c_height = down + (2 * line_interval);
        midi.start();
        midi.config();
    }

    static int[] diatonic = new int[] {0,2,4,5,7,9,11};

    public int getIndex(Note note){
        int index = (int)round((c_height - note.y) / line_interval);
        int octaves = index / 7;
        index = (index % 7);
        index = (octaves * 12) + diatonic[(7 + index) % 7] * (int)signum(index);
        index += 60;
        return index;
    }

    public void playNote(Note note){
        midi.write(new byte[]{(byte)0x90, (byte)getIndex(note), (byte)63});
        synchronized (midi){
            try {
                midi.wait(1000);
            }catch (InterruptedException e){

            }
        }
        midi.write(new byte[]{(byte)0x80, (byte)getIndex(note), (byte)63});
    }
}
