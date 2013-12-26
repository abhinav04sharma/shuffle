package tags;

import com.mpatric.mp3agic.ID3v2;

public class Song implements Comparable<Song> {
  private ID3v2 tag;
  private double rating;
  private String fileName;
  
  public Song(ID3v2 tag, String fileName) {
    this.setTag(tag);
    this.fileName = fileName;
    rating = 0;
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
  
  public String getFileName() {
    return fileName;
  }

  public int compareTo(Song o) {
    return (int)((rating - o.getRating()) * 1000);
  }
  
  public String toString() {
    return tag.getArtist() + "#" + tag.getGenreDescription() + "#" + rating;
  }
  
}
