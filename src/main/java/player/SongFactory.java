package player;

import java.util.List;

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

  public Song getCurrent() {
    return currentSong;
  }

  public void setCurrent(MediaPlayer player, Song song) {
    feedback(player);
    currentSong = song;
  }

  public Song next(MediaPlayer player) {
    feedback(player);
    currentSong = shuffle.next();
    return currentSong;
  }

  public List<Song> getSongs() {
    return shuffle.getSongs();
  }

  private void feedback(MediaPlayer player) {
    shuffle.feedback(currentSong, player.getCurrentTime().toSeconds());
  }
}
