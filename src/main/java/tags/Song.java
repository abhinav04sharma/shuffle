package tags;

/**
 * @author Abhinav Sharma
 */
public class Song implements Comparable<Song> {
  private Tag          tag;
  private boolean      isPlayed;
  private double       rating;
  private final String fileName;

  public Song(Tag tag, String fileName) {
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

  public Tag getTag() {
    return tag;
  }

  public void setTag(Tag tag) {
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
    if (rating < o.getRating())
      return -1;
    if (rating > o.getRating())
      return 1;
    return 0;
  }

  @Override
  public String toString() {
    return getTag().title;
  }

}
