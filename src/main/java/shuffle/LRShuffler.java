package shuffle;

import java.util.Collections;

import learning.LinearRegression;
import learning.TooManyTrainingSetsException;
import tags.Song;

/**
 * @author Abhinav Sharma
 */
public class LRShuffler extends Shuffler {

  private LinearRegression lr;

  @Override
  public void initialize(String musicDirectory, String dataDirectory) {
    super.initialize(musicDirectory, dataDirectory);
    lr = new LinearRegression(2);
  }

  @Override
  protected int getRating(double durationPlayed, double maxDuration) {
    int rating = (int) (durationPlayed);
    return rating;
  }

  @Override
  protected void rateSongs() {
    for (Song s : songs) {
      rateSong(s);
    }
    // sort according to rating
    Collections.sort(songs, Collections.reverseOrder());
    // shuffle sub list
    Collections.shuffle(songs.subList(0, 20));
  }

  @Override
  protected void consumeData(Song song, int rating) {
    // get indices
    int genreIndex = getTagExtractor().getGenreIndex(song);
    int artistIndex = getTagExtractor().getArtistIndex(song);

    // consume the data for training
    try {
      lr.consumeTrainingData(rating, genreIndex + 1, artistIndex + 1);
    } catch (TooManyTrainingSetsException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void computeModel() {
    lr.computeModel();
  }

  @Override
  protected void resetLearner() {
    // reset linear regression
    lr.reset();
  }

  private void rateSong(Song song) {
    // get artist and genre indices
    int genreIndex = getTagExtractor().getGenreIndex(song);
    int artistIndex = getTagExtractor().getArtistIndex(song);

    // index not found so... zero!
    if (genreIndex == -1 || artistIndex == -1) {
      song.setRating(0);
      return;
    }

    // get model
    double[] model = lr.getModel();

    // set rating
    double rating = model[0] + (genreIndex + 1) * model[1] + (artistIndex + 1) * model[2];
    song.setRating(rating);
  }

}
