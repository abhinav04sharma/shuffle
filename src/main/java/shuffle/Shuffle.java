/**
 * 
 */
package shuffle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.mpatric.mp3agic.ID3v2;

import learning.LinearRegression;
import learning.TooManyTrainingSetsException;
import tags.Song;

/**
 * @author Abhinav Sharma
 * 
 */
public class Shuffle {
  private TagExtractor tagExtractor;
  private LinearRegression lr = new LinearRegression(2);
  private ArrayList<String> genres;
  private ArrayList<String> artists;

  Shuffle(String directory) {
    this.tagExtractor = new TagExtractor(directory);
  }

  public static void main(String args[]) {
    new Shuffle("D:/My Music").run();
  }
  
  private void rateSong(Song song) {
    ID3v2 tag = song.getTag();
    // no genre or artist so the rating is 0
    if(tag.getGenreDescription() == null || tag.getArtist() == null) {
      song.setRating(Double.MAX_VALUE);
      return;
    }
    
    // get artist and genre indices
    int genreIndex = Collections.binarySearch(genres, tag.getGenreDescription()) + 1;
    int artistIndex = Collections.binarySearch(artists, song.getTag().getArtist()) + 1;

    // feed this song to linear regression to find coefficients
    LinearRegression currentSongLR = new LinearRegression(2);
    try {
      currentSongLR.consumeTrainingData(1, genreIndex, artistIndex);
    } catch (TooManyTrainingSetsException e) {
      e.printStackTrace();
    }

    // get coefficients for this song!
    currentSongLR.computeModel();
    double[] currentSongModel = currentSongLR.getModel();
    double[] model = lr.getModel();
    
    // current model is null
    if(currentSongModel == new double[]{0.0, 0.0}) {
      song.setRating(Double.MAX_VALUE);
      return;
    }

    // take average of variance in each feature
    double rating =  (Math.abs(currentSongModel[0] - model[0])) + (Math.abs(currentSongModel[1] - model[1])) / 2;
    song.setRating(rating);
  }
  
  public void run() {
    try {
      tagExtractor.run();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    List<Song> songs = tagExtractor.getSongs();
    genres = new ArrayList<String>(tagExtractor.getGenres());
    artists = new ArrayList<String>(tagExtractor.getArtists());

    int count = 0;
    Collections.shuffle(songs);
    
    double prevModel[] = lr.getModel();
    
    for (Iterator<Song> iter = songs.iterator(); iter.hasNext();) {
      if (count == 50) {
        // compute model
        lr.computeModel();
        // set rating for all songs
        for(Song s : songs) { rateSong(s); }
        // sort according to rating
        Collections.sort(songs);
        // reset counter
        count = 0;
        // reset iterator, only of the model has changed!
        if (prevModel != lr.getModel()) iter = songs.iterator();
        prevModel = lr.getModel();
        // reset linear regression
        lr.reset();
      }
      Song song = iter.next();
      ID3v2 tag = song.getTag();
      
      System.out.println(tag.getArtist() + "#" + tag.getGenreDescription());
      
      // features we want are not present, no point learning (TODO: but we should still play it!)
      if (tag.getGenreDescription() == null || tag.getArtist() == null) continue;
      
      // do we like this song?
      int userRating = tag.getGenreDescription().endsWith("Rock") ? 1 : 0;
      // get indices
      int genreIndex = Collections.binarySearch(genres, song.getTag().getGenreDescription()) + 1;
      int artistIndex = Collections.binarySearch(artists, song.getTag().getArtist()) + 1;
      // consume the data for training
      try {
        lr.consumeTrainingData(userRating, genreIndex, artistIndex);
      } catch (TooManyTrainingSetsException e) {
        e.printStackTrace();
      }
      // increment count!
      ++count;
    }
  }
}
