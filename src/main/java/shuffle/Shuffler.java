package shuffle;

import tags.Song;

/**
 * @author Abhinav Sharma
 */
public interface Shuffler {
  public void initialize(String directory);

  public Song next();

  public void feedback(Song song, double duration);

}
