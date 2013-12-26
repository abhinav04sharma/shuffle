/**
 * 
 */
package shuffle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javafx.application.Application;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import learning.LinearRegression;
import learning.TooManyTrainingSetsException;
import tags.Song;

import com.mpatric.mp3agic.ID3v2;

/**
 * @author Abhinav Sharma
 * 
 */
public class Shuffle {
  private TagExtractor tagExtractor;
  private LinearRegression lr = new LinearRegression(2);
  private ArrayList<String> genres;
  private ArrayList<String> artists;
  private int count = 0;

  Shuffle(String directory) {
    this.tagExtractor = new TagExtractor(directory);
  }

  public static void main(String args[]) {
    new Shuffle("D:/My Music").run();
  }
  
  private void rateSong(Song song) {
    ID3v2 tag = song.getTag();
    // no genre or artist so the rating is least
    if(tag.getGenreDescription() == null/* || tag.getArtist() == null*/) {
      song.setRating(Double.NEGATIVE_INFINITY);
      return;
    }
    
    // get artist and genre indices
    int genreIndex = genres.indexOf(tag.getGenreDescription()) + 1;
    int artistIndex = artists.indexOf(song.getTag().getArtist()) + 1;
    
    // genre not found so... -ve infinity!
    if(genreIndex == 0) {
      song.setRating(Double.NEGATIVE_INFINITY);
      return;
    }

    // get model
    double[] model = lr.getModel();
    
    double rating = model[0] + genreIndex*model[1] + artistIndex*model[2];
    song.setRating(rating);
  }
  
  private String readInput() {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String response = null;
    try {
       response = br.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return response;
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

    Collections.shuffle(songs);
    
    double prevModel[] = lr.getModel();
    
    for (Iterator<Song> iter = songs.iterator(); iter.hasNext();) {
      if (count == 10) {
        // compute model
        lr.computeModel();
        // set rating for all songs
        for(Song s : songs) { rateSong(s); }
        // sort according to rating
        Collections.sort(songs, Collections.reverseOrder());
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
      
      
      // features we want are not present, no point learning (TODO: but we should still play it!)
      if (tag.getGenreDescription() == null || tag.getArtist() == null) continue;
      
      // do we like this song?
      System.out.println(tag.getArtist() + "#" + tag.getGenreDescription());
//      String fileName = "file:///" + song.getFileName().replaceAll("\\\\", "/").replaceAll(" ", "%20");
//      Media hit = new Media(fileName);
//      MediaPlayer mediaPlayer = new MediaPlayer(hit);
//      mediaPlayer.play();
      long startTime = System.nanoTime();
      readInput();
//      mediaPlayer.stop();
      long estimatedTime = System.nanoTime() - startTime;
      double userRating = estimatedTime; 
      //tag.getGenreDescription().contains("Alternative") && tag.getArtist().contains("Coldplay") ? 1 : 0;
      
      // get indices
      int genreIndex = genres.indexOf(song.getTag().getGenreDescription()) + 1;
      int artistIndex = artists.indexOf(song.getTag().getArtist()) + 1;

      // couldn't find the genre
      if (genreIndex == 0) { continue; }

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
