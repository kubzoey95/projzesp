package com.ok.wefwds;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kubzoey on 2018-12-19.
 */

public class Piece {
    List<Note> notes;
    Staff staff;

    public Piece(List<Note> notes, Staff staff){
        this.notes = notes;
        this.staff = staff;
    }

    public  Piece(){
        this.notes = new ArrayList<>();
        this.staff = new Staff();
    }

    public void playNotes(){
        staff.midi.start();
        staff.midi.config();
        for(Note note : notes){
            staff.playNote(note);
        }
        staff.midi.stop();
    }

}
