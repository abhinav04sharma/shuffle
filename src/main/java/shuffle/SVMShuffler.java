/**
 * 
 */
package shuffle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import tags.Song;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Abhinav Sharma
 */
public class SVMShuffler implements Shuffler {

  private List<Song>     songs;

  private TagExtractor   tagExtractor;
  private SMO            svm   = new SMO();
  private Instances      data;

  private int            count = 0;
  private Iterator<Song> iter;

  private void rateSong(Song song) {
    double originalRating;

    // store original rating
    originalRating = song.getRating();

    do {

      // get artist and genre indices
      int genreIndex = data.attribute(0).indexOfValue(song.getTag().getGenreDescription());
      int artistIndex = data.attribute(1).indexOfValue(song.getTag().getArtist());

      // index not found so... -ve infinity!
      if (genreIndex == -1 || artistIndex == -1) {
        song.setRating(0);
        break;
      }

      double rating = 0;
      try {
        Instance inst = new Instance(1.0, new double[] { genreIndex, artistIndex, 0 });
        inst.setDataset(data);
        rating = svm.classifyInstance(inst);
      } catch (Exception e) {
        e.printStackTrace();
      }
      song.setRating(rating);
    } while (false);

    // case: song was played, print original and predicted rating
    if (song.isPlayed()) {
      System.out.println(song + " Original rating: " + originalRating + " Predicted Rating: " + song.getRating());
    }
  }

  public void initialize(String directory) {

    try {
      this.tagExtractor = new TagExtractor(directory);
      tagExtractor.run();
    } catch (Exception e) {
      e.printStackTrace();
    }

    songs = tagExtractor.getSongs();
    iter = songs.iterator();
    count = 0;
    Collections.shuffle(songs);

    // make individual attributes
    FastVector labels = new FastVector();
    for (String element : new HashSet<String>(tagExtractor.getGenres())) {
      labels.addElement(element);
    }
    Attribute genreAttr = new Attribute("genre", labels);

    labels = new FastVector();
    for (String element : new HashSet<String>(tagExtractor.getArtists())) {
      labels.addElement(element);
    }
    Attribute artistAttr = new Attribute("artist", labels);

    labels = new FastVector();
    for (String element : new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }) {
      labels.addElement(element);
    }
    Attribute ratingAttr = new Attribute("rating", labels);

    // add all attributes to data-set
    FastVector attributes = new FastVector();
    attributes.addElement(genreAttr);
    attributes.addElement(artistAttr);
    attributes.addElement(ratingAttr);

    data = new Instances("songs", attributes, 50);
    data.setClassIndex(data.numAttributes() - 1);
  }

  public Song next() {

    if (iter.hasNext()) {
      if (count == 10) {
        // compute model
        try {
          svm.buildClassifier(data);
        } catch (Exception e) {
          e.printStackTrace();
        }
        // set rating for all songs
        for (Song s : songs) {
          rateSong(s);
        }
        // sort according to rating
        Collections.sort(songs, Collections.reverseOrder());
        // shuffle sub list
        Collections.shuffle(songs.subList(0, 20));
        // reset counter
        count = 0;
        iter = songs.iterator();
        // reset linear regression
        svm = new SMO();
      }
    } else {
      iter = songs.iterator();
      count = 0;
    }

    Song play = iter.next();
    // skip already played songs
    if (play.isPlayed()) {
      return next();
    }
    ++count;
    return play;
  }

  public void feedback(Song song, double duration) {
    // get indices
    String genre = song.getTag().getGenreDescription();
    String artist = song.getTag().getArtist();
    if (genre == null) {
      genre = "Other";
    }
    if (artist == null) {
      artist = "Others";
    }

    song.setPlayed(true);
    song.setRating(duration);

    // consume the data for training
    double values[] = new double[data.numAttributes()];
    values[0] = data.attribute(0).indexOfValue(genre);
    values[1] = data.attribute(1).indexOfValue(artist);
    values[2] = data.attribute(2).indexOfValue(Integer.toString((int) (duration / 30 + 1)));
    Instance inst = new Instance(1.0, values);
    data.add(inst);
  }
}
