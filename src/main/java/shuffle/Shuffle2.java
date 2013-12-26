/**
 * 
 */
package shuffle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import tags.Song;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import com.mpatric.mp3agic.ID3v2;

/**
 * @author Abhinav Sharma
 * 
 */
public class Shuffle2 {
  private TagExtractor tagExtractor;
  private Logistic lr = new Logistic();
  private Instances data;
  private ArrayList<String> genres;

  Shuffle2(String directory) {
    this.tagExtractor = new TagExtractor(directory);
  }

  public static void main(String args[]) {
    new Shuffle2("D:/My Music").run();
  }

  private void rateSong(Song song) {
    ID3v2 tag = song.getTag();
    // no genre so the rating is least
    if (tag.getGenreDescription() == null) {
      song.setRating(Double.NEGATIVE_INFINITY);
      return;
    }

    // get artist index
    int genreIndex = genres.indexOf(tag.getGenreDescription()) + 1;

    double rating = 0;
    try {
      Instance inst = new Instance(1.0, new double[] { genreIndex, 0 });
      inst.setDataset(data);
      rating = lr.classifyInstance(inst);
    } catch (Exception e) {
      e.printStackTrace();
    }
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

    // make individual attributes
    Attribute genreAttr = new Attribute("genre");
    Attribute ratingAttr = new Attribute("rating");

    // add all attributes to data-set
    FastVector attributes = new FastVector();
    attributes.addElement(genreAttr);
    attributes.addElement(ratingAttr);
    data = new Instances("songs", attributes, 50);
    data.setClassIndex(data.numAttributes() - 1);

    int count = 0;
    Collections.shuffle(songs);

    //double prevModel[] = null;

    for (Iterator<Song> iter = songs.iterator(); iter.hasNext();) {
      if (count == 50) {
        // compute model
        try {
          lr.buildClassifier(data);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        // set rating for all songs
        for (Song s : songs) {
          rateSong(s);
        }
        // sort according to rating
        Collections.sort(songs, Collections.reverseOrder());
        // reset counter
        count = 0;
        // reset iterator, only of the model has changed!
        /*if (prevModel != lr.coefficients())
          iter = songs.iterator();
        prevModel = lr.coefficients();*/
      }
      Song song = iter.next();
      ID3v2 tag = song.getTag();

      System.out.println(tag.getArtist() + "#" + tag.getGenreDescription());

      // features we want are not present, no point learning (TODO: but we
      // should still play it!)
      if (tag.getGenreDescription() == null)
        continue;

      // do we like this song?
      double userRating;
      
      if(tag.getGenreDescription().contains("Rock")) {
        userRating = 1;
      } else {
        userRating = 0;
      }

      // get indices
      int genreIndex = genres.indexOf(song.getTag().getGenreDescription()) + 1;

      // couldn't find the genre
      if (genreIndex == 0) {
        continue;
      }

      // consume the data for training
      double values[] = new double[data.numAttributes()];
      values[0] = genreIndex;
      values[1] = userRating;
      Instance inst = new Instance(1.0, values);
      data.add(inst);

      // increment count!
      ++count;
    }
  }
}
