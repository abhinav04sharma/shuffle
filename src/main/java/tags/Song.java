package tags;

import com.mpatric.mp3agic.ID3v2;

/**
 * @author Abhinav Sharma
 */
public class Song implements Comparable<Song> {
  private ID3v2 tag;
  private boolean isPlayed;
  private double rating;
  private String fileName;
  
  public Song(ID3v2 tag, String fileName) {
    this.setTag(tag);
    this.fileName = fileName;
    this.rating = 0;
    this.isPlayed = false;
  }
  
  public double getRating() {
    return rating;
  }
  
  public void setRating(double rating) {
    this.rating = rating; 
  }

  public ID3v2 getTag() {
    return tag;
  }

  public void setTag(ID3v2 tag) {
    this.tag = tag;
  }
  
  public boolean isPlayed() {
    return isPlayed;
  }
  
  public void setPlayed(boolean played) {
    this.isPlayed = played;
  }
  
  public String getFileName() {
    return fileName;
  }

  public int compareTo(Song o) {
    if (rating < o.getRating()) return -1;
    if (rating > o.getRating()) return 1;
    return 0;
  }
  
  public String toString() {
    return tag.getArtist() + "#" + tag.getGenreDescription() + "#" + rating;
  }
  
}
