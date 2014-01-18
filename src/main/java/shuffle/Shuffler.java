package shuffle;

import java.util.List;

import tags.Song;

/**
 * @author Abhinav Sharma
 */
public interface Shuffler {
  public void initialize(String musicDirectory, String dataDirectory);

  public Song next();

  public void feedback(Song song, double durationPlayed, double maxDuration);

  public List<Song> getSongs();

}
