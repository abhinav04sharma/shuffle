/**
 * 
 */
package shuffle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;

import learning.LinearRegression;
import learning.TooManyTrainingSetsException;

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
  private double percentTolerance;

  Shuffle(String directory, double percentTolerance) {
    this.tagExtractor = new TagExtractor(directory);
    this.percentTolerance = percentTolerance;
  }

  public static void main(String args[]) {
    new Shuffle("D:/My Music", 1).run();
  }

  private boolean isPlayable(ID3v2 song) {
    int genreIndex = Collections.binarySearch(genres, song.getGenreDescription()) + 1;
    int artistIndex = Collections.binarySearch(artists, song.getArtist()) + 1;
    LinearRegression currentSongLR = new LinearRegression(2);

    try {
      currentSongLR.consumeTrainingData(1, genreIndex, artistIndex);
    } catch (TooManyTrainingSetsException e) {
      e.printStackTrace();
    }

    currentSongLR.computeModel();
    double[] currentSongModel = currentSongLR.getModel();
    double[] model = lr.getModel();
    return (Math.abs(currentSongModel[0] - model[0]) / model[0] < percentTolerance && Math.abs(currentSongModel[1] - model[1])
        / model[1] < percentTolerance);
  }

  public void run() {
    try {
      tagExtractor.run();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    List<ID3v2> songs = tagExtractor.getSongs();
    genres = new ArrayList<String>(tagExtractor.getGenres());
    artists = new ArrayList<String>(tagExtractor.getArtists());

    int data = 0;
    Collections.shuffle(songs);
    System.out.println(songs.size());

    for (ID3v2 song : songs) {
      if (data % 10 == 0) {
        lr.computeModel();
      }
      try {
        if (song.getGenreDescription() != null && song.getArtist() != null) {
          int result = song.getArtist().equalsIgnoreCase("coldplay") ? 1
              : 0;
          int genreIndex = Collections.binarySearch(genres, song.getGenreDescription()) + 1;
          int artistIndex = Collections.binarySearch(artists, song.getArtist()) + 1;

          if (data < 10 || isPlayable(song)) {
            System.out.println(song.getArtist() + "#" + song.getTitle());
            lr.consumeTrainingData(result, genreIndex, artistIndex);
            ++data;
          }
        }
      } catch (TooManyTrainingSetsException e) {
        e.printStackTrace();
      }
    }
  }
}
