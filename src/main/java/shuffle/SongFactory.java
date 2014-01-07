package shuffle;

import java.util.List;

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

  public void setCurrent(double prevSongDuration, Song song) {
    feedback(prevSongDuration);
    currentSong = song;
  }

  public Song next(double prevSongDuration) {
    feedback(prevSongDuration);
    currentSong = shuffle.next();
    return currentSong;
  }

  public List<Song> getSongs() {
    return shuffle.getSongs();
  }

  private void feedback(double prevSongDuration) {
    shuffle.feedback(currentSong, prevSongDuration);
  }
}
