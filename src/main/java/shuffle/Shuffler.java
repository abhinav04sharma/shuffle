package shuffle;

import java.util.List;

import tags.Song;

/**
 * @author Abhinav Sharma
 */
public interface Shuffler {
  public void initialize(String directory);

  public Song next();

  public void feedback(Song song, double duration);

  public List<Song> getSongs();

}
