package player;

import javafx.scene.media.MediaPlayer;
import shuffle.SVMShuffler;
import shuffle.Shuffler;
import tags.Song;

/**
 * @author Abhinav Sharma
 */
public class SongFactory {

  private static Song           currentSong = null;
  private final static Shuffler shuffle     = new SVMShuffler();

  public Song initialize(String directory) {
    shuffle.initialize(directory);
    currentSong = shuffle.next();
    return currentSong;
  }

  public Song next(MediaPlayer player) {
    shuffle.feedback(currentSong, player.getCurrentTime().toSeconds());
    currentSong = shuffle.next();
    return currentSong;
  }
}
