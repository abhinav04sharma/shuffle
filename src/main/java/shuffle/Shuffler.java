/**
 * 
 */
package shuffle;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import learning.LinearRegression;
import learning.TooManyTrainingSetsException;
import tags.Song;

/**
 * @author Abhinav Sharma
 */
public class Shuffler {
  private TagExtractor tagExtractor;
  private LinearRegression lr = new LinearRegression(2);

  private List<Song> songs;
  
  private int count = 0;
  private Iterator<Song> iter; 

  public Shuffler(String directory) {
    this.tagExtractor = new TagExtractor(directory);
  }
  
  public List<Song> getSongs() {
    return songs;
  }
  
  private void rateSong(Song song) {
    
    double originalRating;

    // store original rating
    originalRating = song.getRating();
    
    do {

      // get artist and genre indices
      int genreIndex = tagExtractor.getGenreIndex(song);
      int artistIndex = tagExtractor.getArtistIndex(song);

      // index not found so... -ve infinity!
      if(genreIndex == -1 || artistIndex == -1) {
        song.setRating(0);
        break;
      }

      // get model
      double[] model = lr.getModel();

      // set rating
      double rating = model[0] + (genreIndex + 1) * model[1] + (artistIndex + 1) * model[2];
      song.setRating(rating);

    } while (false);
    
    // case: song was played, print original and predicted rating
    if (song.isPlayed()) {
      System.out.println(song + " Original rating: " + originalRating + " Predicted Rating: " + song.getRating());
    }
  }
  
  public void initialize() {
    try {
      tagExtractor.run();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    songs = tagExtractor.getSongs();
    iter = songs.iterator();
    count = 0;
    Collections.shuffle(songs);
  }
  
  public Song next() {
    
    if (iter.hasNext()) {
      if (count == 10) {
        // save previous model
        double prevModel[] = lr.getModel();
        // compute model
        lr.computeModel();
        // set rating for all songs
        for(Song s : songs) { rateSong(s); }
        // sort according to rating
        Collections.sort(songs, Collections.reverseOrder());
        // shuffle sub list
        Collections.shuffle(songs.subList(0, 20));
        // reset counter
        count = 0;
        // reset iterator, only of the model has changed!
        if (prevModel != lr.getModel())
          iter = songs.iterator();
        // reset linear regression
        lr.reset();
      }
    } else {
        iter = songs.iterator();
        count = 0;
    }
    ++count;
    return iter.next();
  }
  
  public void feedback(Song song, double duration) {
    // get indices
    int genreIndex = tagExtractor.getGenreIndex(song);
    int artistIndex = tagExtractor.getArtistIndex(song);
    
    song.setPlayed(true);
    song.setRating(duration);

    // consume the data for training
    try {
      lr.consumeTrainingData(duration, genreIndex + 1, artistIndex + 1);
    } catch (TooManyTrainingSetsException e) {
      e.printStackTrace();
    }
  }
}