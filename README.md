# MIDIGot
Converts MIDI files to SSTC interrupter files
It converts one voice midi file to special binary form which can be read by arduino using https://github.com/Cooble/SSTCInterrupter  

It will make bunch of (MIDI_FILE_SIZE * 3) byte files which can be put on sd card for arduino to read it.  
The limited size of files is due to limited arduino buffer memory.  
