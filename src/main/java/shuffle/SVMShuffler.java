package shuffle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;

import tags.Song;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Abhinav Sharma
 */
public class SVMShuffler extends Shuffler {
  private static final int           MAX_RATING = 10;

  private SMO                        svm;
  private Instances                  data;
  private ArrayList<ArrayList<Song>> songTable;

  @Override
  public void initialize(String musicDirectory, String dataDirectory) {
    super.initialize(musicDirectory, dataDirectory);
    svm = new SMO();
    data = makeDataContainer();
  }

  @Override
  protected int getRating(double durationPlayed, double maxDuration) {
    int rating = (int) (durationPlayed / maxDuration * (MAX_RATING - 1));
    return rating;
  }

  @Override
  protected void rateSongs() {
    // new song table
    songTable = new ArrayList<ArrayList<Song>>(MAX_RATING);
    for (int i = 0; i < MAX_RATING; ++i) {
      songTable.add(new ArrayList<Song>(songs.size() / MAX_RATING));
    }

    // set rating for all songs
    for (Song s : songs) {
      rateSong(s);
    }

    // construct new song list from table
    songs = new ArrayList<Song>(songs.size());
    for (int i = songTable.size() - 1; i >= 0; --i) {
      songs.addAll(songTable.get(i));
    }

    // shuffle sub lists
    for (int i = 0; i < songs.size() - NUM_INSTANCES; i += NUM_INSTANCES) {
      Collections.shuffle(songs.subList(i, i + NUM_INSTANCES), new Random(new Date().getTime()));
    }
  }

  @Override
  protected void consumeData(Song song, int rating) {

    String genre = song.getTag().getGenreDescription();
    String artist = song.getTag().getArtist();

    if (genre == null)
      genre = "Other";

    if (artist == null)
      artist = "Others";

    // consume the data for training
    double values[] = new double[data.numAttributes()];
    values[0] = data.attribute(0).indexOfValue(genre);
    values[1] = data.attribute(1).indexOfValue(artist);
    values[2] = data.attribute(2).indexOfValue(Integer.toString(rating));
    Instance inst = new Instance(1.0, values);
    inst.setDataset(data);
    data.add(inst);
  }

  @Override
  protected void computeModel() {
    // compute model
    try {
      svm.buildClassifier(data);
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  @Override
  protected void resetLearner() {
    // reset SVM
    svm = new SMO();
    data = makeDataContainer();
  }

  private Instances makeDataContainer() {
    Instances instances;

    // make individual attributes
    FastVector labels = new FastVector();
    for (String element : new HashSet<String>(getArtists()))
      labels.addElement(element);
    Attribute artistAttr = new Attribute("artist", labels);

    labels = new FastVector();
    for (String element : new HashSet<String>(getGenres()))
      labels.addElement(element);
    Attribute genreAttr = new Attribute("genre", labels);
    // genre will have more influence than artist
    genreAttr.setWeight(artistAttr.weight() * 2);

    // rating from 0 to MAX_RATING
    labels = new FastVector();
    for (int i = 0; i < MAX_RATING; ++i)
      labels.addElement(Integer.toString(i));
    Attribute ratingAttr = new Attribute("rating", labels);

    // add all attributes to data-set
    FastVector attributes = new FastVector();
    attributes.addElement(genreAttr);
    attributes.addElement(artistAttr);
    attributes.addElement(ratingAttr);

    instances = new Instances("songs", attributes, NUM_INSTANCES);
    instances.setClassIndex(instances.numAttributes() - 1);

    return instances;
  }

  private void rateSong(Song song) {

    int genreIndex = -1;
    int artistIndex = -1;
    double rating = 0;

    // get artist and genre indices
    if (song.getTag().getGenreDescription() != null && song.getTag().getArtist() != null) {
      genreIndex = data.attribute(0).indexOfValue(song.getTag().getGenreDescription());
      artistIndex = data.attribute(1).indexOfValue(song.getTag().getArtist());
    }

    // try to classify song, according to model
    try {

      Instance inst = new Instance(1.0, new double[] { genreIndex, artistIndex, 0 });
      inst.setDataset(data);
      rating = svm.classifyInstance(inst);

    } catch (Exception e) {
      e.printStackTrace();
    }

    // set the rating
    song.setRating(rating);
    // update table
    songTable.get((int) Math.floor(rating)).add(song);
  }

}
