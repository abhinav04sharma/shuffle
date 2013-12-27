package player;

import java.util.List;

import javafx.scene.media.MediaPlayer;
import shuffle.Shuffler;
import tags.Song;


/**
 * @author Abhinav Sharma
 */
public class SongFactory {
  
  private static Song currentSong = null;
  private final static Shuffler shuffle = new Shuffler("D:/My Music");
  
  public Song first() {
    shuffle.initialize();
    currentSong = shuffle.next();
    return currentSong;
  }
  
  public Song next(MediaPlayer player) {
    shuffle.feedback(currentSong, player.getCurrentTime().toSeconds());
    currentSong = shuffle.next();
    return currentSong;
  }
  
  public List<Song> getSongs() {
    return shuffle.getSongs();
  }

}
