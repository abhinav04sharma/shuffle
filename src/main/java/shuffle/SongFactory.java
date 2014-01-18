package shuffle;

import java.util.List;

import tags.Song;

/**
 * @author Abhinav Sharma
 */
public class SongFactory {

  private static Song           currentSong = null;
  private final static Shuffler shuffle     = new SVMShuffler();

  public Song initialize(String musicDirectory, String dataDirectory) {
    shuffle.initialize(musicDirectory, dataDirectory);
    currentSong = shuffle.next();
    return currentSong;
  }

  public Song getCurrent() {
    return currentSong;
  }

  public void setCurrent(double prevSongDuration, double prevSongMaxDuration, Song song) {
    feedback(prevSongDuration, prevSongMaxDuration);
    currentSong = song;
  }

  public Song next(double prevSongDuration, double prevSongMaxDuration) {
    feedback(prevSongDuration, prevSongMaxDuration);
    currentSong = shuffle.next();
    return currentSong;
  }

  public List<Song> getSongs() {
    return shuffle.getSongs();
  }

  private void feedback(double prevSongDuration, double prevSongMaxDuration) {
    shuffle.feedback(currentSong, prevSongDuration, prevSongMaxDuration);
  }
}
