package com.ok.wefwds;

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

    public void playNotes(){
        for(Note note : notes){
            staff.playNote(note);
        }
    }

}
