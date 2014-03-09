package shuffle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import tags.Song;
import tags.TagExtractor;

/**
 * @author Abhinav Sharma
 */
public abstract class Shuffler {
  protected static final int NUM_INSTANCES = 5;

  private int                hateCount;
  private boolean            hatedPrev;

  protected List<Song>       songs;
  protected List<String>     artists;
  protected List<String>     genres;
  protected LinkedList<Song> playedSongs;

  private TagExtractor       tagExtractor;

  private int                count;
  private Iterator<Song>     iter;
  private ListIterator<Song> prevIter;

  public void initialize(List<Song> songs, List<String> artists, List<String> genres) {
    this.songs = songs;
    this.artists = artists;
    this.genres = genres;

    resetCounters();
    Collections.shuffle(songs);

    playedSongs = new LinkedList<Song>();
    prevIter = playedSongs.listIterator();
  }

  public void initialize(String musicDirectory, String dataDirectory) {
    try {
      this.tagExtractor = new TagExtractor(musicDirectory, dataDirectory);
      tagExtractor.run();
    } catch (Exception e) {
      System.out.println(e.toString());
    }

    songs = tagExtractor.getSongs();
    artists = tagExtractor.getArtists();
    genres = tagExtractor.getGenres();

    resetCounters();
    Collections.shuffle(songs);

    playedSongs = new LinkedList<Song>();
    prevIter = playedSongs.listIterator();
  }

  public Song next() {

    if (prevIter.hasPrevious()) {
      return prevIter.previous();
    }

    if (iter.hasNext()) {

      // user hated our learning, re-shuffle
      if (hateCount >= NUM_INSTANCES - 1) {
        System.out.println("So much hate!");
        Collections.shuffle(songs, new Random(new Date().getTime() + new Random(NUM_INSTANCES).nextLong()));
        resetCounters();
        next();
      }

      if (count >= NUM_INSTANCES) {
        // compute model
        computeModel();

        // rate all songs
        rateSongs();

        // reset counters
        resetCounters();

        // reset learner
        resetLearner();

      }
    } else {
      // all songs played! restart!
      songs = new ArrayList<Song>(playedSongs);
      playedSongs = new LinkedList<Song>();
      resetCounters();
    }

    // get the next song, remove it from songs and add to playedSongs
    Song play = iter.next();
    iter.remove();
    playedSongs.addFirst(play);
    prevIter = playedSongs.listIterator(0);

    return play;
  }

  public Song prev() {
    if (prevIter.hasNext()) {
      return prevIter.next();
    }
    return null;
  }

  public void feedback(Song song, double durationPlayed, double maxDuration) {

    // get rating
    int rating = getRating(durationPlayed, maxDuration);

    // set rating
    song.setRating(rating);
    song.setPlayed(true);

    // consume data for learning
    consumeData(song, rating);

    // did we hate the prev song? increment hate-count else reset hate-count
    hateCount = hatedPrev ? hateCount + 1 : 0;

    // did we hate this song? let the next song know!
    hatedPrev = rating < 1 ? true : false;

    // increment count
    ++count;
  }

  public List<Song> getSongs() {
    return songs;
  }

  public List<String> getArtists() {
    return artists;
  }

  public List<String> getGenres() {
    return genres;
  }

  public int getArtistIndex(Song song) {
    return artists.indexOf(song.getTag().artist);
  }

  public int getGenreIndex(Song song) {
    return genres.indexOf(song.getTag().genre);
  }

  private void resetCounters() {
    count = hateCount = 0;
    hatedPrev = false;
    iter = songs.iterator();
  }

  protected abstract int getRating(double durationPlayed, double maxDuration);

  protected abstract void rateSongs();

  protected abstract void consumeData(Song song, int rating);

  protected abstract void computeModel();

  protected abstract void resetLearner();

}
